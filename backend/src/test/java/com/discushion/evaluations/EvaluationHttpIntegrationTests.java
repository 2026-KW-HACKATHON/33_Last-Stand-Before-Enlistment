package com.discushion.evaluations;

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
    "spring.profiles.active=evaluation24-http-test","spring.config.import=","PRIVY_APP_ID=synthetic-evaluation24-app"})
@Import(EvaluationHttpIntegrationTests.Wiring.class)
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class EvaluationHttpIntegrationTests {
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
    String marker,subject;long user,region,post,other,secondUser,root,reply,otherComment;

    @BeforeEach void setup() {
        TIME.set(NOW);LOCK_ENTERED=null;marker="synthetic-evaluation24-"+UUID.randomUUID();subject="did:privy:"+marker;
        region=admin().queryForObject("insert into discushion.regions(name) values(?) returning id",Long.class,marker);
        user=user(subject,"a");secondUser=user(subject+"-second","b");post=post();other=post();
        root=comment(post,null);reply=comment(post,root);otherComment=comment(other,null);
    }
    long user(String sub,String suffix) {
        long id=admin().queryForObject("insert into discushion.users(email,email_verified_at,created_at,updated_at,privy_user_id,registration_completed_at) values(?,?::timestamptz,?::timestamptz,?::timestamptz,?,?::timestamptz) returning id",Long.class,
            marker+suffix+"@example.invalid",NOW.toString(),NOW.toString(),NOW.toString(),sub,NOW.toString());
        admin().update("insert into discushion.profiles(user_id,nickname,activity_region_id,updated_at) values(?,?,?,?::timestamptz)",id,"e"+id,region,NOW.toString());
        admin().update("insert into discushion.neighbor_verified_regions(user_id,region_id,verified_at) values(?,?,?::timestamptz)",id,region,NOW.toString());return id;
    }
    long post() {return admin().queryForObject("insert into discushion.posts(author_user_id,region_id,type,topic,title,content,status,content_revision,created_at,updated_at) values(?,?,'LOCAL_AGENDA','OTHER',?,'synthetic content','PUBLISHED',1,?::timestamptz,?::timestamptz) returning id",Long.class,user,region,marker,NOW.toString(),NOW.toString());}
    long comment(long postId,Long parent) {return admin().queryForObject("insert into discushion.comments(post_id,parent_comment_id,author_kind,content,created_at) values(?,?,'GUEST','synthetic guest opinion',?::timestamptz) returning id",Long.class,postId,parent,NOW.toString());}
    @AfterEach void cleanup() {
        LOCK_ENTERED=null;admin().update("delete from discushion.comment_evaluations where comment_id in(select id from discushion.comments where post_id in(?,?))",post,other);
        admin().update("delete from discushion.comments where post_id in(?,?)",post,other);admin().update("delete from discushion.posts where id in(?,?)",post,other);
        admin().update("delete from discushion.neighbor_verified_regions where user_id in(?,?)",user,secondUser);
        admin().update("delete from discushion.profiles where user_id in(?,?)",user,secondUser);
        admin().update("delete from discushion.users where id in(?,?)",user,secondUser);admin().update("delete from discushion.regions where id=?",region);
    }
    String token(String sub) throws Exception {
        var jwt=new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.ES256).type(JOSEObjectType.JWT).build(),new JWTClaimsSet.Builder().issuer("privy.io").audience("synthetic-evaluation24-app").subject(sub).issueTime(Date.from(NOW.minusSeconds(60))).expirationTime(Date.from(NOW.plusSeconds(3600))).claim("sid","synthetic-evaluation-session").build());
        jwt.sign(new ECDSASigner(KEY));return jwt.serialize();
    }
    HttpResponse<String> request(String method,String path,String bearer,String body,String share) throws Exception {
        var builder=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).timeout(Duration.ofSeconds(15));
        if(bearer!=null)builder.header("Authorization","Bearer "+bearer);if(share!=null)builder.header("X-Post-Share-Token",share);
        if(body!=null)builder.header("Content-Type","application/json");
        return HttpClient.newHttpClient().send(builder.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
    }
    HttpResponse<String> set(long id,String selected,String sub) throws Exception {
        return request(selected==null?"DELETE":"PUT","/api/v1/comments/"+id+"/evaluation",sub==null?null:token(sub),selected==null?null:"{\"type\":\""+selected+"\"}",null);
    }
    tools.jackson.databind.JsonNode data(HttpResponse<String> response) throws Exception {assertThat(response.statusCode()).isEqualTo(200);return JsonMapper.builder().build().readTree(response.body()).get("data");}
    @Test void switchingAndRepeatingPreserveOneRelationAndOriginalTimestamps() throws Exception {
        assertThat(data(set(root,"LIKE",subject)).get("likeCount").asLong()).isEqualTo(1);
        TIME.set(NOW.plusSeconds(1));data(set(root,"LIKE",subject));
        var first=admin().queryForMap("select created_at,updated_at from discushion.comment_evaluations where comment_id=? and user_id=?",root,user);
        assertThat(((java.sql.Timestamp)first.get("created_at")).toInstant()).isEqualTo(NOW);assertThat(((java.sql.Timestamp)first.get("updated_at")).toInstant()).isEqualTo(NOW);
        var switched=data(set(root,"DISLIKE",subject));assertThat(switched.get("likeCount").asLong()).isZero();assertThat(switched.get("dislikeCount").asLong()).isEqualTo(1);
        assertThat(switched.get("myEvaluation").asString()).isEqualTo("DISLIKE");
        assertThat(admin().queryForObject("select count(*) from discushion.comment_evaluations where comment_id=? and user_id=?",Integer.class,root,user)).isEqualTo(1);
        assertThat(admin().queryForObject("select created_at from discushion.comment_evaluations where comment_id=? and user_id=?",java.sql.Timestamp.class,root,user).toInstant()).isEqualTo(NOW);
    }
    @Test void repeatedCancelPreservesOtherUsersAndReplyEvaluationsAndReRegistrationWorks() throws Exception {
        data(set(root,"LIKE",subject));data(set(root,"DISLIKE",subject+"-second"));data(set(reply,"LIKE",subject));
        for(int i=0;i<2;i++){var state=data(set(root,null,subject));assertThat(state.get("myEvaluation").isNull()).isTrue();assertThat(state.get("likeCount").asLong()).isZero();assertThat(state.get("dislikeCount").asLong()).isEqualTo(1);}
        assertThat(data(set(reply,"LIKE",subject)).get("myEvaluation").asString()).isEqualTo("LIKE");
        assertThat(data(set(root,"LIKE",subject)).get("likeCount").asLong()).isEqualTo(1);
        assertThat(admin().queryForObject("select count(*) from discushion.activity_events where user_id=?",Integer.class,user)).isZero();
    }
    @Test void missingIncompleteUnqualifiedAndGuestMembersCannotEvaluate() throws Exception {
        var issued=request("GET","/api/v1/posts/"+post+"/share-link",token(subject),null,null);assertThat(issued.statusCode()).isEqualTo(200);
        String shareUrl=JsonMapper.builder().build().readTree(issued.body()).get("data").get("shareUrl").asString();
        String validShare=URI.create(shareUrl).getQuery().substring(6);
        assertThat(set(root,"LIKE",null).statusCode()).isEqualTo(401);
        assertThat(request("PUT","/api/v1/comments/"+root+"/evaluation","invalid","{\"type\":\"LIKE\"}",validShare).statusCode()).isEqualTo(401);
        assertThat(request("PUT","/api/v1/comments/"+root+"/evaluation",null,"{\"type\":\"LIKE\"}",validShare).statusCode()).isEqualTo(401);
        assertThat(set(root,"LIKE",subject+"-missing").statusCode()).isEqualTo(403);
        admin().update("delete from discushion.neighbor_verified_regions where user_id=?",user);assertThat(set(root,"LIKE",subject).statusCode()).isEqualTo(403);assertThat(set(root,null,subject).statusCode()).isEqualTo(403);
        assertThat(request("PUT","/api/v1/comments/"+root+"/evaluation",token(subject),"{\"type\":\"LIKE\"}",validShare).statusCode()).isEqualTo(403);
        admin().update("update discushion.users set registration_completed_at=null where id=?",user);assertThat(set(root,"LIKE",subject).statusCode()).isEqualTo(403);
    }
    @Test void malformedBodiesUnknownTargetsAndDeletedSourcesAreDenied() throws Exception {
        for(String body:List.of("{}","{\"type\":\"NONE\"}","{\"type\":\"LIKE\",\"userId\":"+secondUser+"}","[1]","{"))
            assertThat(request("PUT","/api/v1/comments/"+root+"/evaluation",token(subject),body,null).statusCode()).isEqualTo(400);
        assertThat(request("DELETE","/api/v1/comments/"+root+"/evaluation",token(subject),"{}",null).statusCode()).isEqualTo(400);
        assertThat(set(9007199254740991L,"LIKE",subject).statusCode()).isEqualTo(404);
        data(set(root,"LIKE",subject));admin().update("update discushion.posts set status='DELETED',deleted_at=?::timestamptz where id=?",NOW.toString(),post);
        assertThat(set(root,"DISLIKE",subject).statusCode()).isEqualTo(404);assertThat(set(root,null,subject).statusCode()).isEqualTo(404);
        assertThat(admin().queryForObject("select evaluation_type from discushion.comment_evaluations where comment_id=? and user_id=?",String.class,root,user)).isEqualTo("LIKE");
    }
    @Test void repliesDoNotRaiseParentLikeSortAndCommentsExposeOnlyViewerEvaluation() throws Exception {
        long secondRoot=comment(post,null);data(set(root,"LIKE",subject));data(set(reply,"LIKE",subject));data(set(secondRoot,"DISLIKE",subject+"-second"));
        var response=request("GET","/api/v1/posts/"+post+"/comments?sort=LIKES",token(subject),null,null);assertThat(response.statusCode()).isEqualTo(200);
        var rows=JsonMapper.builder().build().readTree(response.body()).get("data");
        assertThat(rows.get(0).get("id").asLong()).isEqualTo(root);assertThat(rows.get(0).get("likeCount").asLong()).isEqualTo(1);
        assertThat(rows.get(0).get("replies").get(0).get("likeCount").asLong()).isEqualTo(1);
        assertThat(rows.get(1).get("myEvaluation").isNull()).isTrue();assertThat(rows.get(1).get("dislikeCount").asLong()).isEqualTo(1);
    }
    @Test void concurrentOppositeSelectionsKeepOnlyOneCurrentVotePerMember() throws Exception {
        String bearer=token(subject);var executor=Executors.newFixedThreadPool(4);
        try {var futures=new ArrayList<Future<HttpResponse<String>>>();for(int i=0;i<12;i++){String selected=i%2==0?"LIKE":"DISLIKE";futures.add(executor.submit(()->request("PUT","/api/v1/comments/"+root+"/evaluation",bearer,"{\"type\":\""+selected+"\"}",null)));}
            for(var future:futures)assertThat(future.get(15,TimeUnit.SECONDS).statusCode()).isEqualTo(200);
            assertThat(admin().queryForObject("select count(*) from discushion.comment_evaluations where comment_id=?",Integer.class,root)).isEqualTo(1);
            var state=data(set(root,"LIKE",subject));assertThat(state.get("likeCount").asLong()+state.get("dislikeCount").asLong()).isEqualTo(1);
        } finally {executor.shutdownNow();executor.awaitTermination(5,TimeUnit.SECONDS);}
    }
    @Test void actualRuntimeAllowsEvaluationCrudButNotCommentMutationsOrDdl() throws Exception {
        data(set(root,"LIKE",subject));var jdbc=new JdbcTemplate(source);assertThat(jdbc.queryForObject("select current_user",String.class)).isEqualTo("discushion_server");
        for(String sql:List.of("update discushion.comments set content='changed' where id="+root,"delete from discushion.comments where id="+root,"truncate discushion.comment_evaluations","create table discushion.evaluation24_denied(id int)"))
            assertThatThrownBy(()->jdbc.execute(sql)).isInstanceOf(org.springframework.dao.DataAccessException.class);
        data(set(root,"DISLIKE",subject));data(set(root,null,subject));
    }
    @Test void deletionDuringPostLockWaitPreventsEvaluationStorage() throws Exception {
        String bearer=token(subject);var executor=Executors.newSingleThreadExecutor();LOCK_ENTERED=new CountDownLatch(1);
        try(var connection=adminSource().getConnection()){
            connection.setAutoCommit(false);try(var stmt=connection.prepareStatement("select id from discushion.posts where id=? for update")){stmt.setLong(1,post);stmt.executeQuery().close();}
            var future=executor.submit(()->request("PUT","/api/v1/comments/"+root+"/evaluation",bearer,"{\"type\":\"LIKE\"}",null));assertThat(LOCK_ENTERED.await(5,TimeUnit.SECONDS)).isTrue();
            try(var stmt=connection.prepareStatement("update discushion.posts set status='DELETED',deleted_at=?::timestamptz where id=?")){stmt.setString(1,NOW.toString());stmt.setLong(2,post);stmt.executeUpdate();}connection.commit();
            assertThat(future.get(10,TimeUnit.SECONDS).statusCode()).isEqualTo(404);assertThat(admin().queryForObject("select count(*) from discushion.comment_evaluations where comment_id=?",Integer.class,root)).isZero();
        } finally {executor.shutdownNow();executor.awaitTermination(5,TimeUnit.SECONDS);}
    }
    @Test void aggregateFailureRollsBackTheNewEvaluationWithoutLeakingDatabaseDetails() throws Exception {
        admin().execute("create function discushion.evaluation24_fixture_failure() returns boolean language plpgsql as $$begin raise exception 'synthetic-evaluation24-read-failure';end$$");
        try {admin().execute("create policy evaluation24_fixture_read_failure on discushion.comment_evaluations as restrictive for select to discushion_server using(discushion.evaluation24_fixture_failure())");
            try {var response=set(root,"LIKE",subject);assertThat(response.statusCode()).isEqualTo(500);assertThat(response.body()).contains("INTERNAL_ERROR").doesNotContain("synthetic-evaluation24-read-failure","jdbc:","password");
                assertThat(admin().queryForObject("select count(*) from discushion.comment_evaluations where comment_id=?",Integer.class,root)).isZero();
            }finally{admin().execute("drop policy evaluation24_fixture_read_failure on discushion.comment_evaluations");}
        }finally{admin().execute("drop function discushion.evaluation24_fixture_failure()");}
    }
    private static ECKey key(){try{return new ECKeyGenerator(Curve.P_256).generate();}catch(Exception failure){throw new ExceptionInInitializerError(failure);}}
    @Test void qualificationRevocationUnderUserLockIsRecheckedBeforeEvaluationStorage() throws Exception {
        String bearer=token(subject);var executor=Executors.newSingleThreadExecutor();
        try(var connection=adminSource().getConnection()) {
            connection.setAutoCommit(false);int blocker;
            try(var stmt=connection.createStatement();var rs=stmt.executeQuery("select pg_backend_pid()")){rs.next();blocker=rs.getInt(1);}
            try(var stmt=connection.prepareStatement("select id from discushion.users where id=? for update")){stmt.setLong(1,user);stmt.executeQuery().close();}
            var future=executor.submit(()->request("PUT","/api/v1/comments/"+root+"/evaluation",bearer,"{\"type\":\"LIKE\"}",null));
            boolean waiting=false;long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
            while(System.nanoTime()<deadline){waiting=Boolean.TRUE.equals(admin().queryForObject("select exists(select 1 from pg_stat_activity where ?=any(pg_blocking_pids(pid)))",Boolean.class,blocker));if(waiting)break;Thread.sleep(10);}
            assertThat(waiting).isTrue();try(var stmt=connection.prepareStatement("delete from discushion.neighbor_verified_regions where user_id=?")){stmt.setLong(1,user);stmt.executeUpdate();}connection.commit();
            assertThat(future.get(10,TimeUnit.SECONDS).statusCode()).isEqualTo(403);assertThat(admin().queryForObject("select count(*) from discushion.comment_evaluations where comment_id=?",Integer.class,root)).isZero();
        }finally{executor.shutdownNow();executor.awaitTermination(5,TimeUnit.SECONDS);}
    }
}
