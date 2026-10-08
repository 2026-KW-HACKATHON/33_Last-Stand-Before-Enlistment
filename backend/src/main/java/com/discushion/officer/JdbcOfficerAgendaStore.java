package com.discushion.officer;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;

final class JdbcOfficerAgendaStore {
    private final JdbcTemplate jdbc;

    JdbcOfficerAgendaStore(DataSource source) { this.jdbc = new JdbcTemplate(source); }

    List<Row> page(long institutionId, OfficerAgendaQuery query, OfficerAgendaCursor.Position after, int limit) {
        var sql = new StringBuilder("""
            with candidates as (
              select p.id post_id,p.created_at,r.id region_id,r.name region_name,
                     (select count(*) from discushion.post_reactions pr where pr.post_id=p.id) reaction_count,
                     a.id adoption_id,a.adopted_at
              from discushion.posts p
              join discushion.regions r on r.id=p.region_id
              left join discushion.institution_agenda_adoptions a
                on a.post_id=p.id and a.institution_id=? and a.canceled_at is null
              where p.status='PUBLISHED' and p.type='LOCAL_AGENDA'
            )
            select post_id,created_at,region_id,region_name,reaction_count,adoption_id,adopted_at
            from candidates where true
            """);
        var args = new ArrayList<Object>();
        args.add(institutionId);
        if (query.regionId() != null) {
            sql.append(" and region_id=?");
            args.add(query.regionId());
        }
        if (query.scope() == OfficerAgendaScope.ADOPTED) sql.append(" and adoption_id is not null");
        if (after != null) {
            sql.append(" and (reaction_count<? or (reaction_count=? and (created_at<? or (created_at=? and post_id<?))))");
            args.add(after.reactionCount());
            args.add(after.reactionCount());
            Timestamp createdAt = Timestamp.from(after.createdAt());
            args.add(createdAt);
            args.add(createdAt);
            args.add(after.postId());
        }
        sql.append(" order by reaction_count desc,created_at desc,post_id desc limit ?");
        args.add(limit);
        return jdbc.query(sql.toString(), (rs, row) -> new Row(rs.getLong("post_id"),
                rs.getTimestamp("created_at").toInstant(), rs.getLong("region_id"), rs.getString("region_name"),
                rs.getLong("reaction_count"), optionalLong(rs.getObject("adoption_id")),
                optionalInstant(rs.getTimestamp("adopted_at"))), args.toArray());
    }

    private static OptionalLong optionalLong(Object value) {
        return value == null ? OptionalLong.empty() : OptionalLong.of(((Number) value).longValue());
    }
    private static Optional<Instant> optionalInstant(Timestamp value) {
        return value == null ? Optional.empty() : Optional.of(value.toInstant());
    }

    record Row(long postId, Instant createdAt, long regionId, String regionName, long reactionCount,
            OptionalLong adoptionId, Optional<Instant> adoptedAt) {}
}
