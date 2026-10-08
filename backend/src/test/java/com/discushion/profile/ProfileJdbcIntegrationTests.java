package com.discushion.profile;

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
import static org.mockito.Mockito.*;

@EnabledIfEnvironmentVariable(named="DISCUSHION_TEST_JDBC_URL",matches="jdbc:postgresql://127\\.0\\.0\\.1:55432/discushion_migration_test(?:\\?.*)?")
class ProfileJdbcIntegrationTests {
    private static final Instant NOW=Instant.parse("2026-10-08T00:00:00Z");
    private final Clock clock=Clock.fixed(NOW,ZoneOffset.UTC);
    private final DriverManagerDataSource source=new DriverManagerDataSource(System.getenv("DISCUSHION_TEST_JDBC_URL"),"postgres",System.getenv("DISCUSHION_TEST_DB_PASSWORD"));
    private final JdbcProfileStore store=new JdbcProfileStore(source);
    private final JdbcMemberStore members=new JdbcMemberStore(source,clock);
    private final AtomicReference<VerifiedActor> actor=new AtomicReference<>();
    private final CurrentActorProvider actors=()->Optional.ofNullable(actor.get());
    private final TransactionTemplate write=new TransactionTemplate(new DataSourceTransactionManager(source));
    private final TransactionTemplate read=new TransactionTemplate(new DataSourceTransactionManager(source));
    private ProfileService service;
    private String marker,subject; private long user,other,region,secondRegion;
    @BeforeEach void setup() {
        marker="synthetic-profile10-"+UUID.randomUUID();subject="did:privy:"+marker;
        read.setReadOnly(true);read.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        region=store.jdbc.queryForObject("insert into discushion.regions(name) values(?) returning id",Long.class,marker);
        secondRegion=store.jdbc.queryForObject("insert into discushion.regions(name) values(?) returning id",Long.class,marker+"-second");
        user=createUser(subject,"a");other=createUser(subject+"-other","b");
        actor.set(new VerifiedActor(subject,Optional.of(new LocalMember(user,Optional.of(NOW)))));
        service=service(store);
    }
    private ProfileService service(JdbcProfileStore data) {return new ProfileService(actors,new MemberAuthorization(actors,members,clock),data,read,write,clock);}
    private long createUser(String sub,String suffix) {
        long id=store.jdbc.queryForObject("""
            insert into discushion.users(email,email_verified_at,created_at,updated_at,privy_user_id,registration_completed_at)
            values(?,?::timestamptz,?::timestamptz,?::timestamptz,?,?::timestamptz) returning id
            """,Long.class,marker+suffix+"@example.invalid",NOW.toString(),NOW.toString(),NOW.toString(),sub,NOW.toString());
        store.jdbc.update("insert into discushion.profiles(user_id,nickname,bio,activity_region_id,updated_at) values(?,?,?, ?,?::timestamptz)",
            id,"p"+id,"original",region,NOW.toString());
        store.jdbc.update("insert into discushion.profile_attributes(user_id,attribute) values(?,'RESIDENT')",id);return id;
    }
    @AfterEach void cleanup() {
        for(long id:new long[]{user,other}) {
            store.jdbc.update("delete from discushion.institution_credentials where user_id=?",id);
            store.jdbc.update("delete from discushion.neighbor_verified_regions where user_id=?",id);
            store.jdbc.update("delete from discushion.profile_attributes where user_id=?",id);
            store.jdbc.update("delete from discushion.profiles where user_id=?",id);
            store.jdbc.update("delete from discushion.media_files where owner_user_id=?",id);
            store.jdbc.update("delete from discushion.users where id=?",id);
        }
        store.jdbc.update("delete from discushion.institutions where name like ?",marker+"%");
        store.jdbc.update("delete from discushion.regions where id in (?,?)",region,secondRegion);
    }
    @Test void readsOwnLatestProfileAndDefaultRegionWithoutQualifyingIt() {
        var view=service.current();assertThat(view.id()).isEqualTo(user);assertThat(view.profile().activityRegion().id()).isEqualTo(region);
        assertThat(view.neighborVerifiedRegions()).isEmpty();assertThat(view.institutionVerified()).isFalse();
        assertThat(view.institutionVerification().status()).isEqualTo("NOT_SUBMITTED");
        var before=store.jdbc.queryForMap("select updated_at,registration_completed_at from discushion.users where id=?",user);
        var result=service.patch(ProfilePatch.parse(Map.of("nickname","new"+user,"activityRegionId",secondRegion,"residentAttributes",List.of("STUDENT","WORKER"))));
        assertThat(result.profile().nickname()).isEqualTo("new"+user);assertThat(result.profile().bio()).isEqualTo("original");
        assertThat(result.profile().activityRegion().id()).isEqualTo(secondRegion);assertThat(result.neighborVerifiedRegions()).isEmpty();
        assertThat(service.current()).isEqualTo(result);
        assertThat(store.jdbc.queryForMap("select updated_at,registration_completed_at from discushion.users where id=?",user)).isEqualTo(before);
        assertThat(store.read(other,NOW).profile().nickname()).isEqualTo("p"+other);
    }
    @Test void nullableBioAndEmptyAttributesClearOnlyRequestedValues() {
        var body=new HashMap<String,Object>();body.put("bio",null);body.put("residentAttributes",List.of());
        var result=service.patch(ProfilePatch.parse(body));assertThat(result.profile().bio()).isNull();
        assertThat(result.profile().residentAttributes()).isEmpty();assertThat(result.profile().nickname()).isEqualTo("p"+user);
    }
    @Test void nicknameConflictUnknownRegionAndLateFailureRollbackEveryWrite() {
        var before=service.current();
        assertThatThrownBy(()->service.patch(ProfilePatch.parse(Map.of("nickname","p"+other,"bio","changed"))))
            .isInstanceOfSatisfying(ProfileFailure.class,e->assertThat(e.code).isEqualTo("NICKNAME_ALREADY_IN_USE"));
        assertThatThrownBy(()->service.patch(ProfilePatch.parse(Map.of("nickname","new"+user,"activityRegionId",9007199254740991L))))
            .isInstanceOfSatisfying(ProfileFailure.class,e->assertThat(e.code).isEqualTo("REGION_NOT_FOUND"));
        var failing=spy(new JdbcProfileStore(source));
        doAnswer(call->{call.callRealMethod();throw new IllegalStateException("synthetic-late-write");}).when(failing).update(anyLong(),any(),any());
        assertThatThrownBy(()->service(failing).patch(ProfilePatch.parse(Map.of("bio","changed","residentAttributes",List.of("MERCHANT")))))
            .isInstanceOf(IllegalStateException.class);
        assertThat(service.current()).isEqualTo(before);
    }
    @Test void anonymousUnknownIncompleteAndStaleCompletionCannotWrite() {
        actor.set(null);assertThatThrownBy(service::current).isInstanceOf(IdentityFailure.class);
        actor.set(new VerifiedActor(subject+"-missing",Optional.empty()));assertThatThrownBy(service::current).isInstanceOf(IdentityFailure.class);
        actor.set(new VerifiedActor(subject,Optional.of(new LocalMember(user,Optional.of(NOW)))));
        store.jdbc.update("update discushion.users set registration_completed_at=null where id=?",user);
        assertThatThrownBy(service::current).isInstanceOf(IdentityFailure.class);
        assertThatThrownBy(()->service.patch(ProfilePatch.parse(Map.of("bio","forbidden")))).isInstanceOf(IdentityFailure.class);
        assertThat(store.jdbc.queryForObject("select bio from discushion.profiles where user_id=?",String.class,user)).isEqualTo("original");
    }
    @Test void concurrentPartialUpdatesPreserveBothFields() throws Exception {
        var pool=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);
        try {
            var a=pool.submit(()->{start.await();return service.patch(ProfilePatch.parse(Map.of("bio","newbio")));});
            var b=pool.submit(()->{start.await();return service.patch(ProfilePatch.parse(Map.of("residentAttributes",List.of("MERCHANT"))));});
            start.countDown();a.get(10,TimeUnit.SECONDS);b.get(10,TimeUnit.SECONDS);
            assertThat(service.current().profile().bio()).isEqualTo("newbio");
            assertThat(service.current().profile().residentAttributes()).containsExactly("MERCHANT");
        } finally {pool.shutdownNow();pool.awaitTermination(5,TimeUnit.SECONDS);}
    }
    private long credential(String name,Instant completed,Instant until) {
        long institution=store.jdbc.queryForObject("insert into discushion.institutions(name,created_at) values(?,?::timestamptz) returning id",Long.class,marker+name,NOW.toString());
        return store.jdbc.queryForObject("""
            insert into discushion.institution_credentials(user_id,institution_id,responsible_region_id,completed_at,valid_until)
            values(?,?,?,?::timestamptz,?::timestamptz) returning id
            """,Long.class,user,institution,region,completed.toString(),until.toString());
    }
    @Test void existingProfilePhotoReferenceAndFileStateRemainUntouched() {
        long file=store.jdbc.queryForObject("""
            insert into discushion.media_files(owner_user_id,storage_key,original_name,mime_type,size_bytes,purpose,created_at)
            values(?,?,'synthetic.png','image/png',1,'PROFILE_IMAGE',?::timestamptz) returning id
            """,Long.class,user,marker+"-profile-file",NOW.toString());
        store.jdbc.update("update discushion.profiles set profile_image_file_id=? where user_id=?",file,user);
        var before=store.jdbc.queryForMap("select * from discushion.media_files where id=?",file);
        service.patch(ProfilePatch.parse(Map.of("bio","newbio")));
        assertThat(store.jdbc.queryForObject("select profile_image_file_id from discushion.profiles where user_id=?",Long.class,user)).isEqualTo(file);
        assertThat(store.jdbc.queryForMap("select * from discushion.media_files where id=?",file)).isEqualTo(before);
    }
    @Test void mostRecentActiveCredentialIsShownAndFutureCredentialDoesNotGrantBadge() {
        credential("-active-old",NOW.minusSeconds(120),NOW.plusSeconds(60));
        credential("-active-new",NOW.minusSeconds(60),NOW.plusSeconds(120));
        credential("-future",NOW.plusSeconds(60),NOW.plusSeconds(180));
        assertThat(service.current().institutionVerification().institutionName()).isEqualTo(marker+"-active-new");
        store.jdbc.update("delete from discushion.institution_credentials where user_id=? and completed_at<=?::timestamptz",user,NOW.toString());
        assertThat(service.current().institutionVerified()).isFalse();
        assertThat(service.current().institutionVerification().isActive()).isFalse();
    }
    @Test void realQualificationUsesActiveHistoryThenLatestAndExpirationBoundary() {
        store.jdbc.update("insert into discushion.neighbor_verified_regions(user_id,region_id,verified_at) values(?,?,?::timestamptz)",user,region,NOW.toString());
        long active=credential("-active",NOW.minusSeconds(120),NOW.plusSeconds(60));
        credential("-expired",NOW.minusSeconds(60),NOW);
        var view=service.current();assertThat(view.institutionVerified()).isTrue();
        assertThat(view.institutionVerification().institutionName()).isEqualTo(marker+"-active");
        service.patch(ProfilePatch.parse(Map.of("activityRegionId",secondRegion,"residentAttributes",List.of("MERCHANT"))));
        assertThat(service.current().neighborVerifiedRegions()).extracting(ProfileView.Region::id).containsExactly(region);
        store.jdbc.update("update discushion.institution_credentials set valid_until=?::timestamptz where id=?",NOW.toString(),active);
        view=service.current();assertThat(view.institutionVerified()).isFalse();
        assertThat(view.institutionVerification().status()).isEqualTo("EXPIRED");
        assertThat(view.institutionVerification().institutionName()).isEqualTo(marker+"-expired");
        assertThat(store.jdbc.queryForObject("select count(*) from discushion.institution_credentials where user_id=?",Integer.class,user)).isEqualTo(2);
    }
}
