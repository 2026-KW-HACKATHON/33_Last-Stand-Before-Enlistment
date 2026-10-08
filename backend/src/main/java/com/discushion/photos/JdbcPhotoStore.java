package com.discushion.photos;

import java.time.Instant;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import static com.discushion.photos.PhotoFailure.Reason.*;

public final class JdbcPhotoStore {
    final JdbcTemplate jdbc;
    public JdbcPhotoStore(DataSource source) { jdbc=new JdbcTemplate(source); }
    Instant databaseNow() {return jdbc.queryForObject("select clock_timestamp()",Timestamp.class).toInstant();}
    Optional<PhotoFile> find(long id, boolean lock) {
        return jdbc.query("select * from discushion.media_files where id=?"+(lock?" for update":""),
            JdbcPhotoStore::map,id).stream().findFirst();
    }
    PhotoFile owned(long id,long owner) {
        var file=find(id,true).orElseThrow(()->new PhotoFailure(PHOTO_UPLOAD_NOT_FOUND));
        if(file.owner()!=owner || !"POST_PHOTO".equals(file.purpose())) throw new PhotoFailure(PHOTO_UPLOAD_NOT_FOUND);
        return file;
    }
    boolean referenced(long id) {
        return Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from discushion.post_photos where file_id=?)",Boolean.class,id));
    }
    PhotoFile reserve(long owner,String name,String mime,long bytes,Instant now,Instant bound) {
        return reserve(owner,name,mime,bytes,now,bound,false);
    }
    PhotoFile reserve(long owner,String name,String mime,long bytes,Instant now,Instant bound,boolean relay) {
        var usage=jdbc.queryForMap("""
            select count(*) as files, coalesce(sum(size_bytes),0) as bytes from discushion.media_files
            where owner_user_id=? and purpose='POST_PHOTO' and lifecycle_status in ('UPLOADING','UNLINKED','DELETE_PENDING')
            """,owner);
        if(((Number)usage.get("files")).longValue()>=20 || ((Number)usage.get("bytes")).longValue()+bytes>100_000_000L)
            throw new PhotoFailure(PHOTO_UPLOAD_QUOTA_EXCEEDED);
        var recent=jdbc.queryForMap("""
            select count(*) as files, min(created_at) as oldest from discushion.media_files
            where owner_user_id=? and purpose='POST_PHOTO' and created_at>?
            """,owner,Timestamp.from(now.minusSeconds(60)));
        if(((Number)recent.get("files")).longValue()>=20) {
            var oldest=((Timestamp)recent.get("oldest")).toInstant();
            long retry=Math.max(1,(java.time.Duration.between(now,oldest.plusSeconds(60)).toMillis()+999)/1000);
            throw new PhotoFailure(PHOTO_UPLOAD_RATE_LIMITED,retry);
        }
        String key="post-photos/"+owner+"/"+UUID.randomUUID();
        long id=jdbc.queryForObject("""
            insert into discushion.media_files(owner_user_id,storage_key,original_name,mime_type,size_bytes,purpose,
              created_at,lifecycle_status,upload_authorization_expires_at,upload_transport)
              values(?,?,?,?,?,'POST_PHOTO',?,'UPLOADING',?,?) returning id
            """,Long.class,owner,key,name,mime,bytes,Timestamp.from(now),Timestamp.from(bound),relay?"SERVER_RELAY":"DIRECT_UNCONFIRMED");
        return find(id,false).orElseThrow();
    }
    void authorizationBound(long id,Instant bound) {
        jdbc.update("update discushion.media_files set upload_authorization_expires_at=greatest(upload_authorization_expires_at,?) where id=?",Timestamp.from(bound),id);
    }
    void startUpload(long id,UUID attempt,Instant now) {
        int changed=jdbc.update("""
            update discushion.media_files set upload_attempt_id=?,upload_attempt_status='RUNNING',upload_attempt_started_at=?
            where id=? and upload_transport='SERVER_RELAY' and lifecycle_status='UPLOADING'
              and upload_attempt_id is null and upload_authorization_expires_at>?
            """,attempt,Timestamp.from(now),id,Timestamp.from(now));
        if(changed!=1) throw new PhotoFailure(PHOTO_UPLOAD_NOT_READY);
    }
    void finishUpload(long id,UUID attempt,boolean acknowledged) {
        jdbc.update("""
            update discushion.media_files set upload_attempt_status=?,upload_attempt_finished_at=?
            where id=? and upload_attempt_id=? and upload_attempt_status='RUNNING'
            """,acknowledged?"ACKNOWLEDGED":"UNKNOWN",acknowledged?Timestamp.from(databaseNow()):null,id,attempt);
    }
    void verified(long id,PhotoContent.Verified content,Instant now) {
        jdbc.update("""
            update discushion.media_files set lifecycle_status='UNLINKED',uploaded_at=?,mime_type=?,size_bytes=?
            where id=? and lifecycle_status='UPLOADING'
            """,Timestamp.from(now),content.mime(),content.bytes(),id);
    }
    void deleteIntent(long id,Instant now) {
        jdbc.update("""
            update discushion.media_files set lifecycle_status='DELETE_PENDING',
              delete_requested_at=coalesce(delete_requested_at,?), next_delete_attempt_at=coalesce(next_delete_attempt_at,?)
            where id=? and lifecycle_status not in ('DELETED','DELETE_PENDING')
            """,Timestamp.from(now),Timestamp.from(now),id);
    }
    List<Long> candidates(Instant now,int limit) {
        return jdbc.queryForList("""
            select id from discushion.media_files where purpose='POST_PHOTO' and (
              (lifecycle_status='UPLOADING' and created_at<=?) or
              (lifecycle_status='UNLINKED' and uploaded_at<=?) or
              (lifecycle_status='DELETE_PENDING' and (next_delete_attempt_at is null or next_delete_attempt_at<=?)
               and (deletion_claim_expires_at is null or deletion_claim_expires_at<=?))) order by id limit ?
            """,Long.class,Timestamp.from(now.minusSeconds(86400)),Timestamp.from(now.minusSeconds(86400)),
                Timestamp.from(now),Timestamp.from(now),limit);
    }
    void claim(long id,UUID token,Instant now,Instant expiry) {
        jdbc.update("""
            update discushion.media_files set deletion_claim_token=?,deletion_claimed_at=?,deletion_claim_expires_at=?,
              deletion_attempts=deletion_attempts+1 where id=?
            """,token,Timestamp.from(now),Timestamp.from(expiry),id);
    }
    boolean finish(long id,UUID token,Instant now,boolean complete,String error,Instant retry) {
        return jdbc.update("""
            update discushion.media_files set lifecycle_status=?,deleted_at=?,last_delete_error_code=?,next_delete_attempt_at=?,
              deletion_claim_token=null,deletion_claimed_at=null,deletion_claim_expires_at=null
            where id=? and lifecycle_status='DELETE_PENDING' and deletion_claim_token=? and deletion_claim_expires_at>?
            """,complete?"DELETED":"DELETE_PENDING",complete?Timestamp.from(now):null,error,
                complete?null:Timestamp.from(retry),id,token,Timestamp.from(now))==1;
    }
    static PhotoFile map(ResultSet rs,int row) throws SQLException {
        return new PhotoFile(rs.getLong("id"),rs.getLong("owner_user_id"),rs.getString("storage_key"),rs.getString("mime_type"),
            rs.getLong("size_bytes"),rs.getString("purpose"),rs.getString("lifecycle_status"),time(rs,"created_at"),
            time(rs,"uploaded_at"),time(rs,"linked_at"),time(rs,"delete_requested_at"),time(rs,"deleted_at"),
            time(rs,"upload_authorization_expires_at"),(UUID)rs.getObject("deletion_claim_token"),
            time(rs,"deletion_claim_expires_at"),rs.getInt("deletion_attempts"),
            rs.getString("upload_transport"),rs.getString("upload_attempt_status"));
    }
    private static Instant time(ResultSet rs,String column) throws SQLException {
        var value=rs.getTimestamp(column); return value==null?null:value.toInstant();
    }
}
