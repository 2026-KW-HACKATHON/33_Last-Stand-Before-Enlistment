package com.discushion.identity;

import com.discushion.contracts.identity.CurrentActorProvider;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
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
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import static org.assertj.core.api.Assertions.*;

/** Actual HTTP/filter/JDBC + synthetic provider protocol. No product endpoint or real OTP delivery. */
@EnabledIfEnvironmentVariable(named="DISCUSHION_TEST_JDBC_URL", matches="jdbc:postgresql://127\\.0\\.0\\.1:55432/discushion_migration_test(?:\\?.*)?")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, properties={
    "spring.profiles.active=local,issue6-http-test", "spring.config.import=",
    "PRIVY_APP_ID=synthetic-issue6-app", "PRIVY_APP_SECRET=synthetic-issue6-secret"})
@Import(EmailAuthenticationHttpIntegrationTests.Wiring.class)
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class EmailAuthenticationHttpIntegrationTests {
    private static final String APP = "synthetic-issue6-app";
    private static final ECKey KEY = key();
    private static final PrivyTestServer KEYS = server();
    private static final PrivyTestServer USERS = server();
    @LocalServerPort int port;
    @Autowired DataSource dataSource;
    private String subject;
    private Long userId;
    private final HttpClient http = PrivyHttpJson.client();

    @TestConfiguration(proxyBeanMethods=false)
    static class Wiring {
        @Bean DataSource fixtureDataSource() {
            return new DriverManagerDataSource(System.getenv("DISCUSHION_TEST_JDBC_URL"), "postgres", System.getenv("DISCUSHION_TEST_DB_PASSWORD"));
        }
        @Bean JdbcMemberStore fixtureMemberStore(DataSource source, Clock clock) { return new JdbcMemberStore(source, clock); }
        @Bean VerificationKeySource fixtureKeySource(Clock clock) {
            KEYS.json(new JWKSet(KEY.toPublicJWK()).toString());
            return new PrivyJwksVerificationKeySource(APP, KEYS.base(), PrivyHttpJson.client(), clock,
                Duration.ofMinutes(60), Duration.ofMinutes(10), Duration.ofSeconds(3));
        }
        @Bean @Primary PrivyVerifiedEmailSource fixtureEmailSource(Clock clock) {
            return new PrivyVerifiedEmailSource(APP, "synthetic-issue6-secret", USERS.base(), PrivyHttpJson.client(), clock, Duration.ofSeconds(3));
        }
        @Bean TestRoutes fixtureRoutes(AuthenticatedEmailService email, CurrentActorProvider actors) { return new TestRoutes(email, actors); }
    }

    @RestController @Profile("issue6-http-test")
    static class TestRoutes {
        private final AuthenticatedEmailService email;
        private final CurrentActorProvider actors;
        TestRoutes(AuthenticatedEmailService email, CurrentActorProvider actors) { this.email=email; this.actors=actors; }
        @GetMapping("/api/v1/issue6-test/email")
        Map<String, Object> email() {
            var value=email.currentVerifiedEmail();
            return Map.of("verified", value.isPresent(), "email", value.map(VerifiedEmail::address).orElse(""),
                "localMember", actors.current().orElseThrow().member().isPresent());
        }
    }

    @BeforeEach void prepare() {
        subject="did:privy:synthetic-issue6-http-" + UUID.randomUUID();
        userId=null;
        USERS.requests.clear();
        USERS.json(user(email("provider@example.invalid")));
    }
    @AfterEach void cleanup() {
        if (userId != null) new JdbcTemplate(dataSource).update("delete from discushion.users where id=? and privy_user_id=?", userId, subject);
    }
    @AfterAll static void closeProviderFixtures() { KEYS.close(); USERS.close(); }

    @Test void missingTokenCannotObtainAnEmail() throws Exception {
        var response=get("", null);
        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.body()).contains("UNAUTHORIZED", "traceId");
        assertThat(USERS.requests).isEmpty();
    }
    @Test void forgedAndExpiredTokensNeverTriggerEmailLookup() throws Exception {
        for (var token : List.of(token(key(), Instant.now().plusSeconds(120)), token(KEY, Instant.now().minusSeconds(10)))) {
            assertThat(get("", token).statusCode()).isEqualTo(401);
        }
        assertThat(USERS.requests).isEmpty();
    }
    @Test void realFilterAndProviderLookupAllowPreSignupWithoutCreatingMember() throws Exception {
        var response=get("", token());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"verified\":true", "\"localMember\":false", "provider@example.invalid");
        assertThat(USERS.requests).hasSize(1);
        assertThat(new JdbcTemplate(dataSource).queryForObject("select count(*) from discushion.users where privy_user_id=?", Long.class, subject)).isZero();
    }
    @Test void incompleteMemberRemainsIncompleteAndClientEmailCannotReplaceProviderFacts() throws Exception {
        var jdbc=new JdbcTemplate(dataSource);
        userId=jdbc.queryForObject("""
            insert into discushion.users(email,email_verified_at,created_at,updated_at,privy_user_id,registration_completed_at)
            values (?,?::timestamptz,?::timestamptz,?::timestamptz,?,null) returning id
            """, Long.class, "synthetic-issue6-http-" + UUID.randomUUID() + "@example.invalid",
            Instant.now().minusSeconds(60).toString(), Instant.now().minusSeconds(60).toString(), Instant.now().toString(), subject);
        var response=get("?email=attacker%40example.invalid&subject=did%3Aprivy%3Aother&userId=999", token());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("provider@example.invalid", "\"localMember\":true").doesNotContain("attacker", "999");
        assertThat(USERS.requests.get(0).path()).isEqualTo("/v1/users/" + subject);
        assertThat(jdbc.queryForObject("select registration_completed_at is null from discushion.users where id=?", Boolean.class, userId)).isTrue();
    }
    @Test void providerOutageAndRateLimitUseExistingErrorEnvelopeWithoutSensitiveDetails() throws Exception {
        for (var status : List.of(429, 503)) {
            USERS.respond(status, "application/json", "{\"secret\":\"synthetic-issue6-secret\",\"email\":\"provider@example.invalid\"}");
            var response=get("", token());
            assertThat(response.statusCode()).isEqualTo(503);
            assertThat(response.body()).contains("AUTH_PROVIDER_UNAVAILABLE", "traceId", "details")
                .doesNotContain("synthetic-issue6-secret", "provider@example.invalid", subject, "stackTrace");
        }
    }
    @Test void missingOrAmbiguousEmailDoesNotInventARegistrationResult() throws Exception {
        for (var value : List.of(user(), user(email("one@example.invalid"), email("two@example.invalid")))) {
            USERS.json(value);
            var response=get("", token());
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).contains("\"verified\":false", "\"localMember\":false");
        }
    }
    @Test void wrongProviderSubjectIsRejectedAndActorDoesNotLeakToNextRequest() throws Exception {
        USERS.json(user(email("provider@example.invalid")).replace(subject, "did:privy:other"));
        assertThat(get("", token()).statusCode()).isEqualTo(503);
        USERS.json(user(email("provider@example.invalid")));
        assertThat(get("", token()).statusCode()).isEqualTo(200);
        var requests=USERS.requests.size();
        assertThat(get("", null).statusCode()).isEqualTo(401);
        assertThat(USERS.requests).hasSize(requests);
    }

    private String token() throws Exception { return token(KEY, Instant.now().plusSeconds(120)); }
    private String token(ECKey key, Instant expiration) throws Exception {
        var claims=new JWTClaimsSet.Builder().issuer("privy.io").audience(APP).subject(subject)
            .issueTime(Date.from(Instant.now().minusSeconds(60))).expirationTime(Date.from(expiration)).claim("sid", "synthetic-session").build();
        var jwt=new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.ES256).type(JOSEObjectType.JWT).keyID("trusted").build(), claims);
        jwt.sign(new ECDSASigner(key)); return jwt.serialize();
    }
    private HttpResponse<String> get(String query, String token) throws Exception {
        var request=HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/issue6-test/email" + query)).timeout(Duration.ofSeconds(5));
        if (token != null) request.header("Authorization", "Bearer " + token);
        return http.send(request.GET().build(), HttpResponse.BodyHandlers.ofString());
    }
    private String user(String... accounts) {
        return "{\"id\":\"" + subject + "\",\"linked_accounts\":[" + String.join(",", accounts) + "]}";
    }
    private String email(String address) {
        return "{\"type\":\"email\",\"address\":\"" + address + "\",\"latest_verified_at\":" + Instant.now().minusSeconds(60).getEpochSecond() + "}";
    }
    private static ECKey key() {
        try { return new ECKeyGenerator(Curve.P_256).keyID("trusted").generate(); }
        catch (Exception failure) { throw new ExceptionInInitializerError(failure); }
    }
    private static PrivyTestServer server() {
        try { return new PrivyTestServer(); } catch (Exception failure) { throw new ExceptionInInitializerError(failure); }
    }
}
