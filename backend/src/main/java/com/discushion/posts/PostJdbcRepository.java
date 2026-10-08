package com.discushion.posts;

import com.discushion.contracts.post.PollContext;
import com.discushion.contracts.post.PostContext;
import com.discushion.contracts.post.PostContextReader;
import com.discushion.contracts.post.PostStatus;
import com.discushion.contracts.post.PostType;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public final class PostJdbcRepository implements PostContextReader {
    private final JdbcTemplate jdbc;

    public PostJdbcRepository(DataSource source) { this.jdbc = new JdbcTemplate(source); }

    Instant databaseNow() {
        return jdbc.queryForObject("select clock_timestamp()", Timestamp.class).toInstant();
    }

    long insertPost(long authorId, PostCreationCommand command, Instant now) {
        return jdbc.queryForObject("""
                insert into discushion.posts(author_user_id,region_id,type,topic,title,content,status,content_revision,created_at,updated_at)
                values(?,?,?,?,?,?,'PUBLISHED',1,?,?) returning id
                """, Long.class, authorId, command.regionId(), command.type().name(), command.topic(),
                command.title(), command.content(), Timestamp.from(now), Timestamp.from(now));
    }

    void insertActivity(long postId, PostCreationCommand.Activity activity) {
        jdbc.update("""
                insert into discushion.activity_post_details(post_id,source,schedule,place,activity_status,external_participation_url)
                values(?,?,?,?,?,?)
                """, postId, activity.source(), activity.schedule(), activity.place(), activity.status(), activity.externalUrl());
    }

    void insertVote(long postId, PostCreationCommand.Vote vote) {
        long pollId = jdbc.queryForObject("insert into discushion.polls(post_id,question,ends_at) values(?,?,?) returning id",
                Long.class, postId, vote.question(), Timestamp.from(vote.endsAt()));
        for (int index = 0; index < vote.options().size(); index++) {
            jdbc.update("insert into discushion.poll_options(poll_id,content,sort_order) values(?,?,?)",
                    pollId, vote.options().get(index), index);
        }
    }

    @Override public Optional<PostContext> find(long postId) { return context(postId, false); }

    @Override public Optional<PostContext> findForUpdate(long postId) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || TransactionSynchronizationManager.isCurrentTransactionReadOnly())
            throw new IllegalStateException("Post write transaction is required");
        return context(postId, true);
    }

    private Optional<PostContext> context(long postId, boolean lock) {
        var rows = jdbc.query("select id,type,region_id,author_user_id,status from discushion.posts where id=?"
                        + (lock ? " for update" : ""),
                (rs, row) -> new Base(rs.getLong(1), rs.getString(2), rs.getLong(3), rs.getLong(4), rs.getString(5)), postId);
        if (rows.isEmpty()) return Optional.empty();
        Base base = rows.get(0);
        var polls = jdbc.query("select id,ends_at from discushion.polls where post_id=?"
                        + (lock ? " for update" : ""),
                (rs, row) -> new Poll(rs.getLong(1), rs.getTimestamp(2).toInstant()), postId);
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

    private record Base(long id, String type, long regionId, long authorId, String status) {}
    private record Poll(long id, Instant endsAt) {}
}
