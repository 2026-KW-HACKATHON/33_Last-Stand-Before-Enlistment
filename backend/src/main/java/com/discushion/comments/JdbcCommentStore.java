package com.discushion.comments;

import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;

final class JdbcCommentStore {
    record Row(long id, long postId, Long parent, Long target, String content, String targetName,
               String name, boolean guest, boolean institution, long likes, long dislikes, String evaluation, Instant at) {}
    private final JdbcTemplate jdbc;
    JdbcCommentStore(DataSource source) { jdbc = new JdbcTemplate(source); }
    Optional<Row> find(long id, Long viewer, Instant now) { return rows("c.id=?", List.of(id), viewer, now).stream().findFirst(); }
    List<Row> rows(String where, List<Object> values, Long viewer, Instant now) {
        var args = new ArrayList<Object>(); args.add(Timestamp.from(now)); args.add(Timestamp.from(now)); args.add(viewer); args.addAll(values);
        return jdbc.query("""
            select c.*,p.nickname,
              exists(select 1 from discushion.institution_credentials ic where ic.user_id=c.author_user_id
                and ic.completed_at<=? and ?<ic.valid_until) institution,
              (select count(*) from discushion.comment_evaluations e where e.comment_id=c.id and e.evaluation_type='LIKE') likes,
              (select count(*) from discushion.comment_evaluations e where e.comment_id=c.id and e.evaluation_type='DISLIKE') dislikes,
              (select e.evaluation_type from discushion.comment_evaluations e where e.comment_id=c.id and e.user_id=?) evaluation
            from discushion.comments c left join discushion.profiles p on p.user_id=c.author_user_id where
            """ + where, (rs, n) -> {
                boolean guest = rs.getString("author_kind").equals("GUEST");
                String name = guest ? "게스트" : rs.getString("nickname");
                if (name == null) throw new IllegalStateException("Member comment has no public profile");
                return new Row(rs.getLong("id"), rs.getLong("post_id"), (Long) rs.getObject("parent_comment_id"),
                    (Long) rs.getObject("reply_to_comment_id"), rs.getString("content"), rs.getString("reply_to_display_name"),
                    name, guest, !guest && rs.getBoolean("institution"), rs.getLong("likes"), rs.getLong("dislikes"),
                    rs.getString("evaluation"), rs.getTimestamp("created_at").toInstant());
            }, args.toArray());
    }
    long insert(long postId, Long parent, Long target, String targetName, Long author, String content, Instant now) {
        return jdbc.queryForObject("""
            insert into discushion.comments(post_id,parent_comment_id,reply_to_comment_id,reply_to_display_name,
              author_user_id,author_kind,content,created_at) values(?,?,?,?,?,?,?,?) returning id
            """, Long.class, postId, parent, target, targetName, author, author == null ? "GUEST" : "MEMBER", content, Timestamp.from(now));
    }
    Map<String, Object> view(Row row, boolean member, List<Map<String, Object>> replies) {
        var out = new LinkedHashMap<String, Object>();
        out.put("id", row.id()); out.put("postId", row.postId()); out.put("parentCommentId", row.parent()); out.put("content", row.content());
        out.put("author", Map.of("displayName", row.name(), "isGuest", row.guest(), "institutionVerified", row.institution()));
        out.put("replyTo", row.target() == null ? null : Map.of("commentId", row.target(), "displayName", row.targetName()));
        out.put("likeCount", row.likes()); out.put("dislikeCount", row.dislikes());
        out.put("createdAt", row.at().atOffset(ZoneOffset.ofHours(9))); out.put("replies", replies);
        if (member) out.put("myEvaluation", row.evaluation());
        return out;
    }
}
