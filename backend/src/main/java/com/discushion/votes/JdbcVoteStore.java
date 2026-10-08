package com.discushion.votes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;

final class JdbcVoteStore {
    private final JdbcTemplate jdbc;
    JdbcVoteStore(DataSource source){jdbc=new JdbcTemplate(source);}

    Long selectedOption(long pollId,long userId) {
        return jdbc.query("select option_id from discushion.vote_selections where poll_id=? and user_id=?",
            rs->rs.next()?rs.getLong(1):null,pollId,userId);
    }

    void select(long pollId,long userId,long optionId,Instant now) {
        jdbc.update("""
            insert into discushion.vote_selections(poll_id,user_id,option_id,first_submitted_at,updated_at)
            values(?,?,?,?,?)
            on conflict(poll_id,user_id) do update
              set option_id=excluded.option_id,updated_at=excluded.updated_at
              where discushion.vote_selections.option_id<>excluded.option_id
            """,pollId,userId,optionId,Timestamp.from(now),Timestamp.from(now));
    }

    Map<String,Object> snapshot(long postId,long pollId,List<Long> expectedOptionIds,long viewer,Instant endsAt,Instant now) {
        var rows=jdbc.query("""
            select o.id,o.content,count(v.user_id) vote_count
            from discushion.poll_options o
            left join discushion.vote_selections v on v.poll_id=o.poll_id and v.option_id=o.id
            where o.poll_id=?
            group by o.id,o.content,o.sort_order
            order by o.sort_order,o.id
            """,(rs,row)->Map.<String,Object>of("id",rs.getLong("id"),"content",rs.getString("content"),"voteCount",rs.getLong("vote_count")),pollId);
        var actualOptionIds=rows.stream().map(row->(Long)row.get("id")).toList();
        if(!actualOptionIds.equals(expectedOptionIds))throw new IllegalStateException("Vote options changed while building response");
        long total=rows.stream().mapToLong(row->(Long)row.get("voteCount")).sum();
        var options=rows.stream().map(row->{
            long count=(Long)row.get("voteCount");
            BigDecimal percentage=total==0?BigDecimal.ZERO.setScale(2):BigDecimal.valueOf(count)
                .multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(total),2,RoundingMode.HALF_UP);
            var option=new LinkedHashMap<String,Object>();option.put("id",row.get("id"));option.put("content",row.get("content"));
            option.put("voteCount",count);option.put("votePercentage",percentage);return option;
        }).toList();
        Long mine=selectedOption(pollId,viewer);
        var snapshot=new LinkedHashMap<String,Object>();snapshot.put("postId",postId);
        snapshot.put("status",now.isBefore(endsAt)?"OPEN":"CLOSED");snapshot.put("endsAt",endsAt);
        snapshot.put("participantCount",total);snapshot.put("myOptionId",mine);snapshot.put("options",options);
        return snapshot;
    }
}
