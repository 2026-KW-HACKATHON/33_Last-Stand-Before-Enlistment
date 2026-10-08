package com.discushion.identity;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.Optional;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.assertj.core.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="DISCUSHION_TEST_JDBC_URL", matches="jdbc:postgresql://127\\.0\\.0\\.1:55432/discushion_migration_test(?:\\?.*)?")
class JdbcMemberStoreIntegrationTests {
    private static final Instant NOW = Instant.parse("2026-10-07T00:00:00Z");
    private final DriverManagerDataSource source = new DriverManagerDataSource(System.getenv("DISCUSHION_TEST_JDBC_URL"),
        "postgres", System.getenv("DISCUSHION_TEST_DB_PASSWORD"));
    private final JdbcTemplate jdbc = new JdbcTemplate(source);
    private final TransactionTemplate tx = new TransactionTemplate(new DataSourceTransactionManager(source));
    private final JdbcMemberStore store = new JdbcMemberStore(source, Clock.fixed(NOW, ZoneOffset.UTC));

    private long user(String subject, boolean complete) {
        return jdbc.queryForObject("""
            insert into discushion.users(email,email_verified_at,created_at,updated_at,privy_user_id,registration_completed_at)
            values (?, '2026-10-06T00:00:00Z', '2026-10-06T00:00:00Z', '2026-10-06T00:00:00Z', ?, ?::timestamptz) returning id
            """, Long.class, "synthetic-issue4-"+UUID.randomUUID()+"@example.invalid", subject, complete ? NOW.toString() : null);
    }

    @Test void resolvesOnlyVerifiedSubjectAndPreservesThreeMembershipStates() {
        tx.executeWithoutResult(status -> {
            var subject = "did:privy:synthetic-issue4-"+UUID.randomUUID();
            assertThat(store.findByVerifiedSubject(subject)).isEmpty();
            var id = user(subject, false);
            assertThat(store.findByVerifiedSubject(subject).orElseThrow().userId()).isEqualTo(id);
            assertThat(store.findByVerifiedSubject(subject).orElseThrow().registrationCompletedAt()).isEmpty();
            jdbc.update("update discushion.users set registration_completed_at=?::timestamptz where id=?",NOW.toString(),id);
            assertThat(store.findByVerifiedSubject(subject).orElseThrow().registrationCompletedAt()).contains(NOW);
            assertThat(store.findByVerifiedSubject(subject+"-other")).isEmpty();
            status.setRollbackOnly();
        });
    }

    @Test void readsNeighbourSourceAndAllInstitutionFactsWithoutGrantingActivityRegion() {
        tx.executeWithoutResult(status -> {
            var id = user("did:privy:synthetic-issue4-"+UUID.randomUUID(), true);
            long activityRegion = jdbc.queryForObject("insert into discushion.regions(name) values ('synthetic-issue4-activity') returning id",Long.class);
            long verifiedRegion = jdbc.queryForObject("insert into discushion.regions(name) values ('synthetic-issue4-verified') returning id",Long.class);
            jdbc.update("insert into discushion.profiles(user_id,nickname,activity_region_id,updated_at) values (?,?,?,?::timestamptz)",id,"i4"+id,activityRegion,NOW.toString());
            jdbc.update("insert into discushion.neighbor_verified_regions(user_id,region_id,verified_at) values (?,?,?::timestamptz)",id,verifiedRegion,NOW.toString());
            long institution = jdbc.queryForObject("insert into discushion.institutions(name,created_at) values ('synthetic-issue4-inst',?::timestamptz) returning id",Long.class,NOW.toString());
            jdbc.update("insert into discushion.institution_credentials(user_id,institution_id,responsible_region_id,completed_at,valid_until) values (?,?,?,?::timestamptz,?::timestamptz)",id,institution,activityRegion,NOW.minusSeconds(100).toString(),NOW.toString());
            var member = store.lockAndRead(id).orElseThrow();
            assertThat(member.verifiedRegionIds()).containsExactly(verifiedRegion).doesNotContain(activityRegion);
            assertThat(member.institutionGrants()).hasSize(1);
            assertThat(member.institutionGrants().get(0).validUntil()).isEqualTo(NOW);
            assertThat(member.evaluatedAt()).isEqualTo(NOW);
            status.setRollbackOnly();
        });
    }

