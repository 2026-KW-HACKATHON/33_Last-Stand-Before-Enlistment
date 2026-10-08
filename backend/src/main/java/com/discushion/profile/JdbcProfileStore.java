package com.discushion.profile;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import com.discushion.identity.IdentityFailure;

final class JdbcProfileStore {
    final JdbcTemplate jdbc;
    JdbcProfileStore(DataSource source) {jdbc=new JdbcTemplate(source);}
    long completedUser(String subject) {
        var users=jdbc.query("select id,registration_completed_at from discushion.users where privy_user_id=?",
            (rs,row)->{if(rs.getTimestamp(2)==null) throw new IdentityFailure(IdentityFailure.Reason.INCOMPLETE);return rs.getLong(1);},subject);
        if(users.isEmpty()) throw new IdentityFailure(IdentityFailure.Reason.NOT_REGISTERED);
        return users.get(0);
    }
    ProfileView read(long userId,Instant now) {
        var data=jdbc.queryForMap("""
            select u.email,p.nickname,p.bio,r.id region_id,r.name region_name
            from discushion.users u join discushion.profiles p on p.user_id=u.id
            join discushion.regions r on r.id=p.activity_region_id where u.id=?
            """,userId);
        var attributes=jdbc.queryForList("select attribute from discushion.profile_attributes where user_id=? order by attribute",String.class,userId);
        var regions=jdbc.query("""
            select r.id,r.name from discushion.neighbor_verified_regions n join discushion.regions r on r.id=n.region_id
            where n.user_id=? order by r.id
            """,(rs,row)->new ProfileView.Region(rs.getLong(1),rs.getString(2)),userId);
        var institutions=jdbc.query("""
            select i.name,r.id,r.name,c.completed_at,c.valid_until from discushion.institution_credentials c
            join discushion.institutions i on i.id=c.institution_id join discushion.regions r on r.id=c.responsible_region_id
            where c.user_id=? order by (c.completed_at<=? and ?<c.valid_until) desc,c.completed_at desc,c.id desc limit 1
            """,(rs,row)->{
                var completed=rs.getTimestamp(4).toInstant();var until=rs.getTimestamp(5).toInstant();
                boolean active=!now.isBefore(completed) && now.isBefore(until);
                return new ProfileView.Institution(!now.isBefore(until)?"EXPIRED":"COMPLETED",rs.getString(1),
                    new ProfileView.Region(rs.getLong(2),rs.getString(3)),completed.atOffset(ZoneOffset.ofHours(9)),
                    until.atOffset(ZoneOffset.ofHours(9)),active);
            },userId,Timestamp.from(now),Timestamp.from(now));
        var institution=institutions.isEmpty()?new ProfileView.Institution("NOT_SUBMITTED",null,null,null,null,false):institutions.get(0);
        // Photo URL resolution and mutation are deferred until the separate profile-photo contract is agreed.
        var profile=new ProfileView.Profile((String)data.get("nickname"),(String)data.get("bio"),null,List.copyOf(attributes),
            new ProfileView.Region(((Number)data.get("region_id")).longValue(),(String)data.get("region_name")));
        return new ProfileView(userId,(String)data.get("email"),profile,List.copyOf(regions),institution,institution.isActive());
    }
    void update(long userId,ProfilePatch patch,Instant now) {
        var current=jdbc.queryForMap("select nickname,bio,activity_region_id from discushion.profiles where user_id=? for update",userId);
        Map<String,Object> fields=patch.fields();
        if(fields.containsKey("activityRegionId") && !Boolean.TRUE.equals(jdbc.queryForObject(
                "select exists(select 1 from discushion.regions where id=?)",Boolean.class,fields.get("activityRegionId"))))
            throw new ProfileFailure("REGION_NOT_FOUND",404,"activityRegionId");
        jdbc.update("update discushion.profiles set nickname=?,bio=?,activity_region_id=?,updated_at=? where user_id=?",
            fields.getOrDefault("nickname",current.get("nickname")),fields.getOrDefault("bio",current.get("bio")),
            fields.getOrDefault("activityRegionId",current.get("activity_region_id")),Timestamp.from(now),userId);
        if(fields.containsKey("residentAttributes")) {
            jdbc.update("delete from discushion.profile_attributes where user_id=?",userId);
            for(Object attribute:(List<?>)fields.get("residentAttributes"))
                jdbc.update("insert into discushion.profile_attributes(user_id,attribute) values(?,?)",userId,attribute);
        }
    }
}
