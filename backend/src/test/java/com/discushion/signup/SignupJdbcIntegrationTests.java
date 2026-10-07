package com.discushion.signup;

import com.discushion.contracts.identity.VerifiedActor;
import com.discushion.identity.IdentityFailure;
import com.discushion.identity.VerifiedEmail;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@EnabledIfEnvironmentVariable(named="DISCUSHION_TEST_JDBC_URL",matches="jdbc:postgresql://127\\.0\\.0\\.1:55432/discushion_migration_test(?:\\?.*)?")
class SignupJdbcIntegrationTests {
    private static final Instant NOW=Instant.parse("2026-10-08T00:00:00Z");
    private final DriverManagerDataSource source=new DriverManagerDataSource(System.getenv("DISCUSHION_TEST_JDBC_URL"),"postgres",System.getenv("DISCUSHION_TEST_DB_PASSWORD"));
    private final JdbcSignupStore store=new JdbcSignupStore(source);
    private final TransactionTemplate tx=new TransactionTemplate(new DataSourceTransactionManager(source));
    private final Clock clock=Clock.fixed(NOW,ZoneOffset.UTC);
    private final AtomicReference<VerifiedActor> actor=new AtomicReference<>();
    private final AtomicReference<Optional<VerifiedEmail>> email=new AtomicReference<>();
    private String marker,subject,address,nickname;
    private long region;
    private int providerCalls;
    private final SignupService service=new SignupService(()->Optional.ofNullable(actor.get()),()->{
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();providerCalls++;return email.get();
    },store,tx,clock);
    @BeforeEach void setup() {
        marker="synthetic-signup7-"+UUID.randomUUID();
        subject="did:privy:"+marker;address=marker+"@example.invalid";nickname="n"+UUID.randomUUID().toString().substring(0,8);
        actor.set(new VerifiedActor(subject,Optional.empty()));
        email.set(Optional.of(new VerifiedEmail(subject,address,NOW.minusSeconds(60))));providerCalls=0;
        region=store.jdbc.queryForObject("insert into discushion.regions(name) values(?) returning id",Long.class,marker);
    }
    @AfterEach void cleanup() {
        tx.executeWithoutResult(status->{
            var ids=store.jdbc.queryForList("select id from discushion.users where email like ?",Long.class,marker+"%");
            for(long id:ids) {
                store.jdbc.update("delete from discushion.profile_attributes where user_id=?",id);
                store.jdbc.update("delete from discushion.user_agreements where user_id=?",id);
                store.jdbc.update("delete from discushion.profiles where user_id=?",id);
                store.jdbc.update("delete from discushion.users where id=?",id);
            }
            store.jdbc.update("delete from discushion.regions where id=? and name=?",region,marker);
        });
    }
    private SignupInput input() {return new SignupInput(true,true,false,nickname,"소개",List.of("RESIDENT","STUDENT"),region);}
    private int users() {return store.jdbc.queryForObject("select count(*) from discushion.users where email like ?",Integer.class,marker+"%");}
    private long incomplete(String localSubject,String localEmail) {
        return store.jdbc.queryForObject("""
            insert into discushion.users(email,email_verified_at,created_at,updated_at,privy_user_id)
            values(?,?,?,?,?) returning id
            """,Long.class,localEmail,Timestamp.from(NOW.minusSeconds(60)),Timestamp.from(NOW.minusSeconds(60)),Timestamp.from(NOW.minusSeconds(60)),localSubject);
    }
    @Test void atomicallyStoresMemberAgreementsProfileAndRegionWithoutGrantingQualifications() {
        var outcome=service.complete(input());long id=outcome.result().member().id();
        assertThat(outcome.completedNow()).isTrue();assertThat(outcome.result().registrationStatus()).isEqualTo("COMPLETED");
        assertThat(users()).isEqualTo(1);
        assertThat(store.jdbc.queryForObject("select password_hash is null and registration_completed_at is not null from discushion.users where id=?",Boolean.class,id)).isTrue();
        assertThat(store.jdbc.queryForObject("select activity_region_id from discushion.profiles where user_id=?",Long.class,id)).isEqualTo(region);
        assertThat(store.jdbc.queryForObject("select profile_image_file_id is null from discushion.profiles where user_id=?",Boolean.class,id)).isTrue();
        assertThat(store.jdbc.queryForObject("select count(*) from discushion.user_agreements where user_id=?",Integer.class,id)).isEqualTo(3);
        assertThat(store.jdbc.queryForList("select distinct policy_version from discushion.user_agreements where user_id=?",String.class,id))
            .containsExactly("MVP_UI_ONLY");
        assertThat(store.jdbc.queryForObject("select agreed from discushion.user_agreements where user_id=? and agreement_type='MARKETING'",Boolean.class,id)).isFalse();
        assertThat(store.jdbc.queryForList("select attribute from discushion.profile_attributes where user_id=? order by attribute",String.class,id)).containsExactly("RESIDENT","STUDENT");
        assertThat(store.jdbc.queryForObject("select count(*) from discushion.neighbor_verified_regions where user_id=?",Integer.class,id)).isZero();
        assertThat(store.jdbc.queryForObject("select count(*) from discushion.institution_credentials where user_id=?",Integer.class,id)).isZero();
    }
    @Test void completedRetryReturnsOriginalResultWithoutProviderLookupOrProfileOverwrite() {
        var first=service.complete(input());email.set(Optional.empty());
        var retry=service.complete(new SignupInput(true,true,true,"변경요청","다른소개",List.of(),region));
        assertThat(retry.completedNow()).isFalse();assertThat(retry.result()).isEqualTo(first.result());assertThat(providerCalls).isEqualTo(1);
        assertThat(store.jdbc.queryForObject("select nickname from discushion.profiles where user_id=?",String.class,first.result().member().id())).isEqualTo(nickname);
    }
    @Test void incompleteRegistrationResumesTheSameMember() {
        long id=incomplete(subject,address);
        assertThat(service.complete(input()).result().member().id()).isEqualTo(id);assertThat(users()).isEqualTo(1);
    }
    @Test void missingRequiredAgreementUnknownRegionAndMissingEmailNeverPartiallyCreateMember() {
        assertThatThrownBy(()->service.complete(new SignupInput(false,true,false,nickname,null,List.of(),region))).isInstanceOf(SignupFailure.class);
        assertThatThrownBy(()->service.complete(new SignupInput(true,true,false,nickname,null,List.of(),9007199254740991L)))
            .isInstanceOfSatisfying(SignupFailure.class,e->assertThat(e.reason).isEqualTo(SignupFailure.Reason.REGION_NOT_FOUND));
        email.set(Optional.empty());assertThatThrownBy(()->service.complete(input())).isInstanceOf(SignupFailure.class);
        assertThat(users()).isZero();
    }
    @Test void legacyEmailOrDifferentSubjectCannotBeAutomaticallyLinked() {
        for(String oldSubject:new String[]{null,"did:privy:other-"+marker}) {
            long id=incomplete(oldSubject,address);
            assertThatThrownBy(()->service.complete(input())).isInstanceOfSatisfying(SignupFailure.class,e->assertThat(e.reason).isEqualTo(SignupFailure.Reason.EMAIL_ALREADY_IN_USE));
            assertThat(store.jdbc.queryForObject("select registration_completed_at is null from discushion.users where id=?",Boolean.class,id)).isTrue();
            store.jdbc.update("delete from discushion.users where id=?",id);
        }
    }
    @Test void nicknameConflictRollsBackNewMember() {
        long other=incomplete("did:privy:other-"+marker,marker+"-other@example.invalid");
        store.jdbc.update("insert into discushion.profiles(user_id,nickname,activity_region_id,updated_at) values(?,?,?,?)",other,nickname,region,Timestamp.from(NOW));
        assertThatThrownBy(()->service.complete(input())).isInstanceOfSatisfying(SignupFailure.class,e->assertThat(e.reason).isEqualTo(SignupFailure.Reason.NICKNAME_ALREADY_IN_USE));
        assertThat(users()).isEqualTo(1);
        assertThat(store.jdbc.queryForObject("select count(*) from discushion.users where privy_user_id=?",Integer.class,subject)).isZero();
    }
    @Test void allWritesRollbackIfAFailureOccursAfterSavingEveryRow() {
        var failing=spy(new JdbcSignupStore(source));
        doAnswer(call->{call.callRealMethod();throw new IllegalStateException("synthetic-write-failure");})
            .when(failing).save(anyLong(),any(),any(),any());
        var failingService=new SignupService(()->Optional.of(actor.get()),()->email.get(),failing,tx,clock);
        assertThatThrownBy(()->failingService.complete(input())).isInstanceOf(IllegalStateException.class);
        assertThat(users()).isZero();
        assertThat(store.jdbc.queryForObject("select count(*) from discushion.profiles where nickname=?",Integer.class,nickname)).isZero();
    }
    @Test void uiOnlySignupDoesNotRewriteExistingIncompleteEmail() {
        long id=incomplete(subject,marker+"-old@example.invalid");
        assertThatThrownBy(()->service.complete(input())).isInstanceOf(SignupFailure.class);
        assertThat(store.jdbc.queryForObject("select email from discushion.users where id=?",String.class,id)).isEqualTo(marker+"-old@example.invalid");
    }
    @Test void anonymousAndMismatchedProviderSubjectCannotCreateMember() {
        actor.set(null);assertThatThrownBy(()->service.complete(input())).isInstanceOf(IdentityFailure.class);
        actor.set(new VerifiedActor(subject,Optional.empty()));
        email.set(Optional.of(new VerifiedEmail("did:privy:other",address,NOW.minusSeconds(60))));
        assertThatThrownBy(()->service.complete(input())).isInstanceOf(IllegalStateException.class);
        assertThat(users()).isZero();
    }
    @Test void serviceAndStoreRejectAmbientOrMissingWriteTransactions() {
        assertThatThrownBy(()->store.lock(subject)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(()->tx.execute(status->service.complete(input()))).isInstanceOf(IllegalStateException.class);
        assertThat(providerCalls).isZero();assertThat(users()).isZero();
    }
    @Test void simultaneousSameSubjectCreatesExactlyOneMemberAndOneCompletion() throws Exception {
        var entered=new CountDownLatch(2);var release=new CountDownLatch(1);
        var concurrent=new SignupService(()->Optional.of(actor.get()),()->{
            entered.countDown();try {if(!release.await(5,TimeUnit.SECONDS))throw new IllegalStateException("fixture timeout");}
            catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException(e);}return email.get();
        },store,tx,clock);
        var pool=Executors.newFixedThreadPool(2);
        try {
            var a=pool.submit(()->concurrent.complete(input()));var b=pool.submit(()->concurrent.complete(input()));
            assertThat(entered.await(5,TimeUnit.SECONDS)).isTrue();release.countDown();
            var first=a.get(10,TimeUnit.SECONDS);var second=b.get(10,TimeUnit.SECONDS);
            assertThat(first.result()).isEqualTo(second.result());assertThat(first.completedNow()).isNotEqualTo(second.completedNow());
            assertThat(users()).isEqualTo(1);
        } finally {release.countDown();pool.shutdownNow();pool.awaitTermination(5,TimeUnit.SECONDS);}
    }
    @Test void sameSubjectEmailIndexRaceReturnsCommittedWinnerAfterRollback() throws Exception {
        var racing=spy(new JdbcSignupStore(source));
        var winner=new AtomicReference<SignupService.Outcome>();
        var pool=Executors.newSingleThreadExecutor();
        try {
            doAnswer(call->{
                winner.set(pool.submit(()->service.complete(input())).get(10,TimeUnit.SECONDS));
                throw new org.springframework.dao.DataIntegrityViolationException("synthetic-index-race",
                    new java.sql.SQLException("users_email_key","23505"));
            }).when(racing).insertIfAbsent(any(),any());
            var retry=new SignupService(()->Optional.of(actor.get()),()->email.get(),racing,tx,clock);
            var recovered=retry.complete(input());
            assertThat(recovered.completedNow()).isFalse();
            assertThat(recovered.result()).isEqualTo(winner.get().result());
            assertThat(users()).isEqualTo(1);
            assertThat(store.jdbc.queryForObject("select count(*) from discushion.user_agreements where user_id=?",
                Integer.class,recovered.result().member().id())).isEqualTo(3);
        } finally {pool.shutdownNow();pool.awaitTermination(5,TimeUnit.SECONDS);}
    }
    @Test void simultaneousDifferentSubjectsSharingEmailDoNotMergeAccounts() throws Exception {
        var pool=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);
        try {
            java.util.function.Function<String,String> attempt=sub->{
                try {start.await();
                    var instance=new SignupService(()->Optional.of(new VerifiedActor(sub,Optional.empty())),
                        ()->Optional.of(new VerifiedEmail(sub,address,NOW.minusSeconds(60))),store,tx,clock);
                    instance.complete(input());return "OK";
                } catch(SignupFailure e){return e.reason.name();}catch(Exception e){throw new IllegalStateException(e);}
            };
            var a=pool.submit(()->attempt.apply(subject));var b=pool.submit(()->attempt.apply(subject+"-other"));start.countDown();
            assertThat(List.of(a.get(10,TimeUnit.SECONDS),b.get(10,TimeUnit.SECONDS))).containsExactlyInAnyOrder("OK","EMAIL_ALREADY_IN_USE");
            assertThat(users()).isEqualTo(1);
        } finally {pool.shutdownNow();pool.awaitTermination(5,TimeUnit.SECONDS);}
    }
}
