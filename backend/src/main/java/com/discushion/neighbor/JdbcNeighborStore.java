package com.discushion.neighbor;

import com.discushion.identity.IdentityFailure;
import java.time.ZoneOffset;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;

final class JdbcNeighborStore {
    final JdbcTemplate jdbc;
    JdbcNeighborStore(DataSource source) {jdbc=new JdbcTemplate(source);}
    long currentCompletedUser(String subject) {
        var users=jdbc.query("select id,registration_completed_at from discushion.users where privy_user_id=?",
            (rs,row)->{if(rs.getTimestamp(2)==null) throw new IdentityFailure(IdentityFailure.Reason.INCOMPLETE);return rs.getLong(1);},subject);
        if(users.isEmpty()) throw new IdentityFailure(IdentityFailure.Reason.NOT_REGISTERED);
        return users.get(0);
    }
    List<NeighborView.VerifiedRegion> regions(long userId) {
        return jdbc.query("""
            select r.id,r.name,n.verified_at from discushion.neighbor_verified_regions n
            join discushion.regions r on r.id=n.region_id where n.user_id=? order by r.id
            """,(rs,row)->new NeighborView.VerifiedRegion(rs.getLong(1),rs.getString(2),
                rs.getTimestamp(3).toInstant().atOffset(ZoneOffset.ofHours(9))),userId);
    }
    boolean regionExists(long id) {
        return Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from discushion.regions where id=?)",Boolean.class,id));
    }
}
