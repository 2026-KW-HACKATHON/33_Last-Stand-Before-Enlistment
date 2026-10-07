package com.discushion.photos;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.*;
import com.discushion.identity.MemberAuthorization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static com.discushion.photos.PhotoFailure.Reason.*;

/** Called by #14/#16 inside their writable transaction. No post endpoints or Storage calls here. */
public final class PhotoAttachments {
    public record Reference(Long photoId,Long fileId) {}
    private final JdbcPhotoStore store;
    private final MemberAuthorization authorization;
    private final Clock clock;
    public PhotoAttachments(JdbcPhotoStore store,MemberAuthorization authorization,Clock clock) {
        this.store=store; this.authorization=authorization; this.clock=clock;
    }
    /** null means preserve; [] removes every reference. Returns removed owner file IDs for PATCH meta. */
    public List<Long> replace(long postId,List<Reference> order) {
        if(!TransactionSynchronizationManager.isActualTransactionActive() || TransactionSynchronizationManager.isCurrentTransactionReadOnly())
            throw new IllegalStateException("Photos require the caller's writable transaction");
        PhotoService.validId(postId);
        var member=authorization.lockCurrentCompletedMember();
        var posts=store.jdbc.queryForList("select author_user_id,region_id,status from discushion.posts where id=? for update",postId);
        if(posts.size()!=1 || !Objects.equals(posts.get(0).get("author_user_id"),member.userId())
            || !"PUBLISHED".equals(posts.get(0).get("status"))) throw new PhotoFailure(VALIDATION_ERROR);
        authorization.requireVerifiedRegion(member,((Number)posts.get(0).get("region_id")).longValue());
        var polls=store.jdbc.queryForList("select ends_at from discushion.polls where post_id=? for update",postId);
        if(!polls.isEmpty() && !clock.instant().isBefore(((Timestamp)polls.get(0).get("ends_at")).toInstant())) throw new PhotoFailure(VALIDATION_ERROR);
        if(order==null) return List.of();
        if(order.size()>10) throw new PhotoFailure(PHOTO_SIZE_EXCEEDED);
        var existing=store.jdbc.queryForList("select id,file_id from discushion.post_photos where post_id=? order by sort_order",postId);
        Map<Long,Long> byPhoto=new HashMap<>();
        for(var row:existing) byPhoto.put(((Number)row.get("id")).longValue(),((Number)row.get("file_id")).longValue());
        List<Long> desired=new ArrayList<>();
        for(var ref:order) {
            if(ref==null || (ref.photoId()==null)==(ref.fileId()==null)) throw new PhotoFailure(VALIDATION_ERROR);
            Long id=ref.fileId()!=null?ref.fileId():byPhoto.get(ref.photoId());
            if(id==null) throw new PhotoFailure(VALIDATION_ERROR);
            PhotoService.validId(id); desired.add(id);
        }
        if(new HashSet<>(desired).size()!=desired.size()) throw new PhotoFailure(VALIDATION_ERROR);
        SortedSet<Long> all=new TreeSet<>(byPhoto.values()); all.addAll(desired);
        Map<Long,PhotoFile> files=new HashMap<>();
        for(long id:all) files.put(id,store.owned(id,member.userId()));
        long total=0;
        for(int index=0;index<order.size();index++) {
            var ref=order.get(index); var file=files.get(desired.get(index));
            if(ref.fileId()!=null) {
                if(!"UNLINKED".equals(file.status()) || !file.validated()) throw new PhotoFailure(PHOTO_UPLOAD_NOT_READY);
                if(file.expired(clock.instant())) throw new PhotoFailure(PHOTO_UPLOAD_EXPIRED);
                if(store.referenced(file.id())) throw new PhotoFailure(PHOTO_ALREADY_LINKED);
            } else if(!"LINKED".equals(file.status()) && !"LEGACY".equals(file.status())) throw new PhotoFailure(PHOTO_UPLOAD_NOT_READY);
            total=Math.addExact(total,file.bytes());
        }
        if(total>PhotoContent.MAX_BYTES) throw new PhotoFailure(PHOTO_SIZE_EXCEEDED);
        var removed=byPhoto.values().stream().filter(id->!desired.contains(id)).sorted().toList();
        // File locks may have waited past ends_at. Recheck immediately before the first write.
        if(!polls.isEmpty() && !clock.instant().isBefore(((Timestamp)polls.get(0).get("ends_at")).toInstant())) throw new PhotoFailure(VALIDATION_ERROR);
        // Deferrable ordering constraint permits swaps without temporary invalid/negative positions.
        store.jdbc.execute("set constraints discushion.post_photos_post_id_sort_order_key deferred");
        for(var entry:byPhoto.entrySet()) if(removed.contains(entry.getValue())) {
            store.jdbc.update("delete from discushion.post_photos where id=? and post_id=?",entry.getKey(),postId);
            store.deleteIntent(entry.getValue(),clock.instant());
        }
        for(int index=0;index<order.size();index++) {
            var ref=order.get(index); long id=desired.get(index);
            if(ref.photoId()!=null) store.jdbc.update("update discushion.post_photos set sort_order=? where id=? and post_id=?",index,ref.photoId(),postId);
            else {
                store.jdbc.update("insert into discushion.post_photos(post_id,file_id,sort_order) values(?,?,?)",postId,id,index);
                store.jdbc.update("update discushion.media_files set lifecycle_status='LINKED',linked_at=? where id=?",Timestamp.from(clock.instant()),id);
            }
        }
        store.jdbc.execute("set constraints discushion.post_photos_post_id_sort_order_key immediate");
        return removed;
    }
}
