package com.discushion.share;

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

/** Real local server LOGIN/filter/HMAC/JDBC locks; the post adapter and consuming endpoints are TEST-ONLY. */
@EnabledIfEnvironmentVariable(named="DISCUSHION_TEST_JDBC_URL",matches="jdbc:postgresql://127\\.0\\.0\\.1:55432/discushion_migration_test(?:\\?.*)?")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
    "spring.profiles.active=share21-http-test","spring.config.import=","PRIVY_APP_ID=synthetic-share21-app"})
@Import({ShareHttpIntegrationTests.Wiring.class,ShareHttpIntegrationTests.Probe.class})
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class ShareHttpIntegrationTests {
    static final Instant NOW=Instant.parse("2026-10-08T00:00:00Z");
    static final AtomicReference<Instant> TIME=new AtomicReference<>(NOW);
    static final AtomicInteger WRITES=new AtomicInteger();
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
    @TestConfiguration(proxyBeanMethods=false) @RestController("share21Probe") @Profile("share21-http-test")
    static class Probe {
        private final DataSource source;private final SharedPostAccess access;private final MemberAuthorization members;private final PostContextReader posts;
        Probe(DataSource source,SharedPostAccess access,MemberAuthorization members,PostContextReader posts){this.source=source;this.access=access;this.members=members;this.posts=posts;}
        @GetMapping("/api/v1/test-share/{id}") Map<String,Object> read(@PathVariable("id") long id,@RequestParam(name="action",defaultValue="DETAIL") SharedPostAccess.Action action){
            var tx=new TransactionTemplate(new DataSourceTransactionManager(source));tx.setReadOnly(true);
            return tx.execute(status->Map.of("data",Map.of("guest",access.guestForRead(id,action).isPresent(),"postId",id)));
        }
        @PostMapping("/api/v1/test-share/{id}") Map<String,Object> write(@PathVariable("id") long id){
            return new TransactionTemplate(new DataSourceTransactionManager(source)).execute(status->{
                var guest=access.guestForWrite(id,SharedPostAccess.Action.CREATE_COMMENT);
                if(guest.isEmpty()){var member=members.lockCurrentCompletedMember();var post=posts.findForUpdate(id).orElseThrow();members.requireVerifiedRegion(member,post.regionId());}
                WRITES.incrementAndGet();return Map.of("data",Map.of("guest",guest.isPresent()));
            });
        }
    }
    @AfterAll static void restoreRole(){try{if(runtime!=null)runtime.close();}finally{if(activated){admin().execute("alter role discushion_server nologin password null");activated=false;}}}
    @LocalServerPort int port; @Autowired DataSource source;
    String marker,subject;long user,region,post,other;
    @BeforeEach void setup(){
        TIME.set(NOW);LOCK_ENTERED=null;WRITES.set(0);marker="synthetic-share21-"+UUID.randomUUID();subject="did:privy:"+marker;
        region=admin().queryForObject("insert into discushion.regions(name) values(?) returning id",Long.class,marker);
        user=admin().queryForObject("insert into discushion.users(email,email_verified_at,created_at,updated_at,privy_user_id,registration_completed_at) values(?,?::timestamptz,?::timestamptz,?::timestamptz,?,?::timestamptz) returning id",
            Long.class,marker+"@example.invalid",NOW.toString(),NOW.toString(),NOW.toString(),subject,NOW.toString());
        post=post();other=post();
    }
    long post(){return admin().queryForObject("insert into discushion.posts(author_user_id,region_id,type,topic,title,content,status,content_revision,created_at,updated_at) values(?,?,'LOCAL_AGENDA','OTHER',?,'synthetic public text','PUBLISHED',1,?::timestamptz,?::timestamptz) returning id",Long.class,user,region,marker,NOW.toString(),NOW.toString());}
    @AfterEach void cleanup(){LOCK_ENTERED=null;admin().update("delete from discushion.posts where id in (?,?)",post,other);admin().update("delete from discushion.users where id=?",user);admin().update("delete from discushion.regions where id=?",region);}
    String memberToken(String sub)throws Exception{
        var jwt=new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.ES256).type(JOSEObjectType.JWT).build(),new JWTClaimsSet.Builder().issuer("privy.io").audience("synthetic-share21-app").subject(sub).issueTime(Date.from(NOW.minusSeconds(60))).expirationTime(Date.from(NOW.plusSeconds(604860))).claim("sid","synthetic-share-session").build());
        jwt.sign(new ECDSASigner(KEY));return jwt.serialize();
    }
    HttpResponse<String> request(String method,String path,String bearer,String share)throws Exception{
        var builder=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).timeout(Duration.ofSeconds(15));
        if(bearer!=null)builder.header("Authorization","Bearer "+bearer);if(share!=null)builder.header("X-Post-Share-Token",share);
        return HttpClient.newHttpClient().send(builder.method(method,HttpRequest.BodyPublishers.noBody()).build(),HttpResponse.BodyHandlers.ofString());
    }
    String issued(long id)throws Exception{
        var response=request("GET","/api/v1/posts/"+id+"/share-link",memberToken(subject),null);assertThat(response.statusCode()).isEqualTo(200);
        var data=JsonMapper.builder().build().readTree(response.body()).get("data");
        assertThat(data.get("postId").asLong()).isEqualTo(id);String url=data.get("shareUrl").asString();
        assertThat(url).startsWith("https://frontend.example.invalid/shared/posts/"+id+"?token=");return URI.create(url).getQuery().substring(6);
    }
    @Test void actualRuntimeLoginIssuesDistinctReusablePostBoundLinksWithoutChangingSource()throws Exception{
        assertThat(new JdbcTemplate(source).queryForObject("select current_user",String.class)).isEqualTo("discushion_server");
        String first=issued(post),second=issued(post);assertThat(second).isNotEqualTo(first);
        for(String share:List.of(first,second,first)) assertThat(request("GET","/api/v1/test-share/"+post,null,share).statusCode()).isEqualTo(200);
        assertThat(admin().queryForObject("select content_revision from discushion.posts where id=?",Long.class,post)).isEqualTo(1);
        assertThat(admin().queryForObject("select count(*) from discushion.activity_events where user_id=?",Integer.class,user)).isZero();
    }
    @Test void guestCannotIssueAndMissingIncompleteOrUnregisteredMembersCannotIssue()throws Exception{
        assertThat(request("GET","/api/v1/posts/"+post+"/share-link",null,null).statusCode()).isEqualTo(401);
        assertThat(request("GET","/api/v1/posts/"+post+"/share-link",memberToken(subject+"-missing"),null).statusCode()).isEqualTo(403);
        admin().update("update discushion.users set registration_completed_at=null where id=?",user);
        assertThat(request("GET","/api/v1/posts/"+post+"/share-link",memberToken(subject),null).statusCode()).isEqualTo(403);
    }
    @Test void wrongPostMalformedExpiredAndDeletedContextsAreDenied()throws Exception{
        String share=issued(post);
        assertThat(request("GET","/api/v1/test-share/"+other,null,share).statusCode()).isEqualTo(403);
        for(String bad:Arrays.asList(null,"invalid",share+"x"))assertThat(request("GET","/api/v1/test-share/"+post,null,bad).statusCode()).isEqualTo(401);
        TIME.set(NOW.plusSeconds(604800));assertThat(request("GET","/api/v1/test-share/"+post,null,share).statusCode()).isEqualTo(401);
        TIME.set(NOW);admin().update("update discushion.posts set status='DELETED',deleted_at=?::timestamptz where id=?",NOW.toString(),post);
        assertThat(request("GET","/api/v1/test-share/"+post,null,share).statusCode()).isEqualTo(404);
        assertThat(request("GET","/api/v1/posts/"+post+"/share-link",memberToken(subject),null).statusCode()).isEqualTo(404);
    }
    @Test void shareTokenNeverGrantsMemberOnlyActionsOrBypassesMemberAuthorization()throws Exception{
        String share=issued(post);
        for(String action:List.of("HOME","LIST","MAP","PERSONAL_RECORDS","REACTION","VOTE","BOOKMARK","COMMENT_EVALUATION","ISSUE_LINK"))
            assertThat(request("GET","/api/v1/test-share/"+post+"?action="+action,null,share).statusCode()).isEqualTo(401);
        assertThat(request("POST","/api/v1/test-share/"+post,memberToken(subject),share).statusCode()).isEqualTo(403);
        assertThat(request("POST","/api/v1/test-share/"+post,"invalid",share).statusCode()).isEqualTo(401);
        assertThat(request("POST","/api/v1/test-share/"+post,null,share).statusCode()).isEqualTo(200);
        assertThat(WRITES).hasValue(1);
    }
    @Test void expiryDuringActualPostLockWaitIsRecheckedBeforeTheConsumerCanWrite()throws Exception{
        String share=issued(post);var executor=Executors.newSingleThreadExecutor();LOCK_ENTERED=new CountDownLatch(1);
        try(var connection=adminSource().getConnection()){
            connection.setAutoCommit(false);try(var statement=connection.prepareStatement("select id from discushion.posts where id=? for update")){statement.setLong(1,post);statement.executeQuery().close();}
            var future=executor.submit(()->request("POST","/api/v1/test-share/"+post,null,share));
            assertThat(LOCK_ENTERED.await(5,TimeUnit.SECONDS)).isTrue();TIME.set(NOW.plusSeconds(604800));connection.rollback();
            assertThat(future.get(10,TimeUnit.SECONDS).statusCode()).isEqualTo(401);assertThat(WRITES).hasValue(0);
        }finally{executor.shutdownNow();executor.awaitTermination(5,TimeUnit.SECONDS);}
    }
    @Test void deletionDuringActualPostLockWaitIsRecheckedBeforeTheConsumerCanWrite()throws Exception{
        String share=issued(post);var executor=Executors.newSingleThreadExecutor();LOCK_ENTERED=new CountDownLatch(1);
        try(var connection=adminSource().getConnection()){
            connection.setAutoCommit(false);try(var statement=connection.prepareStatement("select id from discushion.posts where id=? for update")){statement.setLong(1,post);statement.executeQuery().close();}
            var future=executor.submit(()->request("POST","/api/v1/test-share/"+post,null,share));assertThat(LOCK_ENTERED.await(5,TimeUnit.SECONDS)).isTrue();
            try(var update=connection.prepareStatement("update discushion.posts set status='DELETED',deleted_at=?::timestamptz where id=?")){update.setString(1,NOW.toString());update.setLong(2,post);update.executeUpdate();}connection.commit();
            assertThat(future.get(10,TimeUnit.SECONDS).statusCode()).isEqualTo(404);assertThat(WRITES).hasValue(0);
        }finally{executor.shutdownNow();executor.awaitTermination(5,TimeUnit.SECONDS);}
    }
    private static ECKey key(){try{return new ECKeyGenerator(Curve.P_256).generate();}catch(Exception failure){throw new ExceptionInInitializerError(failure);}}
}
