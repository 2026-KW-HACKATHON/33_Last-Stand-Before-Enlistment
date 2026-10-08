package com.discushion.posts;

import com.discushion.contracts.post.PollContext;
import com.discushion.contracts.post.PostContext;
import com.discushion.contracts.post.PostContextReader;
import com.discushion.contracts.post.PostStatus;
import com.discushion.contracts.post.PostType;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Owns the post-first/poll-second locks and the post-edit SQL used by this feature. */
public final class PostJdbcStore implements PostContextReader {
    private final JdbcTemplate jdbc;
    public PostJdbcStore(DataSource source) { this.jdbc = new JdbcTemplate(source); }

    @Override public Optional<PostContext> find(long postId) { return context(postId, false); }
    @Override public Optional<PostContext> findForUpdate(long postId) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || TransactionSynchronizationManager.isCurrentTransactionReadOnly())
            throw new IllegalStateException("Post write transaction is required");
        return context(postId, true);
    }

    private Optional<PostContext> context(long postId, boolean lock) {
        var rows = jdbc.query("select id,type,region_id,author_user_id,status from discushion.posts where id=?"
                + (lock ? " for update" : ""), (rs, row) -> new Base(rs.getLong(1), rs.getString(2),
                rs.getLong(3), rs.getLong(4), rs.getString(5)), postId);
        if (rows.isEmpty()) return Optional.empty();
        Base base = rows.get(0);
        var polls = jdbc.query("select id,ends_at from discushion.polls where post_id=?"
                + (lock ? " for update" : ""), (rs, row) -> new Poll(rs.getLong(1), rs.getTimestamp(2).toInstant()), postId);
        Optional<PollContext> poll = Optional.empty();
        if (!polls.isEmpty()) {
            Poll found = polls.get(0);
            List<Long> options = jdbc.query("select id from discushion.poll_options where poll_id=? order by sort_order,id",
                    (rs, row) -> rs.getLong(1), found.id());
            poll = Optional.of(new PollContext(found.id(), found.endsAt(), options));
        }
        return Optional.of(new PostContext(base.id(), PostType.valueOf(base.type()), base.regionId(), base.authorId(),
                PostStatus.valueOf(base.status()), poll));
    }

    Editable editable(long postId) {
        return jdbc.queryForObject("""
                select p.title,p.content,p.topic,p.region_id,p.type,a.source,a.schedule,a.place,a.activity_status,
                       a.external_participation_url,q.ends_at
                from discushion.posts p
                left join discushion.activity_post_details a on a.post_id=p.id
                left join discushion.polls q on q.post_id=p.id where p.id=?
                """, (rs, row) -> new Editable(rs.getString("title"), rs.getString("content"), rs.getString("topic"),
                rs.getLong("region_id"), PostType.valueOf(rs.getString("type")), rs.getString("source"),
                rs.getString("schedule"), rs.getString("place"), rs.getString("activity_status"),
                rs.getString("external_participation_url"), Optional.ofNullable(rs.getTimestamp("ends_at")).map(Timestamp::toInstant)), postId);
    }

    Instant databaseNow() { return jdbc.queryForObject("select clock_timestamp()", Timestamp.class).toInstant(); }
    boolean hasActiveAdoption(long postId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from discushion.institution_agenda_adoptions where post_id=? and canceled_at is null)",
                Boolean.class, postId));
    }

    void update(long postId, PostPatch patch, Editable current) {
        List<String> columns = new ArrayList<>(); List<Object> values = new ArrayList<>();
        add(columns, values, "title", patch.title()); add(columns, values, "content", patch.content());
        add(columns, values, "topic", patch.topic()); add(columns, values, "region_id", patch.regionId());
        columns.add("updated_at=clock_timestamp()"); columns.add("content_revision=content_revision+1"); values.add(postId);
        if (jdbc.update("update discushion.posts set " + String.join(",", columns) + " where id=? and status='PUBLISHED'", values.toArray()) != 1)
            throw new IllegalStateException("Locked post could not be updated");
        if (patch.detailsSupplied() && current.type() == PostType.LOCAL_ACTIVITY) {
            jdbc.update("update discushion.activity_post_details set source=?,schedule=?,place=?,activity_status=?,external_participation_url=? where post_id=?",
                    value(patch.source(), current.source()), value(patch.schedule(), current.schedule()),
                    value(patch.place(), current.place()), value(patch.activityStatus(), current.activityStatus()),
                    value(patch.externalParticipationUrl(), current.externalParticipationUrl()), postId);
        } else if (patch.endsAt().supplied()) {
            if (jdbc.update("update discushion.polls set ends_at=? where post_id=?", Timestamp.from(patch.endsAt().value()), postId) != 1)
                throw new IllegalStateException("Locked poll could not be updated");
        }
    }

    void delete(long postId) {
        if (jdbc.update("update discushion.posts set status='DELETED',deleted_at=clock_timestamp(),updated_at=clock_timestamp(),content_revision=content_revision+1 where id=? and status='PUBLISHED'", postId) != 1)
            throw new IllegalStateException("Locked post could not be deleted");
    }

    private static void add(List<String> columns, List<Object> values, String name, PostPatch.Field<?> field) {
        if (field.supplied()) { columns.add(name + "=?"); values.add(field.value()); }
    }
    private static Object value(PostPatch.Field<String> field, String existing) { return field.supplied() ? field.value() : existing; }
    record Editable(String title, String content, String topic, long regionId, PostType type, String source,
            String schedule, String place, String activityStatus, String externalParticipationUrl, Optional<Instant> endsAt) {}
    private record Base(long id, String type, long regionId, long authorId, String status) {}
    private record Poll(long id, Instant endsAt) {}
}
