package com.discushion.institution;

import com.discushion.contracts.identity.CurrentActorProvider;
import com.discushion.identity.IdentityFailure;
import com.discushion.identity.JdbcMemberStore;
import java.time.Clock;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/** Own-member snapshot; final institution actions still use the shared write guard. */
final class InstitutionLookup {
    private final CurrentActorProvider actors;
    private final JdbcMemberStore members;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate reads;
    private final Clock clock;

    InstitutionLookup(CurrentActorProvider actors, DataSource source,
            PlatformTransactionManager manager, Clock clock) {
        this.actors = actors;
        this.members = new JdbcMemberStore(source, clock);
        this.jdbc = new JdbcTemplate(source);
        this.clock = clock;
        this.reads = new TransactionTemplate(manager);
        reads.setReadOnly(true);
        reads.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
    }

    InstitutionSnapshot current() {
        var actor = actors.current().orElseThrow(() ->
                new IdentityFailure(IdentityFailure.Reason.INVALID_TOKEN));
        return reads.execute(status -> {
            var member = members.findByVerifiedSubject(actor.privySubject()).orElseThrow(() ->
                    new IdentityFailure(IdentityFailure.Reason.NOT_REGISTERED));
            if (member.registrationCompletedAt().isEmpty()) {
                throw new IdentityFailure(IdentityFailure.Reason.INCOMPLETE);
            }
            var credentials = jdbc.query("""
                    select c.id, c.institution_id, i.name, c.responsible_region_id, r.name,
                           c.completed_at, c.valid_until
                    from discushion.institution_credentials c
                    join discushion.institutions i on i.id = c.institution_id
                    join discushion.regions r on r.id = c.responsible_region_id
                    where c.user_id = ? order by c.id
                    """, (rs, row) -> new InstitutionSnapshot.Credential(
                            rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getLong(4), rs.getString(5),
                            rs.getTimestamp(6).toInstant(), rs.getTimestamp(7).toInstant()), member.userId());
            return new InstitutionSnapshot(member.userId(), clock.instant(), credentials);
        });
    }
}
