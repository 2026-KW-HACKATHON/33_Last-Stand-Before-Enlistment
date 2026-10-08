package com.discushion.adoption;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;

final class JdbcAdoptionStore {
    private final JdbcTemplate jdbc;

    JdbcAdoptionStore(DataSource source) { this.jdbc = new JdbcTemplate(source); }

    Optional<AdoptionResponse> findCurrent(long postId, long institutionId) {
        return jdbc.query("""
                select a.id,a.post_id,i.name institution_name,a.adopted_at
                from discushion.institution_agenda_adoptions a
                join discushion.institutions i on i.id=a.institution_id
                where a.post_id=? and a.institution_id=? and a.canceled_at is null
                """, (rs, row) -> response(rs.getLong("id"), rs.getLong("post_id"),
                        rs.getString("institution_name"), rs.getTimestamp("adopted_at").toInstant()),
                postId, institutionId).stream().findFirst();
    }

    int insert(long postId, long institutionId, long userId, long credentialId, Instant adoptedAt) {
        return jdbc.update("""
                insert into discushion.institution_agenda_adoptions
                    (post_id,institution_id,adopted_by_user_id,credential_id,adopted_at)
                values (?,?,?,?,?)
                on conflict (post_id,institution_id) where canceled_at is null do nothing
                """, postId, institutionId, userId, credentialId, Timestamp.from(adoptedAt));
    }

    Optional<Row> findForUpdate(long postId, long adoptionId, long institutionId) {
        List<Row> rows = jdbc.query("""
                select id,adopted_at,canceled_at
                from discushion.institution_agenda_adoptions
                where id=? and post_id=? and institution_id=?
                for update
                """, (rs, row) -> new Row(rs.getLong("id"), rs.getTimestamp("adopted_at").toInstant(),
                        Optional.ofNullable(rs.getTimestamp("canceled_at")).map(Timestamp::toInstant)),
                adoptionId, postId, institutionId);
        return rows.stream().findFirst();
    }

    int cancel(long adoptionId, long institutionId, long canceledByUserId, Instant canceledAt) {
        return jdbc.update("""
                update discushion.institution_agenda_adoptions
                set canceled_at=?,canceled_by_user_id=?
                where id=? and institution_id=? and canceled_at is null
                """, Timestamp.from(canceledAt), canceledByUserId, adoptionId, institutionId);
    }

    private static AdoptionResponse response(long id, long postId, String institutionName, Instant adoptedAt) {
        return new AdoptionResponse(id, postId, institutionName, adoptedAt);
    }

    record Row(long id, Instant adoptedAt, Optional<Instant> canceledAt) {}
}
