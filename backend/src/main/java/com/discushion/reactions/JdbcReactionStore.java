package com.discushion.reactions;

import com.discushion.contracts.participation.ParticipationSnapshot.ReactionType;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;

final class JdbcReactionStore {
    private final JdbcTemplate jdbc;
    JdbcReactionStore(DataSource source) { jdbc=new JdbcTemplate(source); }
    void set(long postId,long userId,ReactionType type,boolean selected,Instant now) {
        if(selected) jdbc.update("insert into discushion.post_reactions(post_id,user_id,reaction_type,created_at) values(?,?,?,?) on conflict(post_id,user_id,reaction_type) do nothing",postId,userId,type.name(),Timestamp.from(now));
        else jdbc.update("delete from discushion.post_reactions where post_id=? and user_id=? and reaction_type=?",postId,userId,type.name());
    }
    Map<String,Object> snapshot(long postId,long viewer) {
        return jdbc.queryForObject("""
            select count(*) filter(where reaction_type='EMPATHY') empathy,
              count(*) filter(where reaction_type='NEEDED') needed,
              count(*) filter(where reaction_type='CURIOUS') curious,
              coalesce(bool_or(user_id=? and reaction_type='EMPATHY'),false) my_empathy,
              coalesce(bool_or(user_id=? and reaction_type='NEEDED'),false) my_needed,
              coalesce(bool_or(user_id=? and reaction_type='CURIOUS'),false) my_curious
            from discushion.post_reactions where post_id=?
            """,(rs,row)->{
                long empathy=rs.getLong("empathy"),needed=rs.getLong("needed"),curious=rs.getLong("curious");
                var selected=new ArrayList<String>();
                if(rs.getBoolean("my_empathy"))selected.add("EMPATHY");
                if(rs.getBoolean("my_needed"))selected.add("NEEDED");
                if(rs.getBoolean("my_curious"))selected.add("CURIOUS");
                return Map.of("postId",postId,"reactionCounts",Map.of("EMPATHY",empathy,"NEEDED",needed,"CURIOUS",curious,"total",Math.addExact(Math.addExact(empathy,needed),curious)),"myReactions",selected);
            },viewer,viewer,viewer,postId);
    }
}
