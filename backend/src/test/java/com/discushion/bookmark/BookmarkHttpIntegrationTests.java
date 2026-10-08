package com.discushion.bookmark;

import com.discushion.contracts.post.*;
import com.discushion.identity.VerificationKeySource;
import com.discushion.identity.PemVerificationKeySource;
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
import javax.sql.DataSource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;

/** Actual HTTP/identity/bookmark adapter with test-only post readers and local server role. */
@EnabledIfEnvironmentVariable(named="DISCUSHION_TEST_JDBC_URL",matches="jdbc:postgresql://127\\.0\\.0\\.1:55432/discushion_migration_test(?:\\?.*)?")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
    "spring.profiles.active=bookmark26-http-test","spring.config.import=","PRIVY_APP_ID=synthetic-bookmark26-app"})
@Import(BookmarkHttpIntegrationTests.Wiring.class)
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class BookmarkHttpIntegrationTests {
    static final Instant NOW=Instant.parse("2026-10-08T00:00:00Z");
    static final ECKey KEY=key();
    static HikariDataSource runtime;static boolean activated;
    static ECKey key(){try{return new ECKeyGenerator(Curve.P_256).generate();}catch(Exception failure){throw new ExceptionInInitializerError(failure);}}
    static DriverManagerDataSource adminSource(){return new DriverManagerDataSource(System.getenv("DISCUSHION_TEST_JDBC_URL"),"postgres",System.getenv("DISCUSHION_TEST_DB_PASSWORD"));}
    static JdbcTemplate admin(){return new JdbcTemplate(adminSource());}
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry){registry.add("SHARE_TOKEN_SIGNING_KEY",()->Base64.getEncoder().encodeToString(new byte[32]));}

    @TestConfiguration(proxyBeanMethods=false) static class Wiring {
        @Bean @Primary Clock bookmarkFixtureClock(){return Clock.fixed(NOW,ZoneOffset.UTC);}
        @Bean DataSource bookmarkRuntimeSource(){
            assertThat(admin().queryForObject("select inet_server_addr()='127.0.0.1'::inet and current_database()='discushion_migration_test'",Boolean.class)).isTrue();
            assertThat(admin().queryForObject("select not rolcanlogin from pg_roles where rolname='discushion_server'",Boolean.class)).isTrue();
            String password=UUID.randomUUID().toString().replace("-","");admin().execute("alter role discushion_server login password '"+password+"'");activated=true;
            try{var config=new HikariConfig();config.setJdbcUrl(System.getenv("DISCUSHION_TEST_JDBC_URL"));config.setUsername("discushion_server");config.setPassword(password);config.setMaximumPoolSize(4);runtime=new HikariDataSource(config);return runtime;}
            catch(RuntimeException failure){admin().execute("alter role discushion_server nologin password null");activated=false;throw failure;}
        }
        @Bean VerificationKeySource bookmarkFixtureKeys()throws Exception{return new PemVerificationKeySource("-----BEGIN PUBLIC KEY-----\n"+Base64.getEncoder().encodeToString(KEY.toECPublicKey().getEncoded())+"\n-----END PUBLIC KEY-----");}
        @Bean PostContextReader bookmarkPostFixture(DataSource source){return new PostContextReader(){
            private Optional<PostContext> read(long id,boolean lock){
                return new JdbcTemplate(source).query("select id,type,region_id,author_user_id,status from discushion.posts where id=?"+(lock?" for update":""),
                    (rs,row)->new PostContext(rs.getLong(1),PostType.valueOf(rs.getString(2)),rs.getLong(3),rs.getLong(4),PostStatus.valueOf(rs.getString(5)),Optional.empty()),id).stream().findFirst();
            }
            @Override public Optional<PostContext> find(long id){return read(id,false);}
            @Override public Optional<PostContext> findForUpdate(long id){return read(id,true);}
        };}
        @Bean PostSummaryReader bookmarkSummaryFixture(DataSource source){return ids->{
            if(ids.isEmpty())return Map.of();
            String marks=String.join(",",Collections.nCopies(ids.size(),"?"));
            var rows=new JdbcTemplate(source).query("select id,type,region_id,author_user_id,status,created_at,title,topic from discushion.posts where id in("+marks+")",
                (rs,row)->{var status=PostStatus.valueOf(rs.getString("status"));return new PostSummary(rs.getLong("id"),PostType.valueOf(rs.getString("type")),rs.getLong("region_id"),rs.getLong("author_user_id"),status,rs.getTimestamp("created_at").toInstant(),status==PostStatus.PUBLISHED?Optional.of(new PostSummary.Display(rs.getString("title"),rs.getString("topic"))):Optional.empty());},ids.toArray());
            var result=new HashMap<Long,PostSummary>();rows.forEach(value->result.put(value.postId(),value));return Map.copyOf(result);
        };}
    }
    @AfterAll static void restoreRole(){try{if(runtime!=null)runtime.close();}finally{if(activated){admin().execute("alter role discushion_server nologin password null");activated=false;}}}

    @LocalServerPort int port;
    String marker,subject,secondSubject;long user,secondUser,region,agenda,vote,other;
    @BeforeEach void setup(){
        marker="synthetic-bookmark26-"+UUID.randomUUID();subject="did:privy:"+marker;secondSubject=subject+"-second";
        region=admin().queryForObject("insert into discushion.regions(name) values(?) returning id",Long.class,marker);
        user=user(subject,"a");secondUser=user(secondSubject,"b");agenda=post(user,"LOCAL_AGENDA","SAFETY","agenda");vote=post(user,"VOTE","OTHER","vote");other=post(secondUser,"LOCAL_ACTIVITY","SAFETY","other");
    }
    long user(String sub,String suffix){return admin().queryForObject("""
        insert into discushion.users(email,password_hash,email_verified_at,created_at,updated_at,privy_user_id,registration_completed_at)
        values(?,'synthetic',?::timestamptz,?::timestamptz,?::timestamptz,?,?::timestamptz) returning id
        """,Long.class,marker+suffix+"@example.invalid",NOW.toString(),NOW.toString(),NOW.toString(),sub,NOW.toString());}
    long post(long owner,String type,String topic,String title){return admin().queryForObject("""
        insert into discushion.posts(author_user_id,region_id,type,topic,title,content,status,content_revision,created_at,updated_at)
        values(?,?,?, ?,?,'synthetic','PUBLISHED',1,?::timestamptz,?::timestamptz) returning id
        """,Long.class,owner,region,type,topic,title,NOW.toString(),NOW.toString());}
    @AfterEach void cleanup(){
        admin().update("delete from discushion.bookmarks where post_id in(?,?,?)",agenda,vote,other);
        admin().update("delete from discushion.posts where id in(?,?,?)",agenda,vote,other);
        admin().update("delete from discushion.users where id in(?,?)",user,secondUser);
        admin().update("delete from discushion.regions where id=?",region);
    }
    String token(String sub)throws Exception{
        var jwt=new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.ES256).type(JOSEObjectType.JWT).build(),new JWTClaimsSet.Builder().issuer("privy.io").audience("synthetic-bookmark26-app").subject(sub).issueTime(Date.from(NOW.minusSeconds(60))).expirationTime(Date.from(NOW.plusSeconds(3600))).claim("sid","synthetic-bookmark-session").build());
        jwt.sign(new ECDSASigner(KEY));return jwt.serialize();
    }
    HttpResponse<String> request(String method,String path,String bearer,String body)throws Exception{
        var builder=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).timeout(Duration.ofSeconds(15));
        if(bearer!=null)builder.header("Authorization","Bearer "+bearer);if(body!=null)builder.header("Content-Type","application/json");
        return HttpClient.newHttpClient().send(builder.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
    }
    HttpResponse<String> set(String method,long id,String bearer,String body)throws Exception{return request(method,"/api/v1/posts/"+id+"/bookmark",bearer,body);}
    tools.jackson.databind.JsonNode data(HttpResponse<String> response)throws Exception{assertThat(response.statusCode()).isEqualTo(200);return JsonMapper.builder().build().readTree(response.body()).get("data");}

    @Test void repeatedPutDeleteAreIdempotentForACompletedMemberWithoutNeighborVerification()throws Exception{
        var bearer=token(subject);data(set("PUT",agenda,bearer,null));data(set("PUT",agenda,bearer,null));
        assertThat(admin().queryForObject("select created_at from discushion.bookmarks where post_id=? and user_id=?",java.sql.Timestamp.class,agenda,user).toInstant()).isEqualTo(NOW);
        assertThat(data(set("DELETE",agenda,bearer,null)).get("isBookmarked").asBoolean()).isFalse();
        assertThat(data(set("DELETE",agenda,bearer,null)).get("isBookmarked").asBoolean()).isFalse();
        data(set("PUT",agenda,bearer,null));
        assertThat(admin().queryForObject("select count(*) from discushion.bookmarks where post_id=? and user_id=?",Integer.class,agenda,user)).isEqualTo(1);
    }
    @Test void listUsesOnlyCallersCurrentBookmarksAndFiltersTheOriginalSummaries()throws Exception{
        data(set("PUT",agenda,token(subject),null));data(set("PUT",vote,token(subject),null));data(set("PUT",other,token(secondSubject),null));
        var response=request("GET","/api/v1/users/me/bookmarks?type=VOTE&topic=OTHER",token(subject),null);
        var page=JsonMapper.builder().build().readTree(response.body());assertThat(response.statusCode()).isEqualTo(200);
        assertThat(page.get("data")).hasSize(1);assertThat(page.get("data").get(0).get("postId").asLong()).isEqualTo(vote);
        assertThat(page.get("data").get(0).get("title").asText()).isEqualTo("vote");
    }
    @Test void deletedPostsAreHiddenAndCannotBeChangedAndGuestCannotBookmark()throws Exception{
        data(set("PUT",agenda,token(subject),null));
        admin().update("update discushion.posts set status='DELETED',deleted_at=?::timestamptz where id=?",NOW.toString(),agenda);
        var page=request("GET","/api/v1/users/me/bookmarks",token(subject),null);
        assertThat(JsonMapper.builder().build().readTree(page.body()).get("data")).isEmpty();
        assertThat(set("DELETE",agenda,token(subject),null).statusCode()).isEqualTo(404);
        assertThat(set("PUT",agenda,null,null).statusCode()).isEqualTo(401);
        assertThat(admin().queryForObject("select count(*) from discushion.bookmarks where post_id=?",Integer.class,agenda)).isEqualTo(1);
    }
    @Test void malformedInputsAndCursorWithDifferentFiltersFailWithoutWrites()throws Exception{
        var bearer=token(subject);
        assertThat(set("PUT",agenda,bearer,"{\"userId\":99}").statusCode()).isEqualTo(400);
        assertThat(request("GET","/api/v1/users/me/bookmarks?userId=99",bearer,null).statusCode()).isEqualTo(400);
        assertThat(set("PUT",0,bearer,null).statusCode()).isEqualTo(400);
        assertThat(admin().queryForObject("select count(*) from discushion.bookmarks",Integer.class)).isZero();
    }
}
