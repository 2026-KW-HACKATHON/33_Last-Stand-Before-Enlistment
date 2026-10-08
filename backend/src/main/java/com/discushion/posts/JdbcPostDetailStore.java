package com.discushion.posts;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;

final class JdbcPostDetailStore {
    private final JdbcTemplate jdbc;
    JdbcPostDetailStore(DataSource source) { jdbc = new JdbcTemplate(source); }
    void lock(long id) {
        var rows = jdbc.query("select status from discushion.posts where id=? for share", (rs, row) -> rs.getString(1), id);
        if (rows.isEmpty() || !"PUBLISHED".equals(rows.get(0))) throw PostDetailFailure.missing();
        jdbc.query("select id from discushion.polls where post_id=? for share", (rs, row) -> rs.getLong(1), id);
    }
    Base base(long id, Instant now) {
        return jdbc.query("""
                select p.id,p.type,p.topic,p.title,p.content,p.region_id,p.author_user_id,p.created_at,p.updated_at,r.name region_name,pr.nickname,
                exists(select 1 from discushion.institution_credentials ic where ic.user_id=p.author_user_id
                  and ic.completed_at<=? and ?<ic.valid_until) institution
                from discushion.posts p join discushion.regions r on r.id=p.region_id
                left join discushion.profiles pr on pr.user_id=p.author_user_id
                where p.id=? and p.status='PUBLISHED'
                """, (rs, row) -> new Base(rs.getLong("id"), rs.getString("type"), rs.getString("topic"),
                rs.getString("title"), rs.getString("content"), rs.getLong("region_id"), rs.getString("region_name"),
                rs.getLong("author_user_id"), rs.getString("nickname"), rs.getBoolean("institution"),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant()),
                Timestamp.from(now), Timestamp.from(now), id).stream().findFirst().orElseThrow(PostDetailFailure::missing);
    }
    List<Image> images(long id) {
        return jdbc.query("""
                select pp.id,pp.file_id,m.storage_key,m.mime_type,m.size_bytes,m.owner_user_id,m.purpose,m.lifecycle_status
                from discushion.post_photos pp join discushion.media_files m on m.id=pp.file_id
                where pp.post_id=? order by pp.sort_order,pp.id
                """, (rs, row) -> {
            if (!"POST_PHOTO".equals(rs.getString("purpose")) || !"LINKED".equals(rs.getString("lifecycle_status")))
                throw new IllegalStateException("Attached photo is not a linked post photo");
            return new Image(rs.getLong("id"), rs.getLong("file_id"), rs.getString("storage_key"),
                    rs.getString("mime_type"), rs.getLong("size_bytes"), rs.getLong("owner_user_id"));
        }, id);
    }
    String organizerEmail(long authorId) {
        return jdbc.queryForObject("select email from discushion.users where id=?", String.class, authorId);
    }
    Activity activity(long id) {
        return jdbc.query("select source,schedule,place,activity_status,external_participation_url from discushion.activity_post_details where post_id=?",
                (rs, row) -> new Activity(rs.getString("source"), rs.getString("schedule"), rs.getString("place"),
                        rs.getString("activity_status"), rs.getString("external_participation_url")), id)
                .stream().findFirst().orElseThrow(() -> new IllegalStateException("Published activity has no details"));
    }
    Poll poll(long id) {
        return jdbc.query("select id,question,ends_at from discushion.polls where post_id=?",
                (rs, row) -> new Poll(rs.getLong(1), rs.getString(2), rs.getTimestamp(3).toInstant()), id)
                .stream().findFirst().orElseThrow(() -> new IllegalStateException("Published vote has no poll"));
    }
    List<Option> options(long pollId) {
        return jdbc.query("select id,content from discushion.poll_options where poll_id=? order by sort_order,id",
                (rs, row) -> new Option(rs.getLong(1), rs.getString(2)), pollId);
    }
    List<Adoption> adoptions(long id) {
        return jdbc.query("""
                select a.institution_id,i.name,a.adopted_at from discushion.institution_agenda_adoptions a
                join discushion.institutions i on i.id=a.institution_id
                where a.post_id=? and a.canceled_at is null order by a.adopted_at,a.id
                """, (rs, row) -> new Adoption(rs.getLong(1), rs.getString(2), rs.getTimestamp(3).toInstant()), id);
    }
    record Base(long id, String type, String topic, String title, String content, long regionId, String regionName,
            long authorId, String nickname, boolean institution, Instant createdAt, Instant updatedAt) {}
    record Image(long photoId, long fileId, String key, String mime, long bytes, long ownerId) {}
    record Activity(String source, String schedule, String place, String status, String url) {}
    record Poll(long id, String question, Instant endsAt) {}
    record Option(long id, String content) {}
    record Adoption(long institutionId, String name, Instant at) {}
}
