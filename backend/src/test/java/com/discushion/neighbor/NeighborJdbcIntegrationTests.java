package com.discushion.neighbor;

import com.discushion.contracts.identity.*;
import com.discushion.identity.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import static org.assertj.core.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="DISCUSHION_TEST_JDBC_URL",matches="jdbc:postgresql://127\\.0\\.0\\.1:55432/discushion_migration_test(?:\\?.*)?")
class NeighborJdbcIntegrationTests {
    private static final Instant NOW=Instant.parse("2026-10-08T00:00:00Z");
    private final Clock clock=Clock.fixed(NOW,ZoneOffset.UTC);
    private final DriverManagerDataSource source=new DriverManagerDataSource(System.getenv("DISCUSHION_TEST_JDBC_URL"),"postgres",System.getenv("DISCUSHION_TEST_DB_PASSWORD"));
    private final JdbcNeighborStore store=new JdbcNeighborStore(source);
    private final JdbcMemberStore members=new JdbcMemberStore(source,clock);
    private final DataSourceTransactionManager manager=new DataSourceTransactionManager(source);
    private final AtomicReference<VerifiedActor> actor=new AtomicReference<>();
    private final CurrentActorProvider actors=()->Optional.ofNullable(actor.get());
    private NeighborService service;private NeighborDemoProvisioner seed;
    private String marker,subject;private long user,other;private final List<Long> regions=new ArrayList<>();
    @BeforeEach void setup() {
        marker="synthetic-neighbor11-"+UUID.randomUUID();subject="did:privy:"+marker;
        for(int index=0;index<4;index++) regions.add(store.jdbc.queryForObject("insert into discushion.regions(name) values(?) returning id",Long.class,marker+index));
        user=user(subject);other=user(subject+"-other");
        actor.set(new VerifiedActor(subject,Optional.of(new LocalMember(other,Optional.of(NOW)))));
        var read=new TransactionTemplate(manager);read.setReadOnly(true);read.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        service=new NeighborService(actors,store,read);seed=new NeighborDemoProvisioner(source,manager,clock);
    }
    private long user(String sub) {
        long id=store.jdbc.queryForObject("""
            insert into discushion.users(email,email_verified_at,created_at,updated_at,privy_user_id,registration_completed_at)
            values(?,?::timestamptz,?::timestamptz,?::timestamptz,?,?::timestamptz) returning id
            """,Long.class,marker+sub.hashCode()+"@example.invalid",NOW.toString(),NOW.toString(),NOW.toString(),sub,NOW.toString());
        store.jdbc.update("insert into discushion.profiles(user_id,nickname,activity_region_id,updated_at) values(?,?,?,?::timestamptz)",id,"n"+id,regions.get(0),NOW.toString());return id;
    }
    @AfterEach void cleanup() {
        for(long id:new long[]{user,other}) {
            store.jdbc.update("delete from discushion.institution_credentials where user_id=?",id);
            store.jdbc.update("delete from discushion.neighbor_verified_regions where user_id=?",id);
            store.jdbc.update("delete from discushion.profiles where user_id=?",id);
            store.jdbc.update("delete from discushion.users where id=?",id);
        }
        store.jdbc.update("delete from discushion.institutions where name=?",marker);
        for(long id:regions) store.jdbc.update("delete from discushion.regions where id=?",id);
    }
    @Test void listAndTargetUseVerifiedSubjectAndOnlySavedQualifications() {
        seed.add(other,regions.get(1));seed.add(user,regions.get(0));
        var list=service.current(null);assertThat(list.maxVerifiedRegions()).isEqualTo(3);assertThat(list.targetRegion()).isNull();
        assertThat(list.verifiedRegions()).extracting(NeighborView.VerifiedRegion::id).containsExactly(regions.get(0));
        assertThat(list.verifiedRegions().get(0).verifiedAt()).isEqualTo(NOW.atOffset(ZoneOffset.ofHours(9)));
        assertThat(service.current(regions.get(0).toString()).targetRegion().isNeighborVerified()).isTrue();
        assertThat(service.current(regions.get(1).toString()).targetRegion().isNeighborVerified()).isFalse();
        var tx=new TransactionTemplate(manager);var authorization=new MemberAuthorization(
            ()->Optional.of(new VerifiedActor(subject,Optional.of(new LocalMember(user,Optional.of(NOW))))),members,clock);
        tx.executeWithoutResult(status->{var current=authorization.lockCurrentCompletedMember();
            authorization.requireVerifiedRegion(current,regions.get(0));
            assertThatThrownBy(()->authorization.requireVerifiedRegion(current,regions.get(1))).isInstanceOf(IdentityFailure.class);});
    }
    @Test void activityRegionAndActiveInstitutionDoNotCreateNeighbourQualification() {
        long institution=store.jdbc.queryForObject("insert into discushion.institutions(name,created_at) values(?,?::timestamptz) returning id",Long.class,marker,NOW.toString());
        store.jdbc.update("insert into discushion.institution_credentials(user_id,institution_id,responsible_region_id,completed_at,valid_until) values(?,?,?,?::timestamptz,?::timestamptz)",
            user,institution,regions.get(0),NOW.minusSeconds(60).toString(),NOW.plusSeconds(60).toString());
        assertThat(service.current(regions.get(0).toString()).targetRegion().isNeighborVerified()).isFalse();
        assertThat(store.regions(user)).isEmpty();
    }
    @Test void thirdRegionIsAllowedFourthRejectedAndRepeatPreservesCompletionTime() {
        for(int index=0;index<3;index++) seed.add(user,regions.get(index));
        new NeighborDemoProvisioner(source,manager,Clock.fixed(NOW.plusSeconds(60),ZoneOffset.UTC)).add(user,regions.get(0));
        assertThatThrownBy(()->seed.add(user,regions.get(3))).isInstanceOf(IllegalStateException.class);
        assertThat(store.regions(user)).hasSize(3);assertThat(store.regions(user).get(0).verifiedAt().toInstant()).isEqualTo(NOW);
    }
    @Test void competingThirdAndFourthRegistrationsSerializeUnderCommonUserLock() throws Exception {
        seed.add(user,regions.get(0));seed.add(user,regions.get(1));
        var pool=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);
        try {
            java.util.function.Function<Long,String> add=id->{try{start.await();seed.add(user,id);return "OK";}
                catch(IllegalStateException limit){return "LIMIT";}catch(Exception error){throw new IllegalStateException(error);}};
            var a=pool.submit(()->add.apply(regions.get(2)));var b=pool.submit(()->add.apply(regions.get(3)));start.countDown();
            assertThat(List.of(a.get(10,TimeUnit.SECONDS),b.get(10,TimeUnit.SECONDS))).containsExactlyInAnyOrder("OK","LIMIT");
            assertThat(store.regions(user)).hasSize(3);
        } finally {pool.shutdownNow();pool.awaitTermination(5,TimeUnit.SECONDS);}
    }
    @Test void missingIncompleteAndAnonymousMembersCannotReadAndSeedInvalidTargetsNeverWrite() {
        actor.set(null);assertThatThrownBy(()->service.current(null)).isInstanceOf(IdentityFailure.class);
        actor.set(new VerifiedActor(subject+"-absent",Optional.empty()));assertThatThrownBy(()->service.current(null)).isInstanceOf(IdentityFailure.class);
        actor.set(new VerifiedActor(subject,Optional.empty()));store.jdbc.update("update discushion.users set registration_completed_at=null where id=?",user);
        assertThatThrownBy(()->service.current(null)).isInstanceOf(IdentityFailure.class);
        assertThatThrownBy(()->seed.add(user,regions.get(0))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->seed.add(other,9007199254740991L)).isInstanceOf(IllegalArgumentException.class);
        assertThat(store.regions(user)).isEmpty();assertThat(store.regions(other)).isEmpty();
    }
    @Test void unknownTargetIs404AndCorruptOverLimitSourceIsNotSilentlyTruncated() {
        assertThatThrownBy(()->service.current("9007199254740991")).isInstanceOfSatisfying(NeighborFailure.class,error->assertThat(error.code).isEqualTo("REGION_NOT_FOUND"));
        for(long id:regions) store.jdbc.update("insert into discushion.neighbor_verified_regions(user_id,region_id,verified_at) values(?,?,?::timestamptz)",user,id,NOW.toString());
        assertThatThrownBy(()->service.current(null)).isInstanceOf(IllegalStateException.class);
    }
}