    @Test void requiresCallerTransactionAndRejectsReadOnlyOrDifferentDataSource() {
        assertThatThrownBy(() -> store.lockAndRead(1)).isInstanceOf(IllegalStateException.class);
        var readOnly = new TransactionTemplate(new DataSourceTransactionManager(source));
        readOnly.setReadOnly(true);
        readOnly.executeWithoutResult(status -> assertThatThrownBy(() -> store.lockAndRead(1)).isInstanceOf(IllegalStateException.class));
        tx.executeWithoutResult(status -> {
            var other = new DriverManagerDataSource(System.getenv("DISCUSHION_TEST_JDBC_URL"),"postgres",System.getenv("DISCUSHION_TEST_DB_PASSWORD"));
            assertThatThrownBy(() -> new JdbcMemberStore(other,Clock.systemUTC()).lockAndRead(1)).isInstanceOf(IllegalStateException.class);
        });
    }

    @Test void nonexistentMemberAndRollbackDoNotCreateMembership() {
        tx.executeWithoutResult(status -> {
            assertThat(store.find(9007199254740991L)).isEmpty();
            assertThat(store.lockAndRead(9007199254740991L)).isEmpty();
            assertThatThrownBy(() -> store.find(0)).isInstanceOf(IllegalArgumentException.class);
        });
        var subject = "did:privy:synthetic-issue4-"+UUID.randomUUID();
        tx.executeWithoutResult(status -> {user(subject,true);status.setRollbackOnly();});
        assertThat(store.findByVerifiedSubject(subject)).isEmpty();
    }

    @Test void guardKeepsRowLockUntilCallerEndsTransaction() throws Exception {
        var subject = "did:privy:synthetic-issue4-lock-"+UUID.randomUUID();
        long id = user(subject,true);
        try(var owner = source.getConnection(); var contender = source.getConnection()) {
            var ownedSource = new SingleConnectionDataSource(owner,true);
            var ownedGuard = new JdbcMemberStore(ownedSource,Clock.fixed(NOW,ZoneOffset.UTC));
            var ownerTx = new TransactionTemplate(new DataSourceTransactionManager(ownedSource));
            contender.setAutoCommit(false);
            ownerTx.executeWithoutResult(status -> {
                assertThat(ownedGuard.lockAndRead(id)).isPresent();
                try(var statement=contender.createStatement()) {
                    statement.execute("set local lock_timeout='250ms'");
                    assertThatThrownBy(() -> statement.executeUpdate("update discushion.users set updated_at=updated_at where id="+id))
                        .isInstanceOfSatisfying(java.sql.SQLException.class,error -> assertThat(error.getSQLState()).isEqualTo("55P03"));
                    contender.rollback();
                } catch(java.sql.SQLException error) {throw new IllegalStateException(error);}
                status.setRollbackOnly();
            });
            try(var statement=contender.createStatement()) {
                assertThat(statement.executeUpdate("update discushion.users set updated_at=updated_at where id="+id)).isEqualTo(1);
            }
            contender.rollback();
        } finally {jdbc.update("delete from discushion.users where id=? and privy_user_id=?",id,subject);}
    }

    @Test void identityInsertAndGuardWorkWithScopedRlsRoleWithoutSequenceUsage() {
        tx.executeWithoutResult(status -> {
            String role = "discushion_i4_probe_"+UUID.randomUUID().toString().replace("-", "");
            jdbc.execute("create role "+role+" nologin nosuperuser nocreatedb nocreaterole noinherit nobypassrls");
            jdbc.execute("grant usage on schema discushion to "+role);
            jdbc.execute("grant select, insert, update on discushion.users to "+role);
            jdbc.execute("grant select on discushion.neighbor_verified_regions, discushion.institution_credentials to "+role);
            jdbc.execute("create policy discushion_i4_probe on discushion.users to "+role+" using(true) with check(true)");
            jdbc.execute("create policy discushion_i4_probe on discushion.neighbor_verified_regions for select to "+role+" using(true)");
            jdbc.execute("create policy discushion_i4_probe on discushion.institution_credentials for select to "+role+" using(true)");
            try {
                jdbc.execute("set local role "+role);
                assertThat(jdbc.queryForObject("select has_sequence_privilege(current_user,'discushion.users_id_seq','USAGE')",Boolean.class)).isFalse();
                var id=user("did:privy:synthetic-issue4-role-"+UUID.randomUUID(),true);
                assertThat(store.lockAndRead(id)).isPresent();
                assertThat(jdbc.queryForObject("select has_table_privilege(current_user,'discushion.users','DELETE')",Boolean.class)).isFalse();
                assertThat(jdbc.queryForObject("select rolsuper or rolbypassrls from pg_roles where rolname=current_user",Boolean.class)).isFalse();
            } finally {
                jdbc.execute("reset role");
                status.setRollbackOnly();
            }
        });
    }
}
