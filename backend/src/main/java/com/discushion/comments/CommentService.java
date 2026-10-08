package com.discushion.comments;

import com.discushion.contracts.identity.*;
import com.discushion.contracts.post.*;
import com.discushion.contracts.share.SharedPostAccess;
import com.discushion.identity.*;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;
import org.springframework.transaction.support.TransactionTemplate;

final class CommentService implements com.discushion.contracts.participation.PostCommentPageReader {
    private final CurrentActorProvider actors;
    private final JdbcMemberStore members;
    private final MemberAuthorization guard;
    private final SharedPostAccess sharing;
    private final Supplier<PostContextReader> posts;
    private final JdbcCommentStore store;
    private final TransactionTemplate reads, writes;
    private final Clock clock;
    CommentService(CurrentActorProvider actors, JdbcMemberStore members, MemberAuthorization guard, SharedPostAccess sharing,
                   Supplier<PostContextReader> posts, JdbcCommentStore store, TransactionTemplate reads, TransactionTemplate writes, Clock clock) {
        this.actors = actors; this.members = members; this.guard = guard; this.sharing = sharing; this.posts = posts;
        this.store = store; this.reads = reads; this.writes = writes; this.clock = clock;
    }
    @Override public Map<String, Object> initialPage(long postId) {
        return list(postId, new CommentQuery(CommentQuery.Sort.LIKES, 20, null));
    }
    Map<String, Object> list(long postId, CommentQuery query) {
        return reads.execute(status -> {
            Long viewer = currentMember();
            if (viewer == null) {
                if (sharing.guestForRead(postId, SharedPostAccess.Action.COMMENTS).isEmpty()) throw new IllegalStateException("Missing verified guest context");
            }
            else published(posts.get().find(postId), postId);
            var now = clock.instant();
            var args = new ArrayList<Object>(); args.add(postId);
            String where = "c.post_id=? and c.parent_comment_id is null";
            if (query.after() != null) {
                var p = query.after();
                var anchor = store.find(p.id(), viewer, now).orElseThrow(() -> CommentFailure.invalid("cursor"));
                if (anchor.postId() != postId || anchor.parent() != null || !anchor.at().equals(p.createdAt()))
                    throw CommentFailure.invalid("cursor");
                if (query.sort() == CommentQuery.Sort.LIKES) {
                    where += " and ((select count(*) from discushion.comment_evaluations e where e.comment_id=c.id and e.evaluation_type='LIKE'),c.created_at,c.id)<(?,?,?)";
                    args.add(p.likes());
                } else where += " and (c.created_at,c.id)<(?,?)";
                args.add(Timestamp.from(p.createdAt())); args.add(p.id());
            }
            where += query.sort() == CommentQuery.Sort.LIKES ? " order by likes desc,c.created_at desc,c.id desc" : " order by c.created_at desc,c.id desc";
            where += " limit ?"; args.add(query.size() + 1);
            var roots = store.rows(where, args, viewer, now);
            boolean next = roots.size() > query.size();
            roots = roots.subList(0, Math.min(query.size(), roots.size()));
            var children = new HashMap<Long, List<Map<String, Object>>>();
            if (!roots.isEmpty()) {
                var childArgs = new ArrayList<Object>(); childArgs.add(postId); roots.forEach(row -> childArgs.add(row.id()));
                String placeholders = String.join(",", Collections.nCopies(roots.size(), "?"));
                for (var child : store.rows("c.post_id=? and c.parent_comment_id in (" + placeholders + ") order by c.created_at,c.id", childArgs, viewer, now))
                    children.computeIfAbsent(child.parent(), ignored -> new ArrayList<>()).add(store.view(child, viewer != null, List.of()));
            }
            var data = new ArrayList<Map<String, Object>>();
            for (var root : roots) data.add(store.view(root, viewer != null, children.getOrDefault(root.id(), List.of())));
            String cursor = null;
            if (next) {
                var last = roots.get(roots.size() - 1);
                cursor = CommentQuery.encode(postId, query.sort(), new CommentQuery.Position(last.likes(), last.at(), last.id()));
            }
            var meta = new LinkedHashMap<String, Object>(); meta.put("nextCursor", cursor); meta.put("hasNext", next);
            return Map.of("data", data, "meta", meta);
        });
    }
    Map<String, Object> create(long pathId, boolean reply, CommentInput input) {
        return writes.execute(status -> {
            MemberQualification member = actors.current().isPresent() ? guard.lockCurrentCompletedMember() : null;
            var now = clock.instant();
            var initial = reply ? comment(pathId, member == null ? null : member.userId(), now) : null;
            long postId = reply ? initial.postId() : pathId;
            if (member == null) {
                if (sharing.guestForWrite(postId, reply ? SharedPostAccess.Action.CREATE_REPLY : SharedPostAccess.Action.CREATE_COMMENT).isEmpty())
                    throw new IllegalStateException("Missing verified guest context");
            }
            else {
                var post = published(posts.get().findForUpdate(postId), postId);
                guard.requireVerifiedRegion(member, post.regionId());
            }
            Long parent = null, target = null; String targetName = null;
            if (reply) {
                var path = comment(pathId, member == null ? null : member.userId(), now);
                if (path.postId() != postId) throw new IllegalStateException("Reply source changed posts");
                parent = path.parent() == null ? path.id() : path.parent();
                var root = comment(parent, null, now);
                if (root.postId() != postId || root.parent() != null) throw new CommentFailure("COMMENT_DEPTH_EXCEEDED", 400, "commentId");
                target = input.replyToCommentId() == null ? path.id() : input.replyToCommentId();
                var addressed = comment(target, null, now);
                if (addressed.postId() != postId || (addressed.id() != parent && !Objects.equals(addressed.parent(), parent)))
                    throw CommentFailure.invalid("replyToCommentId");
                targetName = addressed.name();
            }
            CommentWords.requireAllowed(input.content());
            // Recheck guest expiry at the final write boundary after resolving the actual thread.
            if (member == null && sharing.guestForWrite(postId, reply ? SharedPostAccess.Action.CREATE_REPLY : SharedPostAccess.Action.CREATE_COMMENT).isEmpty())
                throw new IllegalStateException("Missing verified guest context");
            long id = store.insert(postId, parent, target, targetName, member == null ? null : member.userId(), input.content(), clock.instant());
            // #5 is deliberately deferred. Current participation remains the real comments relation.
            return Map.of("data", store.view(comment(id, member == null ? null : member.userId(), clock.instant()), member != null, List.of()));
        });
    }
    private Long currentMember() {
        var actor = actors.current();
        if (actor.isEmpty()) return null;
        var member = members.findByVerifiedSubject(actor.get().privySubject()).orElseThrow(() -> new IdentityFailure(IdentityFailure.Reason.NOT_REGISTERED));
        if (member.registrationCompletedAt().isEmpty()) throw new IdentityFailure(IdentityFailure.Reason.INCOMPLETE);
        return member.userId();
    }
    private JdbcCommentStore.Row comment(long id, Long viewer, Instant now) {
        return store.find(id, viewer, now).orElseThrow(() -> new CommentFailure("COMMENT_NOT_FOUND", 404, "commentId"));
    }
    private static PostContext published(Optional<PostContext> value, long id) {
        var post = value.orElseThrow(() -> new CommentFailure("POST_NOT_FOUND", 404, "postId"));
        if (post.postId() != id) throw new IllegalStateException("Post adapter returned a different source");
        if (post.status() != PostStatus.PUBLISHED) throw new CommentFailure("POST_NOT_FOUND", 404, "postId");
        return post;
    }
}
