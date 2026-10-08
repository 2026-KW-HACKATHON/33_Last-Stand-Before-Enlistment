package com.discushion.comments;

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

/** Real production comment HTTP/store/filter/share guard under local runtime LOGIN; only the post source is test-only. */
@EnabledIfEnvironmentVariable(named="DISCUSHION_TEST_JDBC_URL",matches="jdbc:postgresql://127\\.0\\.0\\.1:55432/discushion_migration_test(?:\\?.*)?")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
    "spring.profiles.active=comment22-http-test","spring.config.import=","PRIVY_APP_ID=synthetic-comment22-app"})
@Import(CommentHttpIntegrationTests.Wiring.class)
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class CommentHttpIntegrationTests {
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
    String marker,subject;long user,region,post,other;

    @BeforeEach void setup() {
        TIME.set(NOW);LOCK_ENTERED=null;marker="synthetic-comment22-"+UUID.randomUUID();subject="did:privy:"+marker;
        region=admin().queryForObject("insert into discushion.regions(name) values(?) returning id",Long.class,marker);
        user=admin().queryForObject("insert into discushion.users(email,email_verified_at,created_at,updated_at,privy_user_id,registration_completed_at) values(?,?::timestamptz,?::timestamptz,?::timestamptz,?,?::timestamptz) returning id",
            Long.class,marker+"@example.invalid",NOW.toString(),NOW.toString(),NOW.toString(),subject,NOW.toString());
        admin().update("insert into discushion.profiles(user_id,nickname,activity_region_id,updated_at) values(?,?,?,?::timestamptz)",user,"c"+user,region,NOW.toString());
        admin().update("insert into discushion.neighbor_verified_regions(user_id,region_id,verified_at) values(?,?,?::timestamptz)",user,region,NOW.toString());
        post=post();other=post();
    }
    long post() { return admin().queryForObject("insert into discushion.posts(author_user_id,region_id,type,topic,title,content,status,content_revision,created_at,updated_at) values(?,?,'LOCAL_AGENDA','OTHER',?,'synthetic content','PUBLISHED',1,?::timestamptz,?::timestamptz) returning id",Long.class,user,region,marker,NOW.toString(),NOW.toString()); }
    @AfterEach void cleanup() {
        LOCK_ENTERED=null;
        admin().update("delete from discushion.comment_evaluations where comment_id in(select id from discushion.comments where post_id in(?,?))",post,other);
        admin().update("delete from discushion.comments where post_id in(?,?)",post,other);
        admin().update("delete from discushion.posts where id in(?,?)",post,other);
        admin().update("delete from discushion.institution_credentials where user_id=?",user);
        admin().update("delete from discushion.institutions where name=?",marker);
        admin().update("delete from discushion.neighbor_verified_regions where user_id=?",user);
        admin().update("delete from discushion.profiles where user_id=?",user);
        admin().update("delete from discushion.users where id=?",user);
        admin().update("delete from discushion.regions where id=?",region);
    }
    String memberToken(String sub) throws Exception {
        var jwt=new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.ES256).type(JOSEObjectType.JWT).build(),new JWTClaimsSet.Builder().issuer("privy.io").audience("synthetic-comment22-app").subject(sub).issueTime(Date.from(NOW.minusSeconds(60))).expirationTime(Date.from(NOW.plusSeconds(604860))).claim("sid","synthetic-comment-session").build());
        jwt.sign(new ECDSASigner(KEY));return jwt.serialize();
    }
    HttpResponse<String> request(String method,String path,String bearer,String share,String body) throws Exception {
        var builder=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).timeout(Duration.ofSeconds(15));
        if(bearer!=null)builder.header("Authorization","Bearer "+bearer);if(share!=null)builder.header("X-Post-Share-Token",share);
        if(body!=null)builder.header("Content-Type","application/json");
        return HttpClient.newHttpClient().send(builder.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
    }
    tools.jackson.databind.JsonNode json(HttpResponse<String> response) throws Exception { return JsonMapper.builder().build().readTree(response.body()); }
    String issued() throws Exception {
        var response=request("GET","/api/v1/posts/"+post+"/share-link",memberToken(subject),null,null);assertThat(response.statusCode()).isEqualTo(200);
        return URI.create(json(response).get("data").get("shareUrl").asString()).getQuery().substring(6);
    }
    HttpResponse<String> create(long source,String bearer,String share,String content) throws Exception {
        return request("POST","/api/v1/posts/"+source+"/comments",bearer,share,"{\"content\":\""+content+"\"}");
    }
    long root(String content) throws Exception {var response=create(post,memberToken(subject),null,content);assertThat(response.statusCode()).isEqualTo(201);return json(response).get("data").get("id").asLong();}
    long reply(long path,String share,String extra) throws Exception {
        var response=request("POST","/api/v1/comments/"+path+"/replies",null,share,"{\"content\":\"답글\""+extra+"}");
        assertThat(response.statusCode()).isEqualTo(201);return json(response).get("data").get("id").asLong();
    }
    @Test void actualServerLoginCreatesMemberAndGuestCommentsAndSeparatesViewerFields() throws Exception {
        String share=issued();long member=root("회원 의견");
        var response=create(post,null,share,"게스트 의견");assertThat(response.statusCode()).isEqualTo(201);
        var guest=json(response).get("data");assertThat(guest.get("author").get("displayName").asString()).isEqualTo("게스트");
        assertThat(guest.get("author").get("institutionVerified").asBoolean()).isFalse();assertThat(guest.has("myEvaluation")).isFalse();
        assertThat(new JdbcTemplate(source).queryForObject("select current_user",String.class)).isEqualTo("discushion_server");
        var listing=request("GET","/api/v1/posts/"+post+"/comments",memberToken(subject),null,null);
        assertThat(listing.statusCode()).isEqualTo(200);assertThat(json(listing).get("data").get(0).has("myEvaluation")).isTrue();
        var anonymous=request("GET","/api/v1/posts/"+post+"/comments",null,share,null);
        assertThat(anonymous.statusCode()).isEqualTo(200);assertThat(json(anonymous).get("data").get(0).has("myEvaluation")).isFalse();
        assertThat(admin().queryForObject("select count(*) from discushion.comments where post_id=?",Integer.class,post)).isEqualTo(2);
        assertThat(admin().queryForObject("select count(*) from discushion.activity_events where user_id=?",Integer.class,user)).isZero();
    }
    @Test void allWritersShareLiteralDictionaryAndUntrustedIdentityFieldsAreRejected() throws Exception {
        String share=issued();
        for(String word:CommentWords.WORDS)for(boolean guest:List.of(false,true)) {
            var response=create(post,guest?null:memberToken(subject),guest?share:null,"앞"+word+"뒤");
            assertThat(response.statusCode()).isEqualTo(400);assertThat(json(response).get("code").asString()).isEqualTo("COMMENT_FORBIDDEN_WORD");
        }
        var bad=request("POST","/api/v1/posts/"+post+"/comments",null,share,"{\"content\":\"의견\",\"userId\":1}");
        assertThat(bad.statusCode()).isEqualTo(400);
        assertThat(create(post,null,share," ").statusCode()).isEqualTo(400);
        long parent=root("정상 의견");
        assertThat(request("POST","/api/v1/comments/"+parent+"/replies",null,share,"{\"content\":\"씨발\"}").statusCode()).isEqualTo(400);
        assertThat(admin().queryForObject("select count(*) from discushion.comments where post_id=?",Integer.class,post)).isEqualTo(1);
    }
    @Test void memberMustHaveCurrentCompletedRegionAndCannotUseShareToBypassIt() throws Exception {
        String share=issued();admin().update("delete from discushion.neighbor_verified_regions where user_id=?",user);
        assertThat(create(post,memberToken(subject),share,"의견").statusCode()).isEqualTo(403);
        assertThat(request("GET","/api/v1/posts/"+post+"/comments",memberToken(subject),share,null).statusCode()).isEqualTo(200);
        assertThat(create(post,"invalid",share,"의견").statusCode()).isEqualTo(401);
        assertThat(create(post,memberToken(subject+"-missing"),share,"의견").statusCode()).isEqualTo(403);
        admin().update("update discushion.users set registration_completed_at=null where id=?",user);
        assertThat(create(post,memberToken(subject),share,"의견").statusCode()).isEqualTo(403);
        assertThat(request("GET","/api/v1/posts/"+post+"/comments",memberToken(subject),share,null).statusCode()).isEqualTo(403);
        assertThat(create(post,null,share,"게스트").statusCode()).isEqualTo(201);
    }
    @Test void repliesToRepliesStayUnderRootAndCrossPostOrThreadTargetsAreRejected() throws Exception {
        String share=issued();long parent=root("부모"),second=root("다른 부모");long child=reply(parent,share,"");long nested=reply(child,share,"");
        var saved=admin().queryForMap("select parent_comment_id,reply_to_comment_id,reply_to_display_name from discushion.comments where id=?",nested);
        assertThat(saved.get("parent_comment_id")).isEqualTo(parent);assertThat(saved.get("reply_to_comment_id")).isEqualTo(child);assertThat(saved.get("reply_to_display_name")).isEqualTo("게스트");
        assertThat(request("POST","/api/v1/comments/"+parent+"/replies",null,share,"{\"content\":\"의견\",\"replyToCommentId\":"+second+"}").statusCode()).isEqualTo(400);
        var outside=create(other,memberToken(subject),null,"외부");long outsideId=json(outside).get("data").get("id").asLong();
        assertThat(request("POST","/api/v1/comments/"+parent+"/replies",null,share,"{\"content\":\"의견\",\"replyToCommentId\":"+outsideId+"}").statusCode()).isEqualTo(400);
        assertThat(request("POST","/api/v1/comments/"+outsideId+"/replies",null,share,"{\"content\":\"의견\"}").statusCode()).isEqualTo(403);
        assertThat(request("POST","/api/v1/comments/9007199254740991/replies",memberToken(subject),null,"{\"content\":\"의견\"}").statusCode()).isEqualTo(404);
    }
    @Test void rootPaginationIgnoresReplyLikesAndReturnsAllRepliesInOldestOrder() throws Exception {
        String share=issued();long older=root("첫 부모"),newer=root("새 부모");long child1=reply(older,share,""),child2=reply(child1,share,"");
        for(long id:List.of(older,child1))admin().update("insert into discushion.comment_evaluations values(?,?,'LIKE',?::timestamptz,?::timestamptz)",id,user,NOW.toString(),NOW.toString());
        var response=request("GET","/api/v1/posts/"+post+"/comments?size=1",null,share,null);assertThat(response.statusCode()).isEqualTo(200);
        var data=json(response).get("data").get(0);assertThat(data.get("id").asLong()).isEqualTo(older);assertThat(data.get("likeCount").asLong()).isEqualTo(1);
        assertThat(data.get("replies").size()).isEqualTo(2);assertThat(data.get("replies").get(0).get("id").asLong()).isEqualTo(child1);assertThat(data.get("replies").get(1).get("id").asLong()).isEqualTo(child2);
        String cursor=json(response).get("meta").get("nextCursor").asString();
        var next=request("GET","/api/v1/posts/"+post+"/comments?size=1&cursor="+cursor,null,share,null);
        assertThat(json(next).get("data").get(0).get("id").asLong()).isEqualTo(newer);assertThat(json(next).get("meta").get("hasNext").asBoolean()).isFalse();
        assertThat(request("GET","/api/v1/posts/"+post+"/comments?sort=LATEST&cursor="+cursor,null,share,null).statusCode()).isEqualTo(400);
        String forged=CommentQuery.encode(post,CommentQuery.Sort.LIKES,new CommentQuery.Position(1,Instant.MAX,older));
        assertThat(request("GET","/api/v1/posts/"+post+"/comments?cursor="+forged,null,share,null).statusCode()).isEqualTo(400);
        var latest=request("GET","/api/v1/posts/"+post+"/comments?sort=LATEST&size=1",null,share,null);
        assertThat(json(latest).get("data").get(0).get("id").asLong()).isEqualTo(newer);
    }
    @Test void unavailableAndWrongShareSourcesNeverExposeOrCreateComments() throws Exception {
        String share=issued();assertThat(create(other,null,share,"의견").statusCode()).isEqualTo(403);
        assertThat(create(post,null,null,"의견").statusCode()).isEqualTo(401);
        TIME.set(NOW.plusSeconds(604800));assertThat(create(post,null,share,"의견").statusCode()).isEqualTo(401);
        TIME.set(NOW);admin().update("update discushion.posts set status='DELETED',deleted_at=?::timestamptz where id=?",NOW.toString(),post);
        for(String token:Arrays.asList(null,memberToken(subject)))assertThat(request("GET","/api/v1/posts/"+post+"/comments",token,share,null).statusCode()).isEqualTo(404);
        assertThat(create(post,null,share,"의견").statusCode()).isEqualTo(404);
    }
    @Test void serverCanGenerateIdentityButCannotModifyOrDeleteComments() throws Exception {
        long id=root("권한 확인");var jdbc=new JdbcTemplate(source);
        assertThat(jdbc.queryForObject("select has_sequence_privilege(current_user,pg_get_serial_sequence('discushion.comments','id'),'USAGE,SELECT,UPDATE')",Boolean.class)).isFalse();
        assertThat(jdbc.queryForObject("select has_table_privilege(current_user,'discushion.comment_evaluations','INSERT')",Boolean.class)).isTrue();
        for(String sql:List.of("update discushion.comments set content='changed' where id="+id,"delete from discushion.comments where id="+id,
            "truncate discushion.comments","create table discushion.comment22_forbidden(id int)"))
            assertThatThrownBy(()->jdbc.execute(sql)).isInstanceOf(org.springframework.dao.DataAccessException.class);
    }
    @Test void localMockPublicRoleNamesReceiveNoCommentAccess() {
        var fixtureSource=adminSource();var fixture=new JdbcTemplate(fixtureSource);
        new TransactionTemplate(new DataSourceTransactionManager(fixtureSource)).execute(status->{
            // Isolated localhost-only role-name mocks, rolled back; not actual Supabase role validation.
            for(String role:List.of("anon","authenticated","service_role")) {
                fixture.execute("do $$begin if not exists(select 1 from pg_roles where rolname='"+role+"') then create role "+role+" nologin; end if; end$$");
                assertThat(fixture.queryForObject("select has_table_privilege(?,'discushion.comments','SELECT,INSERT')",Boolean.class,role)).isFalse();
                assertThat(fixture.queryForObject("select has_table_privilege(?,'discushion.comment_evaluations','SELECT,INSERT')",Boolean.class,role)).isFalse();
            }
            status.setRollbackOnly();return null;
        });
    }
    @Test void deletionWhileGuestWaitsForRealPostLockRollsBackCommentCreation() throws Exception {
        String share=issued();var executor=Executors.newSingleThreadExecutor();LOCK_ENTERED=new CountDownLatch(1);
        try(var connection=adminSource().getConnection()) {
            connection.setAutoCommit(false);try(var stmt=connection.prepareStatement("select id from discushion.posts where id=? for update")){stmt.setLong(1,post);stmt.executeQuery().close();}
            var future=executor.submit(()->create(post,null,share,"경합 의견"));assertThat(LOCK_ENTERED.await(5,TimeUnit.SECONDS)).isTrue();
            try(var stmt=connection.prepareStatement("update discushion.posts set status='DELETED',deleted_at=?::timestamptz where id=?")){stmt.setString(1,NOW.toString());stmt.setLong(2,post);stmt.executeUpdate();}connection.commit();
            assertThat(future.get(10,TimeUnit.SECONDS).statusCode()).isEqualTo(404);
            assertThat(admin().queryForObject("select count(*) from discushion.comments where post_id=?",Integer.class,post)).isZero();
        } finally {executor.shutdownNow();executor.awaitTermination(5,TimeUnit.SECONDS);}
    }
    @Test void expiryWhileGuestWaitsForPostLockNeverStoresAComment() throws Exception {
        String share=issued();var executor=Executors.newSingleThreadExecutor();LOCK_ENTERED=new CountDownLatch(1);
        try(var connection=adminSource().getConnection()) {
            connection.setAutoCommit(false);try(var stmt=connection.prepareStatement("select id from discushion.posts where id=? for update")){stmt.setLong(1,post);stmt.executeQuery().close();}
            var future=executor.submit(()->create(post,null,share,"늦은 의견"));assertThat(LOCK_ENTERED.await(5,TimeUnit.SECONDS)).isTrue();
            TIME.set(NOW.plusSeconds(604800));connection.rollback();assertThat(future.get(10,TimeUnit.SECONDS).statusCode()).isEqualTo(401);
            assertThat(admin().queryForObject("select count(*) from discushion.comments where post_id=?",Integer.class,post)).isZero();
        } finally {executor.shutdownNow();executor.awaitTermination(5,TimeUnit.SECONDS);}
    }
    @Test void finalResponseFailureRollsBackThePreviouslyInsertedComment() throws Exception {
        admin().update("delete from discushion.profiles where user_id=?",user);
        var response=create(post,memberToken(subject),null,"저장 실패");assertThat(response.statusCode()).isEqualTo(500);
        assertThat(json(response).get("code").asString()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.body()).doesNotContain("discushion.","jdbc:","password");
        assertThat(admin().queryForObject("select count(*) from discushion.comments where post_id=?",Integer.class,post)).isZero();
    }
    @Test void authorNameAndInstitutionBadgeUseCurrentPublicProfileAndValidCredentials() throws Exception {
        String share=issued();long id=root("기관 의견");
        long institution=admin().queryForObject("insert into discushion.institutions(name,created_at) values(?,?::timestamptz) returning id",Long.class,marker,NOW.toString());
        admin().update("insert into discushion.institution_credentials(user_id,institution_id,responsible_region_id,completed_at,valid_until) values(?,?,?,?::timestamptz,?::timestamptz)",
            user,institution,region,NOW.minusSeconds(60).toString(),NOW.plusSeconds(60).toString());
        admin().update("update discushion.profiles set nickname='새이름' where user_id=?",user);
        var data=json(request("GET","/api/v1/posts/"+post+"/comments",null,share,null)).get("data").get(0);
        assertThat(data.get("author").get("displayName").asString()).isEqualTo("새이름");
        assertThat(data.get("author").get("institutionVerified").asBoolean()).isTrue();
        TIME.set(NOW.plusSeconds(60));
        var expired=json(request("GET","/api/v1/posts/"+post+"/comments",null,share,null)).get("data").get(0);
        assertThat(expired.get("author").get("institutionVerified").asBoolean()).isFalse();
    }
    @Test void qualificationRevocationUnderUserLockIsSeenBeforeMemberCommentStorage() throws Exception {
        String token=memberToken(subject);var executor=Executors.newSingleThreadExecutor();
        try(var connection=adminSource().getConnection()) {
            connection.setAutoCommit(false);int blocker;
            try(var stmt=connection.createStatement();var result=stmt.executeQuery("select pg_backend_pid()")){result.next();blocker=result.getInt(1);}
            try(var stmt=connection.prepareStatement("select id from discushion.users where id=? for update")){stmt.setLong(1,user);stmt.executeQuery().close();}
            var future=executor.submit(()->create(post,token,null,"철회 후 의견"));
            long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);boolean waiting=false;
            while(System.nanoTime()<deadline) {
                waiting=Boolean.TRUE.equals(admin().queryForObject("select exists(select 1 from pg_stat_activity where ?=any(pg_blocking_pids(pid)))",Boolean.class,blocker));
                if(waiting)break;Thread.sleep(10);
            }
            assertThat(waiting).isTrue();
            try(var stmt=connection.prepareStatement("delete from discushion.neighbor_verified_regions where user_id=?")){stmt.setLong(1,user);stmt.executeUpdate();}connection.commit();
            assertThat(future.get(10,TimeUnit.SECONDS).statusCode()).isEqualTo(403);
            assertThat(admin().queryForObject("select count(*) from discushion.comments where post_id=?",Integer.class,post)).isZero();
        } finally {executor.shutdownNow();executor.awaitTermination(5,TimeUnit.SECONDS);}
    }
    private static ECKey key(){try{return new ECKeyGenerator(Curve.P_256).generate();}catch(Exception failure){throw new ExceptionInInitializerError(failure);}}
}
