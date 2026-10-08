package com.discushion.votes;

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
import java.util.concurrent.atomic.AtomicReference;
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
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;

/** Production vote endpoint/store under the local discushion_server LOGIN; test-only post adapter. */
@EnabledIfEnvironmentVariable(named="DISCUSHION_TEST_JDBC_URL",matches="jdbc:postgresql://127\\.0\\.0\\.1:55432/discushion_migration_test(?:\\?.*)?")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
    "spring.profiles.active=vote25-http-test","spring.config.import=","PRIVY_APP_ID=synthetic-vote25-app"})
@Import(VoteHttpIntegrationTests.Wiring.class)
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class VoteHttpIntegrationTests {
    static final Instant NOW=Instant.parse("2026-10-08T00:00:00Z");
    static final AtomicReference<Instant> TIME=new AtomicReference<>(NOW);
    static final ECKey KEY=key();
    static HikariDataSource runtime; static boolean activated;
    static DriverManagerDataSource adminSource(){return new DriverManagerDataSource(System.getenv("DISCUSHION_TEST_JDBC_URL"),"postgres",System.getenv("DISCUSHION_TEST_DB_PASSWORD"));}
    static JdbcTemplate admin(){return new JdbcTemplate(adminSource());}
    static String signingKey(){byte[] key=new byte[32];new java.security.SecureRandom().nextBytes(key);return Base64.getEncoder().encodeToString(key);}
    static final String SHARE_KEY=signingKey();
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry){registry.add("SHARE_TOKEN_SIGNING_KEY",()->SHARE_KEY);}

    @TestConfiguration(proxyBeanMethods=false) static class Wiring {
        @Bean @Primary Clock voteFixtureClock(){return new Clock(){
            @Override public ZoneId getZone(){return ZoneOffset.UTC;}
            @Override public Clock withZone(ZoneId zone){return Clock.fixed(instant(),zone);}
            @Override public Instant instant(){return TIME.get();}
        };}
        @Bean VerificationKeySource voteFixtureKeys() throws Exception{return new PemVerificationKeySource("-----BEGIN PUBLIC KEY-----\n"+Base64.getEncoder().encodeToString(KEY.toECPublicKey().getEncoded())+"\n-----END PUBLIC KEY-----");}
        @Bean DataSource voteRuntimeSource(){
            assertThat(admin().queryForObject("select inet_server_addr()='127.0.0.1'::inet and current_database()='discushion_migration_test'",Boolean.class)).isTrue();
            assertThat(admin().queryForObject("select not rolcanlogin from pg_roles where rolname='discushion_server'",Boolean.class)).isTrue();
            String password=UUID.randomUUID().toString().replace("-","");admin().execute("alter role discushion_server login password '"+password+"'");activated=true;
            try{var config=new HikariConfig();config.setJdbcUrl(System.getenv("DISCUSHION_TEST_JDBC_URL"));config.setUsername("discushion_server");config.setPassword(password);config.setMaximumPoolSize(4);runtime=new HikariDataSource(config);return runtime;}
            catch(RuntimeException failure){admin().execute("alter role discushion_server nologin password null");activated=false;throw failure;}
        }
        @Bean PostContextReader voteJdbcPostFixture(DataSource source){return new PostContextReader(){
            @Override public Optional<PostContext> find(long id){return read(id,false);}
            @Override public Optional<PostContext> findForUpdate(long id){return read(id,true);}
            private Optional<PostContext> read(long id,boolean lock){
                var jdbc=new JdbcTemplate(source);
                var rows=jdbc.query("select id,type,region_id,author_user_id,status from discushion.posts where id=?"+(lock?" for update":""),
                    (rs,row)->new Object[]{rs.getLong(1),rs.getString(2),rs.getLong(3),rs.getLong(4),rs.getString(5)},id);
                if(rows.isEmpty())return Optional.empty();var row=rows.get(0);long postId=(long)row[0];
                var polls=jdbc.query("select id,ends_at from discushion.polls where post_id=?"+(lock?" for update":""),
                    (rs,index)->new Object[]{rs.getLong(1),rs.getTimestamp(2).toInstant()},postId);
                Optional<PollContext> poll=Optional.empty();
                if(!polls.isEmpty()){
                    var p=polls.get(0);long pollId=(long)p[0];var options=jdbc.queryForList("select id from discushion.poll_options where poll_id=? order by sort_order,id",Long.class,pollId);
                    poll=Optional.of(new PollContext(pollId,(Instant)p[1],options));
                }
                return Optional.of(new PostContext(postId,PostType.valueOf((String)row[1]),(long)row[2],(long)row[3],PostStatus.valueOf((String)row[4]),poll));
            }
        };}
    }
    @AfterAll static void restoreRole(){try{if(runtime!=null)runtime.close();}finally{if(activated){admin().execute("alter role discushion_server nologin password null");activated=false;}}}
    @LocalServerPort int port; @Autowired DataSource source;
    String marker,subject,otherSubject;long user,otherUser,region,post,poll,option1,option2;

    @BeforeEach void setup(){
        TIME.set(NOW);marker="synthetic-vote25-"+UUID.randomUUID();subject="did:privy:"+marker;otherSubject=subject+"-other";
        region=admin().queryForObject("insert into discushion.regions(name) values(?) returning id",Long.class,marker);
        user=user(subject,"a");otherUser=user(otherSubject,"b");
        post=admin().queryForObject("insert into discushion.posts(author_user_id,region_id,type,topic,title,content,status,content_revision,created_at,updated_at) values(?,?,'VOTE','OTHER',?,'synthetic text','PUBLISHED',1,?::timestamptz,?::timestamptz) returning id",Long.class,user,region,marker,NOW.toString(),NOW.toString());
        poll=admin().queryForObject("insert into discushion.polls(post_id,question,ends_at) values(?,? ,?::timestamptz) returning id",Long.class,post,marker,NOW.plusSeconds(60).toString());
        option1=option("찬성",0);option2=option("반대",1);
    }
    long user(String sub,String suffix){long id=admin().queryForObject("insert into discushion.users(email,email_verified_at,created_at,updated_at,privy_user_id,registration_completed_at) values(?,?::timestamptz,?::timestamptz,?::timestamptz,?,?::timestamptz) returning id",Long.class,marker+suffix+"@example.invalid",NOW.toString(),NOW.toString(),NOW.toString(),sub,NOW.toString());admin().update("insert into discushion.neighbor_verified_regions(user_id,region_id,verified_at) values(?,?,?::timestamptz)",id,region,NOW.toString());return id;}
    long option(String content,int order){return admin().queryForObject("insert into discushion.poll_options(poll_id,content,sort_order) values(?,?,?) returning id",Long.class,poll,content,order);}
    @AfterEach void cleanup(){admin().update("delete from discushion.vote_selections where poll_id=?",poll);admin().update("delete from discushion.poll_options where poll_id=?",poll);admin().update("delete from discushion.polls where id=?",poll);admin().update("delete from discushion.posts where id=?",post);admin().update("delete from discushion.neighbor_verified_regions where user_id in(?,?)",user,otherUser);admin().update("delete from discushion.users where id in(?,?)",user,otherUser);admin().update("delete from discushion.regions where id=?",region);}
    String token(String sub)throws Exception{var jwt=new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.ES256).type(JOSEObjectType.JWT).build(),new JWTClaimsSet.Builder().issuer("privy.io").audience("synthetic-vote25-app").subject(sub).issueTime(Date.from(NOW.minusSeconds(60))).expirationTime(Date.from(NOW.plusSeconds(3600))).claim("sid","synthetic-vote-session").build());jwt.sign(new ECDSASigner(KEY));return jwt.serialize();}
    HttpResponse<String> vote(String sub,long option,boolean confirm)throws Exception{return request(sub,"{\"optionId\":"+option+",\"confirmChange\":"+confirm+"}");}
    HttpResponse<String> request(String sub,String body)throws Exception{return HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/v1/posts/"+post+"/vote")).timeout(Duration.ofSeconds(15)).header("Authorization","Bearer "+token(sub)).header("Content-Type","application/json").PUT(HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());}
    tools.jackson.databind.JsonNode data(HttpResponse<String> response)throws Exception{assertThat(response.statusCode()).isEqualTo(200);return JsonMapper.builder().build().readTree(response.body()).get("data");}

    @Test void firstVoteAndRetryStoreOneCurrentChoiceAndReturnOnlyAggregateAndOwnChoice()throws Exception{
        var first=data(vote(subject,option1,false));assertThat(first.get("options").get(0).get("votePercentage").decimalValue()).isEqualByComparingTo("100.00");
        TIME.set(NOW.plusSeconds(1));var retry=data(vote(subject,option1,false));
        assertThat(retry.get("myOptionId").asLong()).isEqualTo(option1);assertThat(retry.get("participantCount").asLong()).isEqualTo(1);
        assertThat(retry.get("options").get(0).get("voteCount").asLong()).isEqualTo(1);assertThat(retry.toString()).doesNotContain("userId","did:privy");
        assertThat(admin().queryForObject("select count(*) from discushion.vote_selections where poll_id=? and user_id=?",Integer.class,poll,user)).isEqualTo(1);
        assertThat(admin().queryForObject("select first_submitted_at from discushion.vote_selections where poll_id=? and user_id=?",java.sql.Timestamp.class,poll,user).toInstant()).isEqualTo(NOW);
        assertThat(admin().queryForObject("select updated_at from discushion.vote_selections where poll_id=? and user_id=?",java.sql.Timestamp.class,poll,user).toInstant()).isEqualTo(NOW);
    }
    @Test void changeRequiresConfirmationAndAtomicallyReplacesOnlyCurrentChoice()throws Exception{
        data(vote(subject,option1,false));var blocked=request(subject,"{\"optionId\":"+option2+",\"confirmChange\":false}");assertThat(blocked.statusCode()).isEqualTo(409);assertThat(blocked.body()).contains("VOTE_CHANGE_CONFIRMATION_REQUIRED");
        TIME.set(NOW.plusSeconds(1));data(vote(subject,option2,true));data(vote(otherSubject,option1,false));var result=data(vote(subject,option2,false));
        assertThat(result.get("myOptionId").asLong()).isEqualTo(option2);assertThat(result.get("participantCount").asLong()).isEqualTo(2);
        assertThat(result.get("options").get(0).get("voteCount").asLong()).isEqualTo(1);assertThat(result.get("options").get(1).get("voteCount").asLong()).isEqualTo(1);
        assertThat(admin().queryForObject("select first_submitted_at from discushion.vote_selections where poll_id=? and user_id=?",java.sql.Timestamp.class,poll,user).toInstant()).isEqualTo(NOW);
        assertThat(admin().queryForObject("select updated_at from discushion.vote_selections where poll_id=? and user_id=?",java.sql.Timestamp.class,poll,user).toInstant()).isEqualTo(NOW.plusSeconds(1));
    }
    @Test void invalidOptionMissingRegionAndClosedBoundaryDoNotWrite()throws Exception{
        assertThat(vote(subject,999999,false).statusCode()).isEqualTo(400);admin().update("delete from discushion.neighbor_verified_regions where user_id=?",user);
        assertThat(vote(subject,option1,false).statusCode()).isEqualTo(403);admin().update("insert into discushion.neighbor_verified_regions(user_id,region_id,verified_at) values(?,?,?::timestamptz)",user,region,NOW.toString());
        TIME.set(NOW.plusSeconds(60));assertThat(vote(subject,option1,false).statusCode()).isEqualTo(409);
        assertThat(admin().queryForObject("select count(*) from discushion.vote_selections where poll_id=?",Integer.class,poll)).isZero();
    }
    @Test void deletedPostIsHiddenAndServerRoleHasOnlyVoteReadInsertUpdate()throws Exception{
        data(vote(subject,option1,false));admin().update("update discushion.posts set status='DELETED',deleted_at=?::timestamptz where id=?",NOW.toString(),post);
        assertThat(vote(subject,option2,true).statusCode()).isEqualTo(404);var jdbc=new JdbcTemplate(source);
        assertThat(jdbc.queryForObject("select current_user",String.class)).isEqualTo("discushion_server");
        for(String sql:List.of("delete from discushion.vote_selections where poll_id="+poll,"truncate discushion.vote_selections","create table discushion.vote25_denied(id int)","select * from discushion.activity_events"))
            assertThatThrownBy(()->jdbc.execute(sql)).isInstanceOf(org.springframework.dao.DataAccessException.class);
    }
    private static ECKey key(){try{return new ECKeyGenerator(Curve.P_256).generate();}catch(Exception failure){throw new ExceptionInInitializerError(failure);}}
}
