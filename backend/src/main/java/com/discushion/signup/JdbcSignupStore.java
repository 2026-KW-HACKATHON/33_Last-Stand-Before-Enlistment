package com.discushion.signup;

import com.discushion.identity.VerifiedEmail;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

final class JdbcSignupStore {
    /** User decision: MVP displays a frontend UI only; this is not a published document version. */
    static final String AGREEMENT_RECORD_MARKER="MVP_UI_ONLY";
    record Member(long id,String email,Instant completed) {}
    final JdbcTemplate jdbc;
    private final DataSource source;
    JdbcSignupStore(DataSource source) { this.source=source; this.jdbc=new JdbcTemplate(source); }
    private void requireTransaction() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || TransactionSynchronizationManager.isCurrentTransactionReadOnly()
                || !TransactionSynchronizationManager.hasResource(source))
            throw new IllegalStateException("Signup requires the caller's writable JDBC transaction");
    }
    Optional<Member> lock(String subject) {
        requireTransaction();
        return jdbc.query("select id,email,registration_completed_at from discushion.users where privy_user_id=? for update",
            (rs,row)->{var completed=rs.getTimestamp("registration_completed_at");
                return new Member(rs.getLong("id"),rs.getString("email"),completed==null?null:completed.toInstant());},subject)
            .stream().findFirst();
    }
    void insertIfAbsent(VerifiedEmail email,Instant now) {
        requireTransaction();
        jdbc.update("""
            insert into discushion.users(email,email_verified_at,created_at,updated_at,privy_user_id)
            values(?,?,?,?,?) on conflict(privy_user_id) do nothing
            """,email.address(),Timestamp.from(email.verifiedAt()),Timestamp.from(now),Timestamp.from(now),email.privySubject());
    }
    void save(long id,SignupInput input,VerifiedEmail email,Instant now) {
        requireTransaction();
        if (!Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from discushion.regions where id=?)",Boolean.class,input.regionId())))
            throw new SignupFailure(SignupFailure.Reason.REGION_NOT_FOUND,"profile.activityRegionId");
        jdbc.update("""
            insert into discushion.profiles(user_id,nickname,bio,activity_region_id,updated_at) values(?,?,?,?,?)
            on conflict(user_id) do update set nickname=excluded.nickname,bio=excluded.bio,
              activity_region_id=excluded.activity_region_id,updated_at=excluded.updated_at
            """,id,input.nickname(),input.bio(),input.regionId(),Timestamp.from(now));
        jdbc.update("delete from discushion.profile_attributes where user_id=?",id);
        for(String attribute:input.attributes()) jdbc.update("insert into discushion.profile_attributes(user_id,attribute) values(?,?)",id,attribute);
        String[] types={"TERMS_OF_SERVICE","PRIVACY_COLLECTION","MARKETING"};
        boolean[] flags={input.terms(),input.privacy(),input.marketing()};
        for(int index=0;index<types.length;index++) jdbc.update("""
            insert into discushion.user_agreements(user_id,agreement_type,agreed,policy_version,recorded_at)
            values(?,?,?,?,?) on conflict(user_id,agreement_type) do update set
              agreed=excluded.agreed,policy_version=excluded.policy_version,recorded_at=excluded.recorded_at
            """,id,types[index],flags[index],AGREEMENT_RECORD_MARKER,Timestamp.from(now));
        jdbc.update("""
            update discushion.users set email_verified_at=?,registration_completed_at=?,updated_at=?
            where id=? and registration_completed_at is null
            """,Timestamp.from(email.verifiedAt()),Timestamp.from(now),Timestamp.from(now),id);
    }
}
