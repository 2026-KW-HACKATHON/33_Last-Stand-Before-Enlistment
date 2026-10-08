package com.discushion.institution;

import com.discushion.identity.JdbcMemberStore;
import java.sql.Timestamp;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.*;

/**
 * #75 admin-only preparation contract. No bean/endpoint and no server-role write grant.
 * All cooperating qualification writers must take the same user lock. Arbitrary admin SQL
 * is outside this service-level overlap guard; no DB exclusion constraint is added here.
 */
public final class InstitutionDemoProvisioner {
    private final JdbcMemberStore members;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate writes;

    public InstitutionDemoProvisioner(DataSource adminSource, PlatformTransactionManager adminTransactions, Clock clock) {
        members = new JdbcMemberStore(adminSource, clock);
        jdbc = new JdbcTemplate(adminSource);
        writes = new TransactionTemplate(adminTransactions);
    }

    /** Returns the existing ID for an identical retry; rejects all other overlapping validity periods. */
    public long add(long userId, long institutionId, long regionId, Instant completedAt) {
        validId(userId); validId(institutionId); validId(regionId);
        Instant completed = Objects.requireNonNull(completedAt, "Missing completion time").truncatedTo(ChronoUnit.MICROS);
        Instant until = validUntil(completed);
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("Seed provisioning requires its own transaction");
        }
        return writes.execute(status -> {
            var member = members.lockAndRead(userId).orElseThrow(() -> new IllegalArgumentException("Unknown seed member"));
            if (member.registrationCompletedAt().isEmpty()) throw new IllegalArgumentException("Seed member must complete signup");
            if (!Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from discushion.institutions where id=?)", Boolean.class, institutionId))
                    || !Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from discushion.regions where id=?)", Boolean.class, regionId))) {
                throw new IllegalArgumentException("Unknown seed institution or region");
            }
            for (var grant : member.institutionGrants()) {
                if (grant.institutionId() == institutionId && grant.responsibleRegionId() == regionId
                        && grant.completedAt().equals(completed) && grant.validUntil().equals(until)) return grant.credentialId();
            }
            if (member.institutionGrants().stream().anyMatch(grant ->
                    completed.isBefore(grant.validUntil()) && grant.completedAt().isBefore(until))) {
                throw new IllegalStateException("Only one institution credential may be valid at a time");
            }
            return jdbc.queryForObject("""
                    insert into discushion.institution_credentials
                        (user_id,institution_id,responsible_region_id,request_id,completed_at,valid_until)
                    values(?,?,?,null,?,?) returning id
                    """, Long.class, userId, institutionId, regionId, Timestamp.from(completed), Timestamp.from(until));
        });
    }

    static Instant validUntil(Instant completed) {
        return completed.atZone(ZoneId.of("Asia/Seoul")).plusYears(1).toInstant();
    }

    private static void validId(long id) {
        if (id < 1 || id > 9007199254740991L) throw new IllegalArgumentException("Invalid seed identity");
    }
}
