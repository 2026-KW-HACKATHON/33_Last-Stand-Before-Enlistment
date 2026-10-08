package com.discushion.home;

import com.discushion.identity.IdentityFailure;
import java.math.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;

/** One read snapshot, bounded cards, and a batch option query; never changes profile or creates content. */
final class JdbcHomeStore {
    private final JdbcTemplate jdbc; private final String origin,bucket;
    JdbcHomeStore(DataSource source,String origin,String bucket){jdbc=new JdbcTemplate(source);this.origin=origin;this.bucket=bucket;}
    record Region(long id,String name){}
    Instant now(){return jdbc.queryForObject("select current_timestamp",Timestamp.class).toInstant();}
    Region region(long userId,Long requested){
        var rows=jdbc.query("select u.registration_completed_at,p.activity_region_id from discushion.users u left join discushion.profiles p on p.user_id=u.id where u.id=?",
            (rs,n)->{if(rs.getTimestamp(1)==null)throw new IdentityFailure(IdentityFailure.Reason.INCOMPLETE);
                if(rs.getObject(2)==null)throw new IllegalStateException("Completed member profile missing");return rs.getLong(2);},userId);
        if(rows.isEmpty())throw new IdentityFailure(IdentityFailure.Reason.MEMBER_NOT_FOUND);
        long id=requested==null?rows.get(0):requested;
        var regions=jdbc.query("select id,name from discushion.regions where id=?",(rs,n)->new Region(rs.getLong(1),rs.getString(2)),id);
        if(regions.isEmpty())throw new HomeFailure(404,"REGION_NOT_FOUND",null);
        return regions.get(0);
    }
    Map<String,Long> counts(long region){
        var result=new LinkedHashMap<String,Long>();for(String type:List.of("LOCAL_AGENDA","LOCAL_ACTIVITY","VOTE"))result.put(type,0L);
        jdbc.query("select type,count(*) from discushion.posts where region_id=? and status='PUBLISHED' group by type",rs->{result.put(rs.getString(1),rs.getLong(2));},region);
        return result;
    }
    List<Map<String,Object>> cards(long region,long viewer,Instant now,boolean openOnly,int limit){
        var sql="""
            select p.id,p.type,p.topic,p.title,p.content,p.created_at,p.updated_at,r.id region_id,r.name region_name,
              f.nickname,a.activity_status,pl.id poll_id,pl.question,pl.ends_at,
              exists(select 1 from discushion.institution_credentials ic where ic.user_id=p.author_user_id
                and ic.completed_at<=? and ?<ic.valid_until) institution_verified,
              (select m.storage_key from discushion.post_photos ph join discushion.media_files m on m.id=ph.file_id
                where ph.post_id=p.id and m.purpose='POST_PHOTO' and m.lifecycle_status='LINKED'
                and m.owner_user_id=p.author_user_id order by ph.sort_order,ph.id limit 1) thumbnail_key,
              (select count(*) from discushion.comments c where c.post_id=p.id) comment_count,
              (select count(*) from discushion.post_reactions pr where pr.post_id=p.id and pr.reaction_type='EMPATHY') empathy,
              (select count(*) from discushion.post_reactions pr where pr.post_id=p.id and pr.reaction_type='NEEDED') needed,
              (select count(*) from discushion.post_reactions pr where pr.post_id=p.id and pr.reaction_type='CURIOUS') curious,
              (select v.option_id from discushion.vote_selections v where v.poll_id=pl.id and v.user_id=?) my_option_id
            from discushion.posts p join discushion.regions r on r.id=p.region_id
            join discushion.profiles f on f.user_id=p.author_user_id
            left join discushion.activity_post_details a on a.post_id=p.id
            left join discushion.polls pl on pl.post_id=p.id
            where p.region_id=? and p.status='PUBLISHED'
            """;
        var args=new ArrayList<Object>(List.of(Timestamp.from(now),Timestamp.from(now),viewer,region));
        if(openOnly){sql+=" and p.type='VOTE' and pl.ends_at>?";args.add(Timestamp.from(now));}
        sql+=" order by p.created_at desc,p.id desc limit ?";args.add(limit);
        return jdbc.query(sql,(rs,n)->{
            var card=new LinkedHashMap<String,Object>();card.put("id",rs.getLong("id"));card.put("type",rs.getString("type"));
            card.put("topic",rs.getString("topic"));card.put("title",rs.getString("title"));card.put("excerpt",rs.getString("content"));
            card.put("region",Map.of("id",rs.getLong("region_id"),"name",rs.getString("region_name")));
            var author=new LinkedHashMap<String,Object>();author.put("displayName",rs.getString("nickname"));author.put("profileImageUrl",null);
            author.put("institutionVerified",rs.getBoolean("institution_verified"));card.put("author",author);
            card.put("thumbnailUrl",publicUrl(rs.getString("thumbnail_key")));
            long e=rs.getLong("empathy"),ne=rs.getLong("needed"),c=rs.getLong("curious");
            card.put("reactionCounts",Map.of("EMPATHY",e,"NEEDED",ne,"CURIOUS",c,"total",e+ne+c));card.put("commentCount",rs.getLong("comment_count"));
            card.put("createdAt",rs.getTimestamp("created_at").toInstant().atOffset(ZoneOffset.ofHours(9)));
            card.put("updatedAt",rs.getTimestamp("updated_at").toInstant().atOffset(ZoneOffset.ofHours(9)));
            if("LOCAL_ACTIVITY".equals(rs.getString("type"))){
                String state=rs.getString("activity_status");if(state==null)throw new IllegalStateException("Activity source missing");card.put("activityStatus",state);
            }
            if("VOTE".equals(rs.getString("type"))){
                if(rs.getObject("poll_id")==null)throw new IllegalStateException("Poll source missing");
                var vote=new LinkedHashMap<String,Object>();vote.put("question",rs.getString("question"));
                var ends=rs.getTimestamp("ends_at").toInstant();vote.put("status",now.isBefore(ends)?"OPEN":"CLOSED");
                vote.put("endsAt",ends.atOffset(ZoneOffset.ofHours(9)));vote.put("myOptionId",rs.getObject("my_option_id"));
                card.put("vote",vote);
            }
            return (Map<String,Object>)card;
        },args.toArray());
    }
    @SafeVarargs final void fillVotes(List<Map<String,Object>>... lists){
        var ids=new LinkedHashSet<Long>();for(var list:lists)for(var card:list)if(card.containsKey("vote"))ids.add((Long)card.get("id"));
        if(ids.isEmpty())return;
        String slots=String.join(",",Collections.nCopies(ids.size(),"?"));
        var options=jdbc.query("select pl.post_id,o.id,o.content,count(v.user_id) vote_count from discushion.polls pl join discushion.poll_options o on o.poll_id=pl.id left join discushion.vote_selections v on v.poll_id=o.poll_id and v.option_id=o.id where pl.post_id in ("+slots+") group by pl.post_id,o.id,o.content,o.sort_order order by pl.post_id,o.sort_order,o.id",
            (rs,n)->new Option(rs.getLong(1),rs.getLong(2),rs.getString(3),rs.getLong(4)),ids.toArray());
        for(var list:lists)for(var card:list)if(card.get("vote") instanceof Map<?,?> raw){
            @SuppressWarnings("unchecked") var vote=(Map<String,Object>)raw;
            var own=options.stream().filter(o->o.postId==(Long)card.get("id")).toList();
            if(own.size()<2 || own.size()>10)throw new IllegalStateException("Poll options missing");
            long total=own.stream().mapToLong(Option::count).sum();vote.put("participantCount",total);
            vote.put("options",own.stream().map(o->Map.of("id",o.id,"content",o.content,"voteCount",o.count,
                "votePercentage",total==0?BigDecimal.ZERO.setScale(2):BigDecimal.valueOf(o.count).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(total),2,RoundingMode.HALF_UP))).toList());
        }
    }
    private record Option(long postId,long id,String content,long count){}
    private String publicUrl(String key){
        if(key==null)return null;
        if(origin==null || bucket==null || !origin.matches("https://[a-zA-Z0-9.-]+") || !bucket.matches("[a-zA-Z0-9_-]+"))throw new IllegalStateException("Public photo origin unavailable");
        return origin+"/storage/v1/object/public/"+bucket+"/"+Arrays.stream(key.split("/",-1)).map(v->URLEncoder.encode(v,StandardCharsets.UTF_8).replace("+","%20")).reduce((a,b)->a+"/"+b).orElseThrow();
    }
}
