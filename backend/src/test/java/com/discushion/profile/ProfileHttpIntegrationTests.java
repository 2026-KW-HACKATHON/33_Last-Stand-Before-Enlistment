package com.discushion.profile;

import com.discushion.identity.*;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.*;
import java.net.URI;
import java.net.http.*;
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
import static org.assertj.core.api.Assertions.*;

/** Real HTTP/filter/signature/JDBC with synthetic keys, not actual Privy OTP or FE integration. */
@EnabledIfEnvironmentVariable(named="DISCUSHION_TEST_JDBC_URL",matches="jdbc:postgresql://127\\.0\\.0\\.1:55432/discushion_migration_test(?:\\?.*)?")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
    "spring.profiles.active=profile10-http-test","spring.config.import=","PRIVY_APP_ID=synthetic-profile10-app"})
@Import(ProfileHttpIntegrationTests.Wiring.class)
class ProfileHttpIntegrationTests {
    private static final ECKey KEY=key();
    @TestConfiguration(proxyBeanMethods=false) static class Wiring {
        @Bean DataSource profileFixtureDataSource() {return new DriverManagerDataSource(System.getenv("DISCUSHION_TEST_JDBC_URL"),"postgres",System.getenv("DISCUSHION_TEST_DB_PASSWORD"));}
        @Bean VerificationKeySource profileFixtureKeys() throws Exception {
            return new PemVerificationKeySource("-----BEGIN PUBLIC KEY-----\n"+Base64.getEncoder().encodeToString(KEY.toECPublicKey().getEncoded())+"\n-----END PUBLIC KEY-----");
        }
    }
    @LocalServerPort int port; @Autowired DataSource source;
    private String marker,subject; private long user,region;
    private JdbcTemplate jdbc() {return new JdbcTemplate(source);}
    @BeforeEach void setup() {
        marker="synthetic-profile10-http-"+UUID.randomUUID();subject="did:privy:"+marker;
        region=jdbc().queryForObject("insert into discushion.regions(name) values(?) returning id",Long.class,marker);
        user=jdbc().queryForObject("""
            insert into discushion.users(email,email_verified_at,created_at,updated_at,privy_user_id,registration_completed_at)
            values(?,now(),now(),now(),?,now()) returning id
            """,Long.class,marker+"@example.invalid",subject);
        jdbc().update("insert into discushion.profiles(user_id,nickname,bio,activity_region_id,updated_at) values(?,?,?, ?,now())",user,"p"+user,"oldbio",region);
    }
    @AfterEach void cleanup() {
        jdbc().update("delete from discushion.profile_attributes where user_id=?",user);
        jdbc().update("delete from discushion.profiles where user_id=?",user);
        jdbc().update("delete from discushion.users where id=?",user);
        jdbc().update("delete from discushion.regions where id=?",region);
    }
    private String token(String sub,ECKey key,Instant until) throws Exception {
        var claims=new JWTClaimsSet.Builder().issuer("privy.io").audience("synthetic-profile10-app").subject(sub)
            .issueTime(Date.from(Instant.now().minusSeconds(60))).expirationTime(Date.from(until)).claim("sid","synthetic-profile10-session").build();
        var jwt=new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.ES256).type(JOSEObjectType.JWT).build(),claims);
        jwt.sign(new ECDSASigner(key));return jwt.serialize();
    }
    private String token() throws Exception {return token(subject,KEY,Instant.now().plusSeconds(120));}
    private HttpResponse<String> request(String method,String path,String body,String access) throws Exception {
        var builder=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).timeout(Duration.ofSeconds(10));
        if(access!=null) builder.header("Authorization","Bearer "+access);
        if(body!=null) builder.header("Content-Type","application/json");
        return HttpClient.newHttpClient().send(builder.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
    }
    @Test void getAndPatchReturnCurrentProfileAndNeverCreateQualifications() throws Exception {
        var get=request("GET","/api/v1/users/me",null,token());assertThat(get.statusCode()).isEqualTo(200);
        assertThat(get.body()).contains("\"id\":"+user,"\"bio\":\"oldbio\"","\"institutionVerified\":false","\"neighborVerifiedRegions\":[]");
        var patch=request("PATCH","/api/v1/users/me","{\"bio\":null,\"residentAttributes\":[\"WORKER\",\"STUDENT\"]}",token());
        assertThat(patch.statusCode()).isEqualTo(200);assertThat(patch.body()).contains("\"bio\":null","STUDENT","WORKER","\"nickname\":\"p"+user+"\"");
        assertThat(request("GET","/api/v1/users/me",null,token()).body()).isEqualTo(patch.body());
        assertThat(patch.headers().allValues("Set-Cookie")).isEmpty();
        assertThat(jdbc().queryForObject("select count(*) from discushion.neighbor_verified_regions where user_id=?",Integer.class,user)).isZero();
    }
    @Test void missingInvalidExpiredAndForgedTokensAre401() throws Exception {
        for(String access:Arrays.asList(null,"invalid",token(subject,KEY,Instant.now().minusSeconds(10)),token(subject,key(),Instant.now().plusSeconds(120)))) {
            var result=request("GET","/api/v1/users/me",null,access);assertThat(result.statusCode()).isEqualTo(401);
            assertThat(result.body()).contains("UNAUTHORIZED");assertThat(result.headers().firstValue("WWW-Authenticate")).contains("Bearer");
        }
    }
    @Test void missingAndIncompleteMembershipAre403AndNoClientIdCanSelectAnotherUser() throws Exception {
        assertThat(request("GET","/api/v1/users/me",null,token(subject+"-missing",KEY,Instant.now().plusSeconds(120))).statusCode()).isEqualTo(403);
        var get=request("GET","/api/v1/users/me?userId=999",null,token());assertThat(get.statusCode()).isEqualTo(200);
        assertThat(get.body()).contains("\"id\":"+user).doesNotContain("\"id\":999");
        jdbc().update("update discushion.users set registration_completed_at=null where id=?",user);
        for(String method:List.of("GET","PATCH")) {
            var result=request(method,"/api/v1/users/me",method.equals("GET")?null:"{\"bio\":\"blocked\"}",token());
            assertThat(result.statusCode()).isEqualTo(403);assertThat(result.body()).contains("USER_REGISTRATION_REQUIRED");
        }
        assertThat(jdbc().queryForObject("select bio from discushion.profiles where user_id=?",String.class,user)).isEqualTo("oldbio");
    }
    @Test void invalidJsonForbiddenFieldsAndRegionFailWithoutWritesOrEchoingSecrets() throws Exception {
        for(String body:List.of("{}","[]","null","{bad","{\"nickname\":null}","{\"residentAttributes\":null}",
                "{\"email\":\"synthetic-secret\"}","{\"removeProfileImage\":true}","{\"institutionVerified\":true}")) {
            var result=request("PATCH","/api/v1/users/me",body,token());assertThat(result.statusCode()).as(body).isEqualTo(400);
            assertThat(result.body()).contains("VALIDATION_ERROR","traceId").doesNotContain("synthetic-secret");
        }
        assertThat(request("PATCH","/api/v1/users/me","{\"activityRegionId\":9007199254740991}",token()).statusCode()).isEqualTo(404);
        assertThat(jdbc().queryForObject("select bio from discushion.profiles where user_id=?",String.class,user)).isEqualTo("oldbio");
    }
    @Test void inconsistentDatabaseIsMasked500AndNotInventedProfile() throws Exception {
        jdbc().update("delete from discushion.profiles where user_id=?",user);
        var result=request("GET","/api/v1/users/me",null,token());assertThat(result.statusCode()).isEqualTo(500);
        assertThat(result.body()).contains("INTERNAL_ERROR","traceId").doesNotContain("discushion.profiles",subject,"stackTrace");
    }
    private static ECKey key() {try{return new ECKeyGenerator(Curve.P_256).generate();}catch(Exception error){throw new ExceptionInInitializerError(error);}}
}
