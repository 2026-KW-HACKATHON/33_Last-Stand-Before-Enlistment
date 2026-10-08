package com.discushion.personal;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/** Reads only current source relations. No history copies, activity writes or per-card queries. */
final class JdbcPersonalStore {
    private final NamedParameterJdbcTemplate jdbc;
    JdbcPersonalStore(DataSource source) { jdbc = new NamedParameterJdbcTemplate(source); }
    record Row(long postId, Instant at, Instant participatedAt) {}
    record Base(long id, String regionName, String nickname, boolean institutionVerified, String excerpt,
            String thumbnailKey, Instant updatedAt, String activityStatus, Long pollId, String question, Instant endsAt,
            boolean commented, boolean evaluated) {}
    record Option(long id, String content) {}
    List<Row> page(long user, PersonalQuery query, PersonalCursor.Position after, Instant now) {
        var args = new HashMap<String,Object>(); args.put("user", user); args.put("now", Timestamp.from(now)); args.put("limit", query.size()+1);
        String relation = switch (query.kind()) {
            case POSTS -> "select id post_id,created_at at,created_at participated_at from discushion.posts where author_user_id=:user";
            case VOTES -> "select p.post_id,v.updated_at at,v.first_submitted_at participated_at from discushion.vote_selections v join discushion.polls p on p.id=v.poll_id where v.user_id=:user";
            case PARTICIPATIONS -> """
                select post_id,max(at) at,max(at) participated_at from (
                  select post_id,created_at at from discushion.post_reactions where user_id=:user
                  union all select post_id,created_at from discushion.comments where author_kind='MEMBER' and author_user_id=:user
                  union all select c.post_id,e.updated_at from discushion.comment_evaluations e join discushion.comments c on c.id=e.comment_id where e.user_id=:user
                  union all select p.post_id,v.updated_at from discushion.vote_selections v join discushion.polls p on p.id=v.poll_id where v.user_id=:user
                ) actions group by post_id
                """;
        };
        String sql = "with mine as ("+relation+") select p.id,m.at,m.participated_at from mine m join discushion.posts p on p.id=m.post_id left join discushion.polls poll on poll.post_id=p.id where ";
        sql += query.kind()==PersonalQuery.Kind.VOTES && query.status()==PersonalQuery.Status.ALL
                ? "p.type='VOTE'" : "p.status='PUBLISHED'";
        if(query.kind()==PersonalQuery.Kind.VOTES) sql += " and p.type='VOTE'";
        if(query.type()!=null) { sql += " and p.type=:type"; args.put("type",query.type().name()); }
        if(query.status()!=PersonalQuery.Status.ALL) sql += query.status()==PersonalQuery.Status.OPEN ? " and poll.ends_at>:now" : " and poll.ends_at<=:now";
        if(after!=null) { sql += " and (m.at,p.id)<(:at,:id)"; args.put("at",Timestamp.from(after.at())); args.put("id",after.postId()); }
        sql += " order by m.at desc,p.id desc limit :limit";
        return jdbc.query(sql,args,(rs,n)->new Row(rs.getLong("id"),rs.getTimestamp("at").toInstant(),rs.getTimestamp("participated_at").toInstant()));
    }
    Map<Long,Base> bases(Set<Long> ids,long viewer,Instant now) {
        if(ids.isEmpty())return Map.of();
        var result=new LinkedHashMap<Long,Base>();
        jdbc.query("""
            select p.id,p.content,p.updated_at,r.name region_name,f.nickname,a.activity_status,
              poll.id poll_id,poll.question,poll.ends_at,
              exists(select 1 from discushion.institution_credentials ic where ic.user_id=p.author_user_id and ic.completed_at<=:now and :now<ic.valid_until) institution_verified,
              (select m.storage_key from discushion.post_photos ph join discushion.media_files m on m.id=ph.file_id
                where ph.post_id=p.id and m.owner_user_id=p.author_user_id and m.purpose='POST_PHOTO' and m.lifecycle_status='LINKED'
                order by ph.sort_order,ph.id limit 1) thumbnail_key,
              exists(select 1 from discushion.comments c where c.post_id=p.id and c.author_kind='MEMBER' and c.author_user_id=:viewer) commented,
              exists(select 1 from discushion.comment_evaluations e join discushion.comments c on c.id=e.comment_id where c.post_id=p.id and e.user_id=:viewer) evaluated
            from discushion.posts p join discushion.regions r on r.id=p.region_id
            join discushion.profiles f on f.user_id=p.author_user_id
            left join discushion.activity_post_details a on a.post_id=p.id
            left join discushion.polls poll on poll.post_id=p.id
            where p.id in (:ids) and p.status='PUBLISHED'
            """,Map.of("ids",ids,"viewer",viewer,"now",Timestamp.from(now)),rs->{
                long id=rs.getLong("id");var ends=rs.getTimestamp("ends_at");
                result.put(id,new Base(id,rs.getString("region_name"),rs.getString("nickname"),rs.getBoolean("institution_verified"),
                        rs.getString("content"),rs.getString("thumbnail_key"),rs.getTimestamp("updated_at").toInstant(),rs.getString("activity_status"),
                        (Long)rs.getObject("poll_id"),rs.getString("question"),ends==null?null:ends.toInstant(),rs.getBoolean("commented"),rs.getBoolean("evaluated")));
            });
        return Map.copyOf(result);
    }
    Map<Long,List<Option>> options(Set<Long> publishedPostIds) {
        if(publishedPostIds.isEmpty())return Map.of();
        var result=new LinkedHashMap<Long,List<Option>>();
        jdbc.query("""
            select poll.post_id,o.id,o.content from discushion.polls poll join discushion.poll_options o on o.poll_id=poll.id
            join discushion.posts p on p.id=poll.post_id where poll.post_id in (:ids) and p.status='PUBLISHED'
            order by poll.post_id,o.sort_order,o.id
            """,Map.of("ids",publishedPostIds),rs->{result.computeIfAbsent(rs.getLong("post_id"),id->new ArrayList<>()).add(new Option(rs.getLong("id"),rs.getString("content")));});
        return result;
    }
}
