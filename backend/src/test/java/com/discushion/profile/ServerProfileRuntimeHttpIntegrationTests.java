package com.discushion.profile;

import com.discushion.identity.*;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.*;
import com.zaxxer.hikari.*;
import java.net.URI;
import java.net.http.*;
import java.sql.SQLException;
import java.time.*;
import java.util.*;
import javax.sql.DataSource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.annotation.DirtiesContext;
import static org.assertj.core.api.Assertions.*;

/** Production HTTP/filter/profile adapters using an actual isolated password LOGIN, not SET ROLE/admin reads. */
@EnabledIfEnvironmentVariable(named="DISCUSHION_TEST_JDBC_URL",matches="jdbc:postgresql://127\\.0\\.0\\.1:55432/discushion_migration_test(?:\\?.*)?")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
    "spring.profiles.active=server-profile30-http-test","spring.config.import=","PRIVY_APP_ID=synthetic-server-profile30-app"})
@Import(ServerProfileRuntimeHttpIntegrationTests.Wiring.class)
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class ServerProfileRuntimeHttpIntegrationTests {
    private static final String ROLE="discushion_server";
    private static final Instant NOW=Instant.parse("2026-10-08T00:00:00Z");
    private static final ECKey KEY=key();
    private static HikariDataSource runtime;
    private static boolean activated;
    private static JdbcTemplate admin() {
        return new JdbcTemplate(new DriverManagerDataSource(System.getenv("DISCUSHION_TEST_JDBC_URL"),"postgres",System.getenv("DISCUSHION_TEST_DB_PASSWORD")));
    }
    @TestConfiguration(proxyBeanMethods=false) static class Wiring {
        @Bean @Primary Clock serverProfileFixtureClock() {return Clock.fixed(NOW,ZoneOffset.UTC);}
        @Bean VerificationKeySource serverProfileFixtureKeys() throws Exception {
            return new PemVerificationKeySource("-----BEGIN PUBLIC KEY-----\n"+Base64.getEncoder().encodeToString(KEY.toECPublicKey().getEncoded())+"\n-----END PUBLIC KEY-----");
        }
        @Bean DataSource serverProfileRuntimeDataSource() {
            var setup=admin();
            assertThat(setup.queryForObject("select inet_server_addr()='127.0.0.1'::inet and current_database()='discushion_migration_test'",Boolean.class)).isTrue();
            assertThat(setup.queryForObject("select not rolcanlogin from pg_roles where rolname=?",Boolean.class,ROLE)).isTrue();
            String password=UUID.randomUUID().toString().replace("-","");
            setup.execute("alter role discushion_server login password '"+password+"'");activated=true;
            try {
                var config=new HikariConfig();config.setJdbcUrl(System.getenv("DISCUSHION_TEST_JDBC_URL"));
                config.setUsername(ROLE);config.setPassword(password);config.setMaximumPoolSize(2);config.setConnectionTimeout(5000);
                runtime=new HikariDataSource(config);return runtime;
            } catch(RuntimeException failure) {setup.execute("alter role discushion_server nologin password null");activated=false;throw failure;}
        }
    }
    @AfterAll static void restoreIsolatedRole() {
        try {if(runtime!=null) runtime.close();}
        finally {if(activated){admin().execute("alter role discushion_server nologin password null");activated=false;}}
    }
    @LocalServerPort int port; @Autowired DataSource source;
    private String marker,subject; private long user,other,region,institution;
    private long createUser(String sub,boolean completed) {
        long id=admin().queryForObject("""
            insert into discushion.users(email,email_verified_at,created_at,updated_at,privy_user_id,registration_completed_at)
            values(?,?::timestamptz,?::timestamptz,?::timestamptz,?,?::timestamptz) returning id
            """,Long.class,sub.substring("did:privy:".length())+"@example.invalid",NOW.toString(),NOW.toString(),NOW.toString(),sub,completed?NOW.toString():null);
        admin().update("insert into discushion.profiles(user_id,nickname,bio,activity_region_id,updated_at) values(?,?,?, ?,?::timestamptz)",
            id,"s"+id,"original",region,NOW.toString());return id;
    }
    @BeforeEach void setup() {
        marker="synthetic-server-profile30-"+UUID.randomUUID();subject="did:privy:"+marker;
        region=admin().queryForObject("insert into discushion.regions(name) values(?) returning id",Long.class,marker);
        institution=admin().queryForObject("insert into discushion.institutions(name,created_at) values(?,?::timestamptz) returning id",Long.class,marker,NOW.toString());
        user=createUser(subject,true);other=createUser(subject+"-other",true);
    }
    @AfterEach void cleanup() {
        for(long id:new long[]{user,other}) {
            admin().update("delete from discushion.institution_credentials where user_id=?",id);
            admin().update("delete from discushion.neighbor_verified_regions where user_id=?",id);
            admin().update("delete from discushion.profile_attributes where user_id=?",id);
            admin().update("delete from discushion.profiles where user_id=?",id);
            admin().update("delete from discushion.users where id=?",id);
        }
        admin().update("delete from discushion.institutions where id=? and name=?",institution,marker);
        admin().update("delete from discushion.regions where id=? and name=?",region,marker);
    }
    private String token(String sub,Instant until) throws Exception {
        var jwt=new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.ES256).type(JOSEObjectType.JWT).build(),
            new JWTClaimsSet.Builder().issuer("privy.io").audience("synthetic-server-profile30-app").subject(sub)
                .issueTime(Date.from(NOW.minusSeconds(60))).expirationTime(Date.from(until)).claim("sid","synthetic-session").build());
        jwt.sign(new ECDSASigner(KEY));return jwt.serialize();
    }
    private String token() throws Exception {return token(subject,NOW.plusSeconds(120));}
    private HttpResponse<String> request(String method,String query,String body,String access) throws Exception {
        var builder=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/v1/users/me"+query)).timeout(Duration.ofSeconds(10));
        if(access!=null) builder.header("Authorization","Bearer "+access);
        if(body!=null) builder.header("Content-Type","application/json");
        return HttpClient.newHttpClient().send(builder.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
    }
    private void credential(Instant until) {
        admin().update("""
            insert into discushion.institution_credentials(user_id,institution_id,responsible_region_id,completed_at,valid_until)
            values(?,?,?,?::timestamptz,?::timestamptz)
            """,user,institution,region,NOW.minusSeconds(60).toString(),until.toString());
    }
    @Test void actualLoginAndRlsAllowGetAndPatchEvenWithoutInstitutionHistory() throws Exception {
        assertThat(new JdbcTemplate(source).queryForObject("select current_user",String.class)).isEqualTo(ROLE);
        var get=request("GET","",null,token());assertThat(get.statusCode()).isEqualTo(200);
        assertThat(get.body()).contains("NOT_SUBMITTED","\"institutionVerified\":false","\"id\":"+user);
        var patch=request("PATCH","","{\"bio\":\"changed\",\"residentAttributes\":[\"STUDENT\"]}",token());
        assertThat(patch.statusCode()).isEqualTo(200);assertThat(patch.body()).contains("changed","STUDENT","NOT_SUBMITTED");
        assertThat(request("GET","",null,token()).body()).isEqualTo(patch.body());
    }
    @Test void realInstitutionRowsAndExpirationBoundaryAreVisibleUnderSelectPolicy() throws Exception {
        credential(NOW.plusSeconds(60));
        var active=request("GET","",null,token());assertThat(active.statusCode()).isEqualTo(200);
        assertThat(active.body()).contains(marker,"\"institutionVerified\":true","\"status\":\"COMPLETED\"");
        admin().update("update discushion.institution_credentials set valid_until=?::timestamptz where user_id=?",NOW.toString(),user);
        var expired=request("PATCH","","{\"bio\":null}",token());assertThat(expired.statusCode()).isEqualTo(200);
        assertThat(expired.body()).contains("\"status\":\"EXPIRED\"","\"institutionVerified\":false","\"bio\":null");
    }
    @Test void missingOrExpiredTokenAndIncompleteOrUnknownMembershipRemainDenied() throws Exception {
        for(String access:Arrays.asList(null,"invalid",token(subject,NOW.minusSeconds(1))))
            assertThat(request("GET","",null,access).statusCode()).isEqualTo(401);
        assertThat(request("GET","",null,token(subject+"-unknown",NOW.plusSeconds(120))).statusCode()).isEqualTo(403);
        admin().update("update discushion.users set registration_completed_at=null where id=?",user);
        for(String method:List.of("GET","PATCH")) {
            var result=request(method,"",method.equals("GET")?null:"{\"bio\":\"blocked\"}",token());
            assertThat(result.statusCode()).isEqualTo(403);assertThat(result.body()).contains("USER_REGISTRATION_REQUIRED");
        }
        assertThat(admin().queryForObject("select bio from discushion.profiles where user_id=?",String.class,user)).isEqualTo("original");
    }
    @Test void anotherMemberQueryAndForbiddenBodyCannotSelectOrModifyTheirProfile() throws Exception {
        var get=request("GET","?userId="+other,null,token());assertThat(get.statusCode()).isEqualTo(200);
        assertThat(get.body()).contains("\"id\":"+user,"\"nickname\":\"s"+user+"\"").doesNotContain("\"nickname\":\"s"+other+"\"");
        assertThat(request("PATCH","","{\"userId\":"+other+",\"bio\":\"blocked\"}",token()).statusCode()).isEqualTo(400);
        assertThat(request("PATCH","?userId="+other,"{\"bio\":\"mine\"}",token()).statusCode()).isEqualTo(200);
        assertThat(admin().queryForObject("select bio from discushion.profiles where user_id=?",String.class,other)).isEqualTo("original");
    }
    @Test void missingInstitutionPermissionReproducesGetFailureAndPatchRollback() throws Exception {
        admin().execute("revoke select on discushion.institutions from discushion_server");
        try {
            assertThat(request("GET","",null,token()).statusCode()).isEqualTo(500);
            var result=request("PATCH","","{\"bio\":\"must-rollback\"}",token());
            assertThat(result.statusCode()).isEqualTo(500);assertThat(result.body()).contains("INTERNAL_ERROR").doesNotContain("permission denied","discushion.institutions");
            assertThat(admin().queryForObject("select bio from discushion.profiles where user_id=?",String.class,user)).isEqualTo("original");
        } finally {admin().execute("grant select on discushion.institutions to discushion_server");}
        assertThat(request("GET","",null,token()).statusCode()).isEqualTo(200);
    }
    @Test void selectGrantNeverAllowsInstitutionOrQualificationWritesOrSchemaChanges() throws Exception {
        for(String sql:List.of("insert into discushion.institutions(name,created_at) values('forbidden',now())",
                "update discushion.institutions set name='forbidden' where false","delete from discushion.institutions where false",
                "update discushion.institution_credentials set valid_until=now() where false",
                "create table discushion.forbidden_profile30(id bigint)")) {
            try(var connection=source.getConnection()) {
                connection.setAutoCommit(false);
                try {assertThatThrownBy(()->connection.createStatement().execute(sql)).isInstanceOfSatisfying(SQLException.class,
                    error->assertThat(error.getSQLState()).isEqualTo("42501"));} finally {connection.rollback();}
            }
        }
        assertThat(new JdbcTemplate(source).queryForObject("select has_table_privilege(current_user,'discushion.institutions','SELECT WITH GRANT OPTION')",Boolean.class)).isFalse();
        assertThat(new JdbcTemplate(source).queryForObject("select count(*) from pg_policies where schemaname='discushion' and tablename='institutions' and roles=array['discushion_server']::name[] and cmd='SELECT' and qual='true'",Integer.class)).isEqualTo(1);
    }
    private static ECKey key() {try{return new ECKeyGenerator(Curve.P_256).generate();}catch(Exception error){throw new ExceptionInInitializerError(error);}}
}
