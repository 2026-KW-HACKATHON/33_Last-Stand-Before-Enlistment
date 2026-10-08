package com.discushion.map;
import com.discushion.identity.IdentityFailure;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
final class JdbcDongStore {
    private final JdbcTemplate jdbc;private final String origin,bucket;
    JdbcDongStore(DataSource source,String origin,String bucket){jdbc=new JdbcTemplate(source);this.origin=origin;this.bucket=bucket;}
    long center(long user,Long supplied){
        var ids=jdbc.query("select u.registration_completed_at,p.activity_region_id from discushion.users u left join discushion.profiles p on p.user_id=u.id where u.id=?",(rs,n)->{
            if(rs.getTimestamp(1)==null)throw new IdentityFailure(IdentityFailure.Reason.INCOMPLETE);
            if(rs.getObject(2)==null)throw new IllegalStateException("Completed member profile missing");
            return rs.getLong(2);
        },user);
        if(ids.isEmpty())throw new IdentityFailure(IdentityFailure.Reason.MEMBER_NOT_FOUND);
        return supplied==null?ids.get(0):supplied;
    }
    Map<Long,Map<String,Object>> regions(Set<Long> ids){
        var result=new LinkedHashMap<Long,Map<String,Object>>();
        jdbc.query("select id,name from discushion.regions where id in("+slots(ids.size())+")",rs->{
            long id=rs.getLong(1);result.put(id,Map.of("id",id,"name",rs.getString(2)));
        },ids.toArray());return result;
    }
    Map<Long,Map<String,Object>> representatives(List<Long> ids){
        if(ids.isEmpty())return Map.of();
        var sql="""
            with candidates as (
              select p.id,p.region_id,p.author_user_id,p.type,p.title,p.created_at,
                (select count(*) from discushion.post_reactions pr where pr.post_id=p.id) reaction_count
              from discushion.posts p where p.status='PUBLISHED' and p.type in ('LOCAL_AGENDA','VOTE')
                and p.region_id in (%s)
            ), ranked as (
              select *,row_number() over(partition by region_id order by reaction_count desc,created_at desc,id desc) position from candidates
            )
            select r.*,
              (select m.storage_key from discushion.post_photos ph join discushion.media_files m on m.id=ph.file_id
                where ph.post_id=r.id and m.purpose='POST_PHOTO' and m.lifecycle_status='LINKED'
                  and m.owner_user_id=r.author_user_id order by ph.sort_order,ph.id limit 1) thumbnail_key
            from ranked r where position=1
            """.formatted(slots(ids.size()));
        var result=new LinkedHashMap<Long,Map<String,Object>>();
        jdbc.query(sql,rs->{
            var card=new LinkedHashMap<String,Object>();card.put("id",rs.getLong("id"));card.put("type",rs.getString("type"));
            card.put("title",rs.getString("title"));card.put("reactionCount",rs.getLong("reaction_count"));
            card.put("thumbnailUrl",publicUrl(rs.getString("thumbnail_key")));result.put(rs.getLong("region_id"),card);
        },ids.toArray());return result;
    }
    private static String slots(int size){return String.join(",",Collections.nCopies(size,"?"));}
    private String publicUrl(String key){
        if(key==null)return null;
        if(origin==null || bucket==null || !origin.matches("https://[a-zA-Z0-9.-]+") || !bucket.matches("[a-zA-Z0-9_-]+"))throw new IllegalStateException("Public photo origin unavailable");
        return origin+"/storage/v1/object/public/"+bucket+"/"+Arrays.stream(key.split("/",-1)).map(v->URLEncoder.encode(v,StandardCharsets.UTF_8).replace("+","%20")).reduce((a,b)->a+"/"+b).orElseThrow();
    }
}
