package com.discushion.identity;

import com.discushion.contracts.identity.CurrentActorProvider;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.*;
import java.net.URI;
import java.net.http.*;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.Base64;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import static org.assertj.core.api.Assertions.*;

/** Test-only routes and generated signing key; actual HTTP + JDBC, never the real Privy app or FE. */
@EnabledIfEnvironmentVariable(named="DISCUSHION_TEST_JDBC_URL",matches="jdbc:postgresql://127\\.0\\.0\\.1:55432/discushion_migration_test(?:\\?.*)?")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, properties={
    "spring.profiles.active=local,issue4-http-test", "spring.config.import=", "PRIVY_APP_ID=synthetic-issue4-app"})
@Import(IdentityHttpIntegrationTests.Wiring.class)
class IdentityHttpIntegrationTests {
    private static final com.nimbusds.jose.jwk.ECKey KEY = generatedKey();
    @LocalServerPort int port;
    @Autowired DataSource dataSource;
    private String subject;
    private Long userId;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(5)).build();

    @TestConfiguration(proxyBeanMethods=false)
    static class Wiring {
        @Bean DataSource fixtureDataSource() {
            return new DriverManagerDataSource(System.getenv("DISCUSHION_TEST_JDBC_URL"),"postgres",System.getenv("DISCUSHION_TEST_DB_PASSWORD"));
        }
        @Bean JdbcMemberStore fixtureMemberStore(DataSource source, Clock clock) {return new JdbcMemberStore(source,clock);}
        @Bean VerificationKeySource fixturePublicKey() throws Exception {
            String pem="-----BEGIN PUBLIC KEY-----\n"+Base64.getEncoder().encodeToString(KEY.toECPublicKey().getEncoded())+"\n-----END PUBLIC KEY-----";
            return new PemVerificationKeySource(pem);
        }
        @Bean TestRoutes fixtureRoutes(CurrentActorProvider actors, JdbcMemberStore members,Clock clock) {
            return new TestRoutes(actors,new MemberAuthorization(actors,members,clock));
        }
    }

    @RestController
    @org.springframework.context.annotation.Profile("issue4-http-test")
    static class TestRoutes {
        private final CurrentActorProvider actors;
        private final MemberAuthorization authorization;
        TestRoutes(CurrentActorProvider actors,MemberAuthorization authorization) {this.actors=actors;this.authorization=authorization;}
        @GetMapping("/api/v1/issue4-test/current")
        Map<String,Object> current() {return Map.of("authenticated",actors.current().isPresent());}
        @GetMapping("/api/v1/issue4-test/member")
        @Transactional
        public Map<String,Object> member() {return Map.of("userId",authorization.lockCurrentCompletedMember().userId());}
        @GetMapping("/api/v1/issue4-test/error")
        Map<String,Object> error() {throw new IllegalStateException("synthetic-sensitive-DB-detail");}
    }

    @BeforeEach void prepare() {subject="did:privy:synthetic-issue4-http-"+UUID.randomUUID();userId=null;}
    @AfterEach void cleanup() {
        if(userId!=null) new JdbcTemplate(dataSource).update("delete from discushion.users where id=? and privy_user_id=?",userId,subject);
    }
    private void member(boolean completed) {
        var jdbc=new JdbcTemplate(dataSource);
        userId=jdbc.queryForObject("""
            insert into discushion.users(email,email_verified_at,created_at,updated_at,privy_user_id,registration_completed_at)
            values (?,?::timestamptz,?::timestamptz,?::timestamptz,?,?::timestamptz) returning id
            """,Long.class,"synthetic-issue4-http-"+UUID.randomUUID()+"@example.invalid",Instant.now().minusSeconds(60).toString(),
            Instant.now().minusSeconds(60).toString(),Instant.now().toString(),subject,completed?Instant.now().toString():null);
    }
    private String token() throws Exception {
        var now=Instant.now();
        var claims=new JWTClaimsSet.Builder().issuer("privy.io").audience("synthetic-issue4-app").subject(subject)
            .issueTime(Date.from(now.minusSeconds(1))).expirationTime(Date.from(now.plusSeconds(300))).claim("sid","synthetic-session").build();
        var jwt=new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.ES256).type(JOSEObjectType.JWT).build(),claims);
        jwt.sign(new ECDSASigner(KEY));return jwt.serialize();
    }
    private HttpResponse<String> get(String path,String token) throws Exception {
        var request=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).timeout(java.time.Duration.ofSeconds(5));
        if(token!=null) request.header("Authorization","Bearer "+token);
        return http.send(request.GET().build(),HttpResponse.BodyHandlers.ofString());
    }

    @Test void missingTokenIsAnonymousOnlyOnAnonymousRoute() throws Exception {
        assertThat(get("/api/v1/issue4-test/current",null).body()).contains("\"authenticated\":false");
        var result=get("/api/v1/issue4-test/member",null);
        assertThat(result.statusCode()).isEqualTo(401);
        assertThat(result.body()).contains("UNAUTHORIZED","traceId");
    }
    @Test void invalidTokenDoesNotFallBackToGuest() throws Exception {
        var result=get("/api/v1/issue4-test/current","synthetic-invalid-token");
        assertThat(result.statusCode()).isEqualTo(401);
        assertThat(result.body()).doesNotContain("synthetic-invalid-token","authenticated");
    }
    @Test void unregisteredAndIncompleteBothRequireSignup() throws Exception {
        var absent=get("/api/v1/issue4-test/member",token());
        assertThat(absent.statusCode()).isEqualTo(403);
        assertThat(absent.body()).contains("USER_REGISTRATION_REQUIRED").doesNotContain(subject);
        member(false);
        var incomplete=get("/api/v1/issue4-test/member",token());
        assertThat(incomplete.statusCode()).isEqualTo(403);
        assertThat(incomplete.body()).contains("USER_REGISTRATION_REQUIRED").doesNotContain("REGISTRATION_INCOMPLETE");
    }
    @Test void validJwtUsesActualMemberAndDoesNotLeakAcrossRequests() throws Exception {
        member(true);
        var result=get("/api/v1/issue4-test/member?userId=999",token());
        assertThat(result.statusCode()).isEqualTo(200);
        assertThat(result.body()).contains("\"userId\":"+userId).doesNotContain("999",subject);
        assertThat(get("/api/v1/issue4-test/current",null).body()).contains("\"authenticated\":false");
    }
    @Test void internalErrorsKeepEnvelopeAndHideRawDetails() throws Exception {
        var result=get("/api/v1/issue4-test/error",null);
        assertThat(result.statusCode()).isEqualTo(500);
        assertThat(result.body()).contains("INTERNAL_ERROR","details","traceId")
            .doesNotContain("synthetic-sensitive","stackTrace");
    }
    private static com.nimbusds.jose.jwk.ECKey generatedKey() {
        try{return new ECKeyGenerator(Curve.P_256).generate();}catch(Exception error){throw new ExceptionInInitializerError(error);}
    }
}
