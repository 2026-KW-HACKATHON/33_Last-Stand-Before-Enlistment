package com.discushion.reactions;

import com.discushion.contracts.post.*;
import com.discushion.contracts.share.SharedPostAccess;
import com.discushion.identity.*;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.*;
import com.zaxxer.hikari.*;
import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import javax.sql.DataSource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.*;
import org.springframework.transaction.support.*;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;

/** Production reaction controller/store/filter under local runtime LOGIN; only the post source is test-only. */
@EnabledIfEnvironmentVariable(named="DISCUSHION_TEST_JDBC_URL",matches="jdbc:postgresql://127\\.0\\.0\\.1:55432/discushion_migration_test(?:\\?.*)?")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
    "spring.profiles.active=reaction23-http-test","spring.config.import=","PRIVY_APP_ID=synthetic-reaction23-app"})
@Import(ReactionHttpIntegrationTests.Wiring.class)
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class ReactionHttpIntegrationTests {
    static final Instant NOW=Instant.parse("2026-10-08T00:00:00Z");
    static final AtomicReference<Instant> TIME=new AtomicReference<>(NOW);
    static volatile CountDownLatch LOCK_ENTERED;
    static final ECKey KEY=key();
    static HikariDataSource runtime; static boolean activated;
    static DriverManagerDataSource adminSource() {return new DriverManagerDataSource(System.getenv("DISCUSHION_TEST_JDBC_URL"),"postgres",System.getenv("DISCUSHION_TEST_DB_PASSWORD"));}
    static JdbcTemplate admin() {return new JdbcTemplate(adminSource());}
    static String shareKey() {byte[] key=new byte[32];new java.security.SecureRandom().nextBytes(key);return Base64.getEncoder().encodeToString(key);}
    static final String SHARE_KEY=shareKey();
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("SHARE_TOKEN_SIGNING_KEY",()->SHARE_KEY);
        registry.add("PUBLIC_WEB_BASE_URL",()->"https://frontend.example.invalid");
    }
    @TestConfiguration(proxyBeanMethods=false) static class Wiring {
        @Bean @Primary Clock shareFixtureClock() {return new Clock(){
            @Override public ZoneId getZone(){return ZoneOffset.UTC;}
            @Override public Clock withZone(ZoneId zone){return Clock.fixed(instant(),zone);}
            @Override public Instant instant(){return TIME.get();}
        };}
        @Bean VerificationKeySource shareFixtureKeys() throws Exception {return new PemVerificationKeySource("-----BEGIN PUBLIC KEY-----\n"+Base64.getEncoder().encodeToString(KEY.toECPublicKey().getEncoded())+"\n-----END PUBLIC KEY-----");}
        @Bean DataSource shareRuntimeSource() {
            assertThat(admin().queryForObject("select inet_server_addr()='127.0.0.1'::inet and current_database()='discushion_migration_test'",Boolean.class)).isTrue();
            assertThat(admin().queryForObject("select not rolcanlogin from pg_roles where rolname='discushion_server'",Boolean.class)).isTrue();
            String password=UUID.randomUUID().toString().replace("-","");
            admin().execute("alter role discushion_server login password '"+password+"'");activated=true;
            try {var config=new HikariConfig();config.setJdbcUrl(System.getenv("DISCUSHION_TEST_JDBC_URL"));config.setUsername("discushion_server");config.setPassword(password);config.setMaximumPoolSize(4);runtime=new HikariDataSource(config);return runtime;}
            catch(RuntimeException failure){admin().execute("alter role discushion_server nologin password null");activated=false;throw failure;}
        }
        @Bean PostContextReader shareJdbcPostFixture(DataSource source) {return new PostContextReader(){
            private Optional<PostContext> read(long id,boolean lock) {
                if(lock && LOCK_ENTERED!=null) LOCK_ENTERED.countDown();
                return new JdbcTemplate(source).query("select id,type,region_id,author_user_id,status from discushion.posts where id=?"+(lock?" for update":""),
                    (rs,row)->new PostContext(rs.getLong(1),PostType.valueOf(rs.getString(2)),rs.getLong(3),rs.getLong(4),PostStatus.valueOf(rs.getString(5)),Optional.empty()),id).stream().findFirst();
            }
            @Override public Optional<PostContext> find(long id){return read(id,false);}
            @Override public Optional<PostContext> findForUpdate(long id){return read(id,true);}
        };}
    }
    @AfterAll static void restoreRole(){try{if(runtime!=null)runtime.close();}finally{if(activated){admin().execute("alter role discushion_server nologin password null");activated=false;}}}
    @LocalServerPort int port; @Autowired DataSource source;
    String marker,subject;long user,region,post,other,secondUser;

    @BeforeEach void setup() {
        TIME.set(NOW);LOCK_ENTERED=null;marker="synthetic-reaction23-"+UUID.randomUUID();subject="did:privy:"+marker;
        region=admin().queryForObject("insert into discushion.regions(name) values(?) returning id",Long.class,marker);
        user=user(subject,"a");secondUser=user(subject+"-second","b");post=post();other=post();
    }
    long user(String sub,String suffix) {
        long id=admin().queryForObject("insert into discushion.users(email,email_verified_at,created_at,updated_at,privy_user_id,registration_completed_at) values(?,?::timestamptz,?::timestamptz,?::timestamptz,?,?::timestamptz) returning id",
            Long.class,marker+suffix+"@example.invalid",NOW.toString(),NOW.toString(),NOW.toString(),sub,NOW.toString());
        admin().update("insert into discushion.neighbor_verified_regions(user_id,region_id,verified_at) values(?,?,?::timestamptz)",id,region,NOW.toString());return id;
    }
    long post() {return admin().queryForObject("insert into discushion.posts(author_user_id,region_id,type,topic,title,content,status,content_revision,created_at,updated_at) values(?,?,'LOCAL_AGENDA','OTHER',?,'synthetic text','PUBLISHED',1,?::timestamptz,?::timestamptz) returning id",Long.class,user,region,marker,NOW.toString(),NOW.toString());}
    @AfterEach void cleanup() {
        LOCK_ENTERED=null;admin().update("delete from discushion.post_reactions where post_id in(?,?)",post,other);
        admin().update("delete from discushion.posts where id in(?,?)",post,other);
        admin().update("delete from discushion.neighbor_verified_regions where user_id in(?,?)",user,secondUser);
        admin().update("delete from discushion.users where id in(?,?)",user,secondUser);admin().update("delete from discushion.regions where id=?",region);
    }
    String token(String sub) throws Exception {
        var jwt=new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.ES256).type(JOSEObjectType.JWT).build(),new JWTClaimsSet.Builder().issuer("privy.io").audience("synthetic-reaction23-app").subject(sub).issueTime(Date.from(NOW.minusSeconds(60))).expirationTime(Date.from(NOW.plusSeconds(3600))).claim("sid","synthetic-reaction-session").build());
        jwt.sign(new ECDSASigner(KEY));return jwt.serialize();
    }
    HttpResponse<String> request(String method,long id,String type,String token,String body,String share) throws Exception {
        var builder=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/v1/posts/"+id+"/reactions/"+type)).timeout(Duration.ofSeconds(15));
        if(token!=null)builder.header("Authorization","Bearer "+token);if(share!=null)builder.header("X-Post-Share-Token",share);
        if(body!=null)builder.header("Content-Type","application/json");
        return HttpClient.newHttpClient().send(builder.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
    }
    HttpResponse<String> set(String method,String type,String sub) throws Exception {return request(method,post,type,sub==null?null:token(sub),null,null);}
    tools.jackson.databind.JsonNode data(HttpResponse<String> response) throws Exception {
        assertThat(response.statusCode()).isEqualTo(200);return JsonMapper.builder().build().readTree(response.body()).get("data");
    }
    @Test void allThreeSelectionsAreIndependentAndRepeatedPutPreservesOneOriginalRelation() throws Exception {
        for(String type:List.of("EMPATHY","NEEDED","CURIOUS"))data(set("PUT",type,subject));
        TIME.set(NOW.plusSeconds(1));var state=data(set("PUT","EMPATHY",subject));
        assertThat(state.get("reactionCounts").get("total").asLong()).isEqualTo(3);assertThat(state.get("myReactions").size()).isEqualTo(3);
        assertThat(admin().queryForObject("select count(*) from discushion.post_reactions where post_id=? and user_id=?",Integer.class,post,user)).isEqualTo(3);
        assertThat(admin().queryForObject("select created_at from discushion.post_reactions where post_id=? and user_id=? and reaction_type='EMPATHY'",java.sql.Timestamp.class,post,user).toInstant()).isEqualTo(NOW);
        assertThat(admin().queryForObject("select count(*) from discushion.activity_events where user_id=?",Integer.class,user)).isZero();
    }
    @Test void repeatedDeleteAndReRegistrationDoNotChangeOtherUsersOrOtherTypes() throws Exception {
        data(set("PUT","EMPATHY",subject));data(set("PUT","NEEDED",subject));data(set("PUT","EMPATHY",subject+"-second"));
        for(int repeat=0;repeat<2;repeat++) {
            var state=data(set("DELETE","EMPATHY",subject));assertThat(state.get("reactionCounts").get("EMPATHY").asLong()).isEqualTo(1);
            assertThat(state.get("reactionCounts").get("total").asLong()).isEqualTo(2);assertThat(state.get("myReactions").toString()).isEqualTo("[\"NEEDED\"]");
        }
        var otherState=data(set("PUT","EMPATHY",subject+"-second"));assertThat(otherState.get("myReactions").toString()).isEqualTo("[\"EMPATHY\"]");
        assertThat(data(set("PUT","EMPATHY",subject)).get("reactionCounts").get("total").asLong()).isEqualTo(3);
        assertThat(data(request("DELETE",other,"CURIOUS",token(subject),null,null)).get("reactionCounts").get("total").asLong()).isZero();
    }
    @Test void missingIncompleteUnqualifiedAndInvalidMembersCannotUseGuestHeadersToReact() throws Exception {
        assertThat(request("PUT",post,"EMPATHY",null,null,"synthetic-share").statusCode()).isEqualTo(401);
        assertThat(request("PUT",post,"EMPATHY","invalid",null,"synthetic-share").statusCode()).isEqualTo(401);
        assertThat(set("PUT","EMPATHY",subject+"-missing").statusCode()).isEqualTo(403);
        admin().update("delete from discushion.neighbor_verified_regions where user_id=?",user);
        assertThat(request("PUT",post,"EMPATHY",token(subject),null,"synthetic-share").statusCode()).isEqualTo(403);
        assertThat(set("DELETE","EMPATHY",subject).statusCode()).isEqualTo(403);
        admin().update("update discushion.users set registration_completed_at=null where id=?",user);
        assertThat(set("PUT","EMPATHY",subject).statusCode()).isEqualTo(403);
        assertThat(admin().queryForObject("select count(*) from discushion.post_reactions where post_id=?",Integer.class,post)).isZero();
    }
    @Test void invalidBodiesAndTypesNeverMutateAndDeletedTargetsRejectBothTransitions() throws Exception {
        assertThat(set("PUT","LIKE",subject).statusCode()).isEqualTo(400);
        assertThat(request("PUT",post,"EMPATHY",token(subject),"{\"userId\":"+secondUser+"}",null).statusCode()).isEqualTo(400);
        data(set("PUT","EMPATHY",subject));admin().update("update discushion.posts set status='DELETED',deleted_at=?::timestamptz where id=?",NOW.toString(),post);
        for(String method:List.of("PUT","DELETE"))assertThat(set(method,"EMPATHY",subject).statusCode()).isEqualTo(404);
        assertThat(admin().queryForObject("select count(*) from discushion.post_reactions where post_id=?",Integer.class,post)).isEqualTo(1);
    }
    @Test void concurrentSameAndDifferentUserRequestsProduceExactRelationsAndCounts() throws Exception {
        String first=token(subject),second=token(subject+"-second");var executor=Executors.newFixedThreadPool(4);
        try {
            var jobs=new ArrayList<Future<HttpResponse<String>>>();
            for(int i=0;i<12;i++){String bearer=i%2==0?first:second;jobs.add(executor.submit(()->request("PUT",post,"EMPATHY",bearer,null,null)));}
            for(var job:jobs)assertThat(job.get(15,TimeUnit.SECONDS).statusCode()).isEqualTo(200);
            var state=data(set("PUT","EMPATHY",subject));assertThat(state.get("reactionCounts").get("EMPATHY").asLong()).isEqualTo(2);
            assertThat(admin().queryForObject("select count(*) from discushion.post_reactions where post_id=?",Integer.class,post)).isEqualTo(2);
        } finally {executor.shutdownNow();executor.awaitTermination(5,TimeUnit.SECONDS);}
    }
    @Test void deletionDuringActualPostLockWaitPreventsReactionStorage() throws Exception {
        var executor=Executors.newSingleThreadExecutor();LOCK_ENTERED=new CountDownLatch(1);String bearer=token(subject);
        try(var connection=adminSource().getConnection()) {
            connection.setAutoCommit(false);try(var stmt=connection.prepareStatement("select id from discushion.posts where id=? for update")){stmt.setLong(1,post);stmt.executeQuery().close();}
            var future=executor.submit(()->request("PUT",post,"EMPATHY",bearer,null,null));assertThat(LOCK_ENTERED.await(5,TimeUnit.SECONDS)).isTrue();
            try(var stmt=connection.prepareStatement("update discushion.posts set status='DELETED',deleted_at=?::timestamptz where id=?")){stmt.setString(1,NOW.toString());stmt.setLong(2,post);stmt.executeUpdate();}connection.commit();
            assertThat(future.get(10,TimeUnit.SECONDS).statusCode()).isEqualTo(404);
            assertThat(admin().queryForObject("select count(*) from discushion.post_reactions where post_id=?",Integer.class,post)).isZero();
        } finally {executor.shutdownNow();executor.awaitTermination(5,TimeUnit.SECONDS);}
    }
    @Test void runtimePermissionsAllowOnlyReactionReadInsertDelete() throws Exception {
        data(set("PUT","EMPATHY",subject));var jdbc=new JdbcTemplate(source);
        assertThat(jdbc.queryForObject("select current_user",String.class)).isEqualTo("discushion_server");
        for(String sql:List.of("update discushion.post_reactions set reaction_type='CURIOUS'","truncate discushion.post_reactions","create table discushion.reaction23_denied(id int)","select * from discushion.comments"))
            assertThatThrownBy(()->jdbc.execute(sql)).isInstanceOf(org.springframework.dao.DataAccessException.class);
    }
    @Test void snapshotSqlFailureRollsBackTheNewReactionAndHidesDatabaseDetails() throws Exception {
        admin().execute("create function discushion.reaction23_fixture_failure() returns boolean language plpgsql as $$begin raise exception 'synthetic-reaction23-read-failure';end$$");
        try {
            admin().execute("create policy reaction23_fixture_read_failure on discushion.post_reactions as restrictive for select to discushion_server using(discushion.reaction23_fixture_failure())");
            try {
                var response=set("PUT","EMPATHY",subject);assertThat(response.statusCode()).isEqualTo(500);
                assertThat(response.body()).contains("INTERNAL_ERROR").doesNotContain("synthetic-reaction23-read-failure","jdbc:","password");
                assertThat(admin().queryForObject("select count(*) from discushion.post_reactions where post_id=?",Integer.class,post)).isZero();
            } finally {admin().execute("drop policy reaction23_fixture_read_failure on discushion.post_reactions");}
        } finally {admin().execute("drop function discushion.reaction23_fixture_failure()");}
    }
    @Test void qualificationRevocationUnderUserLockIsRecheckedBeforeReactionStorage() throws Exception {
        String bearer=token(subject);var executor=Executors.newSingleThreadExecutor();
        try(var connection=adminSource().getConnection()) {
            connection.setAutoCommit(false);int blocker;
            try(var stmt=connection.createStatement();var rs=stmt.executeQuery("select pg_backend_pid()")){rs.next();blocker=rs.getInt(1);}
            try(var stmt=connection.prepareStatement("select id from discushion.users where id=? for update")){stmt.setLong(1,user);stmt.executeQuery().close();}
            var future=executor.submit(()->request("PUT",post,"EMPATHY",bearer,null,null));
            boolean waiting=false;long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
            while(System.nanoTime()<deadline){waiting=Boolean.TRUE.equals(admin().queryForObject("select exists(select 1 from pg_stat_activity where ?=any(pg_blocking_pids(pid)))",Boolean.class,blocker));if(waiting)break;Thread.sleep(10);}
            assertThat(waiting).isTrue();try(var stmt=connection.prepareStatement("delete from discushion.neighbor_verified_regions where user_id=?")){stmt.setLong(1,user);stmt.executeUpdate();}connection.commit();
            assertThat(future.get(10,TimeUnit.SECONDS).statusCode()).isEqualTo(403);
            assertThat(admin().queryForObject("select count(*) from discushion.post_reactions where post_id=?",Integer.class,post)).isZero();
        } finally {executor.shutdownNow();executor.awaitTermination(5,TimeUnit.SECONDS);}
    }
    private static ECKey key(){try{return new ECKeyGenerator(Curve.P_256).generate();}catch(Exception failure){throw new ExceptionInInitializerError(failure);}}
}
