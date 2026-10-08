package com.discushion.personal;

import java.sql.Timestamp;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

final class JdbcBoardStore {
    private final NamedParameterJdbcTemplate jdbc;
    JdbcBoardStore(DataSource source){jdbc=new NamedParameterJdbcTemplate(source);}
    long region(long viewer,Long requested){
        Long id=requested;
        if(id==null){var found=jdbc.query("select activity_region_id from discushion.profiles where user_id=:user",Map.of("user",viewer),(rs,n)->(Long)rs.getObject(1));
            if(found.isEmpty()||found.get(0)==null)throw new IllegalStateException("Completed profile region unavailable");id=found.get(0);}
        if(Boolean.FALSE.equals(jdbc.queryForObject("select exists(select 1 from discushion.regions where id=:id)",Map.of("id",id),Boolean.class)))throw new BoardFailure();
        return id;
    }
    List<PersonalCursor.Position> page(long region,BoardQuery query,PersonalCursor.Position after){
        String sql="select id,created_at from discushion.posts where region_id=:region and status='PUBLISHED'";
        var args=new HashMap<String,Object>();args.put("region",region);args.put("limit",query.size()+1);
        if(query.type()!=null){sql+=" and type=:type";args.put("type",query.type().name());}
        if(query.topic()!=null){sql+=" and topic=:topic";args.put("topic",query.topic());}
        if(after!=null){sql+=" and (created_at,id)<(:at,:id)";args.put("at",Timestamp.from(after.at()));args.put("id",after.postId());}
        return jdbc.query(sql+" order by created_at desc,id desc limit :limit",args,(rs,n)->new PersonalCursor.Position(rs.getTimestamp("created_at").toInstant(),rs.getLong("id")));
    }
}
