package com.discushion.institution;

import com.discushion.contracts.identity.*;
import com.discushion.identity.*;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.support.TransactionTemplate;
import static org.assertj.core.api.Assertions.*;

/** Existing DB facts, shared authorization and the admin-only demo registration contract. */
@EnabledIfEnvironmentVariable(named = "DISCUSHION_TEST_JDBC_URL",
        matches = "jdbc:postgresql://127\\.0\\.0\\.1:55432/discushion_migration_test(?:\\?.*)?")
class InstitutionLookupJdbcIntegrationTests {
    private static final Instant NOW = Instant.parse("2026-10-08T00:00:00Z");
    private final DriverManagerDataSource source = new DriverManagerDataSource(
            System.getenv("DISCUSHION_TEST_JDBC_URL"), "postgres", System.getenv("DISCUSHION_TEST_DB_PASSWORD"));
    private final JdbcTemplate jdbc = new JdbcTemplate(source);
    private final DataSourceTransactionManager manager = new DataSourceTransactionManager(source);
    private final AtomicReference<VerifiedActor> actor = new AtomicReference<>();
    private final AtomicReference<Instant> time = new AtomicReference<>(NOW);
    private final Clock clock = new Clock() {
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return Clock.fixed(instant(), zone); }
        @Override public Instant instant() { return time.get(); }
    };
    private final InstitutionLookup lookup = new InstitutionLookup(() -> Optional.ofNullable(actor.get()), source, manager, clock);
    private String marker, subject;
    private long user, other, institution, region, otherRegion;

    @BeforeEach void setup() {
        marker = "synthetic-institution12-" + UUID.randomUUID();
        subject = "did:privy:" + marker;
        region = jdbc.queryForObject("insert into discushion.regions(name) values(?) returning id", Long.class, marker);
        otherRegion = jdbc.queryForObject("insert into discushion.regions(name) values(?) returning id", Long.class, marker + "-other");
        institution = jdbc.queryForObject("insert into discushion.institutions(name,created_at) values(?,?) returning id",
                Long.class, marker, Timestamp.from(NOW));
        user = user(subject);
        other = user(subject + "-other");
        // Deliberately stale/wrong member snapshot: only the verified subject may select the owner.
        actor.set(new VerifiedActor(subject, Optional.of(new LocalMember(other, Optional.of(NOW)))));
    }

    private long user(String sub) {
        return jdbc.queryForObject("""
                insert into discushion.users(email,email_verified_at,created_at,updated_at,privy_user_id,registration_completed_at)
                values(?,?,?,?,?,?) returning id
                """, Long.class, sub.substring("did:privy:".length()) + "@example.invalid",
                Timestamp.from(NOW), Timestamp.from(NOW), Timestamp.from(NOW), sub, Timestamp.from(NOW));
    }

    private long credential(long owner, long responsibleRegion, Instant completedAt, Instant validUntil) {
        return jdbc.queryForObject("""
                insert into discushion.institution_credentials(user_id,institution_id,responsible_region_id,completed_at,valid_until)
                values(?,?,?,?,?) returning id
                """, Long.class, owner, institution, responsibleRegion, Timestamp.from(completedAt), Timestamp.from(validUntil));
    }

    @AfterEach void cleanup() {
        for (long id : new long[]{user, other}) {
            jdbc.update("delete from discushion.institution_credentials where user_id=?", id);
            jdbc.update("delete from discushion.neighbor_verified_regions where user_id=?", id);
            jdbc.update("delete from discushion.profiles where user_id=?", id);
            jdbc.update("delete from discushion.users where id=?", id);
        }
        jdbc.update("delete from discushion.institutions where id=?", institution);
        jdbc.update("delete from discushion.regions where id in (?,?)", region, otherRegion);
    }

    @Test void selectsStoredFactsBySubjectWithInstitutionAndRegionNamesWithoutTruncatingHistory() {
        long expired = credential(user, region, NOW.minusSeconds(120), NOW.minusSeconds(60));
        long active = credential(user, region, NOW.minusSeconds(30), NOW.plusSeconds(30));
        credential(other, otherRegion, NOW.minusSeconds(30), NOW.plusSeconds(30));
        var snapshot = lookup.current();
        assertThat(snapshot.userId()).isEqualTo(user);
        assertThat(snapshot.credentials()).extracting(InstitutionSnapshot.Credential::id).containsExactly(expired, active);
        assertThat(snapshot.credentials()).allSatisfy(row -> {
            assertThat(row.institutionId()).isEqualTo(institution);
            assertThat(row.institutionName()).isEqualTo(marker);
            assertThat(row.responsibleRegionId()).isEqualTo(region);
            assertThat(row.responsibleRegionName()).isEqualTo(marker);
        });
        assertThat(snapshot.hasActiveInstitution()).isTrue();
        assertThat(jdbc.queryForObject("select count(*) from discushion.institution_credentials where user_id=?", Integer.class, user)).isEqualTo(2);
    }

    @Test void completionIsInclusiveExpiryExclusiveAndEachLookupReevaluatesClock() {
        credential(user, region, NOW, NOW.plusSeconds(60));
        time.set(NOW.minusNanos(1));
        assertThat(lookup.current().hasActiveInstitution()).isFalse();
        time.set(NOW);
        assertThat(lookup.current().hasActiveInstitution()).isTrue();
        time.set(NOW.plusSeconds(60).minusNanos(1));
        assertThat(lookup.current().responsibleFor(region)).isTrue();
        time.set(NOW.plusSeconds(60));
        var expired = lookup.current();
        assertThat(expired.hasActiveInstitution()).isFalse();
        assertThat(expired.responsibleFor(region)).isFalse();
        assertThat(expired.credentials()).hasSize(1);
        assertThat(expired.evaluatedAt()).isEqualTo(NOW.plusSeconds(60));
    }

    @Test void regionEligibilityAgreesWithActualCommonMemberGuardAndDoesNotGrantNeighborQualification() {
        credential(user, region, NOW.minusSeconds(1), NOW.plusSeconds(60));
        var snapshot = lookup.current();
        assertThat(snapshot.hasActiveInstitution()).isTrue();
        assertThat(snapshot.responsibleFor(region)).isTrue();
        assertThat(snapshot.responsibleFor(otherRegion)).isFalse();
        var store = new JdbcMemberStore(source, clock);
        var auth = new MemberAuthorization(() -> Optional.of(new VerifiedActor(subject,
                Optional.of(new LocalMember(user, Optional.of(NOW))))), store, clock);
        new TransactionTemplate(manager).executeWithoutResult(status -> {
            var member = auth.lockCurrentCompletedMember();
            auth.requireActiveInstitution(member, Optional.empty());
            auth.requireActiveInstitution(member, Optional.of(region));
            assertReason(() -> auth.requireActiveInstitution(member, Optional.of(otherRegion)), IdentityFailure.Reason.INSTITUTION_REQUIRED);
            assertReason(() -> auth.requireVerifiedRegion(member, region), IdentityFailure.Reason.REGION_REQUIRED);
        });
        time.set(NOW.plusSeconds(60));
        new TransactionTemplate(manager).executeWithoutResult(status -> {
            var member = auth.lockCurrentCompletedMember();
            assertReason(() -> auth.requireActiveInstitution(member, Optional.empty()), IdentityFailure.Reason.INSTITUTION_REQUIRED);
        });
    }

    @Test void signupActivityRegionAndNeighborQualificationDoNotCreateInstitutionAuthority() {
        jdbc.update("insert into discushion.profiles(user_id,nickname,activity_region_id,updated_at) values(?,?,?,?)",
                user, "i" + user, region, Timestamp.from(NOW));
        jdbc.update("insert into discushion.neighbor_verified_regions(user_id,region_id,verified_at) values(?,?,?)",
                user, region, Timestamp.from(NOW));
        var snapshot = lookup.current();
        assertThat(snapshot.credentials()).isEmpty();
        assertThat(snapshot.hasActiveInstitution()).isFalse();
        assertThat(snapshot.responsibleFor(region)).isFalse();
    }

    @Test void anonymousMissingAndCurrentlyIncompleteMembersAreRejectedWithoutChangingCredentials() {
        credential(user, region, NOW.minusSeconds(1), NOW.plusSeconds(60));
        actor.set(null);
        assertReason(lookup::current, IdentityFailure.Reason.INVALID_TOKEN);
        actor.set(new VerifiedActor(subject + "-missing", Optional.of(new LocalMember(user, Optional.of(NOW)))));
        assertReason(lookup::current, IdentityFailure.Reason.NOT_REGISTERED);
        actor.set(new VerifiedActor(subject, Optional.of(new LocalMember(user, Optional.of(NOW)))));
        jdbc.update("update discushion.users set registration_completed_at=null where id=?", user);
        assertReason(lookup::current, IdentityFailure.Reason.INCOMPLETE);
        assertThat(jdbc.queryForObject("select count(*) from discushion.institution_credentials where user_id=?", Integer.class, user)).isEqualTo(1);
    }

    private InstitutionDemoProvisioner seed() { return new InstitutionDemoProvisioner(source, manager, clock); }

    @Test void demoRetryPreservesIdentityAndHistoryWhileOtherOverlappingPeriodsAreRejected() {
        long first = seed().add(user, institution, region, NOW);
        time.set(NOW.plusSeconds(60));
        assertThat(seed().add(user, institution, region, NOW.plusNanos(1))).isEqualTo(first);
        assertThatThrownBy(() -> seed().add(user, institution, otherRegion, NOW.plusSeconds(1))).isInstanceOf(IllegalStateException.class);
        Instant expiry = NOW.atZone(ZoneId.of("Asia/Seoul")).plusYears(1).toInstant();
        long renewed = seed().add(user, institution, otherRegion, expiry);
        assertThat(renewed).isNotEqualTo(first);
        var snapshot = lookup.current();
        assertThat(snapshot.credentials()).hasSize(2);
        assertThat(snapshot.responsibleFor(region)).isTrue();
        assertThat(snapshot.responsibleFor(otherRegion)).isFalse();
        time.set(expiry);
        assertThat(lookup.current().responsibleFor(region)).isFalse();
        assertThat(lookup.current().responsibleFor(otherRegion)).isTrue();
        assertThat(jdbc.queryForObject("select count(*) from discushion.institution_credentials where user_id=? and request_id is null", Integer.class, user)).isEqualTo(2);
    }

    @Test void leapDayCompletionUsesNextFebruary28AtTheSameKoreanTime() {
        Instant completed = ZonedDateTime.of(2024, 2, 29, 23, 30, 0, 0, ZoneId.of("Asia/Seoul")).toInstant();
        seed().add(user, institution, region, completed);
        var row = lookup.current().credentials().get(0);
        assertThat(row.validUntil()).isEqualTo(ZonedDateTime.of(2025, 2, 28, 23, 30, 0, 0, ZoneId.of("Asia/Seoul")).toInstant());
        assertThat(lookup.current().hasActiveInstitution()).isFalse();
    }

    @Test void futureCredentialIsNotActiveAndItsOverlappingFutureReplacementIsRejected() {
        seed().add(user, institution, region, NOW.plusSeconds(86400));
        assertThatThrownBy(() -> seed().add(user, institution, otherRegion, NOW.plusSeconds(172800))).isInstanceOf(IllegalStateException.class);
        assertThat(lookup.current().hasActiveInstitution()).isFalse();
        assertThat(lookup.current().credentials()).hasSize(1);
    }

    @Test void concurrentDemoRegistrationsSerializeToOneCredentialUnderTheSharedUserLock() throws Exception {
        var executor = Executors.newFixedThreadPool(2);
        var start = new CountDownLatch(1);
        java.util.function.Function<Long, String> add = target -> {
            try { start.await(); seed().add(user, institution, target, NOW); return "OK"; }
            catch (IllegalStateException denied) { return "OVERLAP"; }
            catch (Exception error) { throw new IllegalStateException(error); }
        };
        try {
            var first = executor.submit(() -> add.apply(region));
            var second = executor.submit(() -> add.apply(otherRegion));
            start.countDown();
            assertThat(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS))).containsExactlyInAnyOrder("OK", "OVERLAP");
            assertThat(lookup.current().credentials()).hasSize(1);
        } finally { executor.shutdownNow(); executor.awaitTermination(5, TimeUnit.SECONDS); }
    }

    @Test void invalidDemoTargetsIncompleteMembersAndAmbientTransactionsNeverWrite() {
        assertThatThrownBy(() -> seed().add(user, 9007199254740991L, region, NOW)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> seed().add(user, institution, 9007199254740991L, NOW)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> seed().add(0, institution, region, NOW)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> seed().add(user, institution, region, null)).isInstanceOf(NullPointerException.class);
        new TransactionTemplate(manager).executeWithoutResult(status ->
                assertThatThrownBy(() -> seed().add(user, institution, region, NOW)).isInstanceOf(IllegalStateException.class));
        jdbc.update("update discushion.users set registration_completed_at=null where id=?", user);
        assertThatThrownBy(() -> seed().add(user, institution, region, NOW)).isInstanceOf(IllegalArgumentException.class);
        assertThat(jdbc.queryForObject("select count(*) from discushion.institution_credentials where user_id=?", Integer.class, user)).isZero();
    }

    private static void assertReason(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, IdentityFailure.Reason reason) {
        assertThatThrownBy(call).isInstanceOfSatisfying(IdentityFailure.class, failure -> assertThat(failure.reason()).isEqualTo(reason));
    }
}
