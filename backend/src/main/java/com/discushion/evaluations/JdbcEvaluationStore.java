package com.discushion.evaluations;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;

final class JdbcEvaluationStore {
    private final JdbcTemplate jdbc;
    JdbcEvaluationStore(DataSource source){jdbc=new JdbcTemplate(source);}
    long postId(long commentId) {
        return jdbc.query("select post_id from discushion.comments where id=?",(rs,row)->rs.getLong(1),commentId).stream().findFirst()
            .orElseThrow(()->new EvaluationFailure("COMMENT_NOT_FOUND",404,"commentId"));
    }
    void set(long id,long user,EvaluationInput.Type type,Instant now) {
        if(type==null)jdbc.update("delete from discushion.comment_evaluations where comment_id=? and user_id=?",id,user);
        else jdbc.update("""
            insert into discushion.comment_evaluations as stored(comment_id,user_id,evaluation_type,created_at,updated_at)
              values(?,?,?,?,?) on conflict(comment_id,user_id) do update
              set evaluation_type=excluded.evaluation_type,updated_at=excluded.updated_at
              where stored.evaluation_type<>excluded.evaluation_type
            """,id,user,type.name(),Timestamp.from(now),Timestamp.from(now));
    }
    Map<String,Object> snapshot(long id,long viewer) {
        return jdbc.queryForObject("""
            select count(*) filter(where evaluation_type='LIKE') likes,
              count(*) filter(where evaluation_type='DISLIKE') dislikes,
              max(evaluation_type) filter(where user_id=?) mine
            from discushion.comment_evaluations where comment_id=?
            """,(rs,row)->{
                var out=new LinkedHashMap<String,Object>();out.put("commentId",id);out.put("myEvaluation",rs.getString("mine"));
                out.put("likeCount",rs.getLong("likes"));out.put("dislikeCount",rs.getLong("dislikes"));return out;
            },viewer,id);
    }
}
