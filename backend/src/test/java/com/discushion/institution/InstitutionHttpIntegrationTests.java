package com.discushion.institution;

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
import org.springframework.jdbc.datasource.*;
import org.springframework.test.annotation.DirtiesContext;
import static org.assertj.core.api.Assertions.*;

/** Real local server LOGIN, HTTP, production filter and JDBC. Signing keys and seed facts are synthetic. */
@EnabledIfEnvironmentVariable(named = "DISCUSHION_TEST_JDBC_URL",
        matches = "jdbc:postgresql://127\\.0\\.0\\.1:55432/discushion_migration_test(?:\\?.*)?")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.profiles.active=institution12-http-test", "spring.config.import=", "PRIVY_APP_ID=synthetic-institution12-app"})
@Import(InstitutionHttpIntegrationTests.Wiring.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class InstitutionHttpIntegrationTests {
    private static final Instant NOW = Instant.parse("2026-10-08T00:00:00Z");
    private static final ECKey KEY = key();
    private static HikariDataSource runtime;
    private static boolean activated;
    private static JdbcTemplate admin() {
        return new JdbcTemplate(adminSource());
    }
    private static DriverManagerDataSource adminSource() {
        return new DriverManagerDataSource(System.getenv("DISCUSHION_TEST_JDBC_URL"), "postgres", System.getenv("DISCUSHION_TEST_DB_PASSWORD"));
    }
    @TestConfiguration(proxyBeanMethods = false) static class Wiring {
        @Bean @Primary Clock institutionFixtureClock() { return Clock.fixed(NOW, ZoneOffset.UTC); }
        @Bean VerificationKeySource institutionFixtureKeys() throws Exception {
            return new PemVerificationKeySource("-----BEGIN PUBLIC KEY-----\n" + Base64.getEncoder().encodeToString(KEY.toECPublicKey().getEncoded()) + "\n-----END PUBLIC KEY-----");
        }
        @Bean DataSource institutionRuntimeDataSource() {
            assertThat(admin().queryForObject("select inet_server_addr()='127.0.0.1'::inet and current_database()='discushion_migration_test'", Boolean.class)).isTrue();
            assertThat(admin().queryForObject("select not rolcanlogin from pg_roles where rolname='discushion_server'", Boolean.class)).isTrue();
            String password = UUID.randomUUID().toString().replace("-", "");
            admin().execute("alter role discushion_server login password '" + password + "'");
            activated = true;
            try {
                var config = new HikariConfig(); config.setJdbcUrl(System.getenv("DISCUSHION_TEST_JDBC_URL"));
                config.setUsername("discushion_server"); config.setPassword(password); config.setMaximumPoolSize(2); config.setConnectionTimeout(5000);
                runtime = new HikariDataSource(config); return runtime;
            } catch (RuntimeException failure) {
                admin().execute("alter role discushion_server nologin password null"); activated = false; throw failure;
            }
        }
    }
    @AfterAll static void restoreRole() {
        try { if (runtime != null) runtime.close(); }
        finally { if (activated) { admin().execute("alter role discushion_server nologin password null"); activated = false; } }
    }
    @Autowired DataSource source;
    @LocalServerPort int port;
    private String marker, subject;
    private long user, other, region, institution;
    @BeforeEach void setup() {
        marker = "synthetic-institution12-http-" + UUID.randomUUID(); subject = "did:privy:" + marker;
        region = admin().queryForObject("insert into discushion.regions(name) values(?) returning id", Long.class, marker);
        institution = admin().queryForObject("insert into discushion.institutions(name,created_at) values(?,?::timestamptz) returning id", Long.class, marker, NOW.toString());
        user = user(subject); other = user(subject + "-other");
    }
    private long user(String sub) {
        return admin().queryForObject("""
                insert into discushion.users(email,email_verified_at,created_at,updated_at,privy_user_id,registration_completed_at)
                values(?,?::timestamptz,?::timestamptz,?::timestamptz,?,?::timestamptz) returning id
                """, Long.class, sub.substring("did:privy:".length()) + "@example.invalid", NOW.toString(), NOW.toString(), NOW.toString(), sub, NOW.toString());
    }
    @AfterEach void cleanup() {
        for (long id : new long[]{user, other}) {
            admin().update("delete from discushion.institution_credentials where user_id=?", id);
            admin().update("delete from discushion.users where id=?", id);
        }
        admin().update("delete from discushion.institutions where id=?", institution);
        admin().update("delete from discushion.regions where id=?", region);
    }
    private String token(String sub, Instant until) throws Exception {
        var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.ES256).type(JOSEObjectType.JWT).build(),
                new JWTClaimsSet.Builder().issuer("privy.io").audience("synthetic-institution12-app").subject(sub)
                        .issueTime(Date.from(NOW.minusSeconds(60))).expirationTime(Date.from(until)).claim("sid", "synthetic-session").build());
        jwt.sign(new ECDSASigner(KEY)); return jwt.serialize();
    }
    private String token() throws Exception { return token(subject, NOW.plusSeconds(120)); }
    private HttpResponse<String> request(String method, String query, String access) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/institution-verifications" + query)).timeout(Duration.ofSeconds(10));
        if (access != null) builder.header("Authorization", "Bearer " + access);
        return HttpClient.newHttpClient().send(builder.method(method, HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
    }
    private void seed(long owner, Instant completed) {
        var data = adminSource();
        new InstitutionDemoProvisioner(data, new DataSourceTransactionManager(data), Clock.fixed(NOW, ZoneOffset.UTC))
                .add(owner, institution, region, completed);
    }

    @Test void actualServerLoginReturnsEmptyOrActiveRepresentativeWithEstablishedEnvelope() throws Exception {
        assertThat(new JdbcTemplate(source).queryForObject("select current_user", String.class)).isEqualTo("discushion_server");
        var empty = request("GET", "", token()); assertThat(empty.statusCode()).isEqualTo(200);
        assertThat(empty.body()).isEqualTo("{\"data\":{\"id\":" + user + ",\"institutionVerification\":{\"status\":\"NOT_SUBMITTED\",\"institutionId\":null,\"institutionName\":null,\"responsibleRegion\":null,\"completedAt\":null,\"validUntil\":null,\"isActive\":false},\"institutionVerified\":false}}");
        seed(user, NOW);
        var active = request("GET", "", token()); assertThat(active.statusCode()).isEqualTo(200);
        assertThat(active.body()).contains("\"id\":" + user, "\"institutionId\":" + institution, "\"status\":\"COMPLETED\"", marker, "2027-10-08T09:00:00+09:00", "\"isActive\":true", "\"institutionVerified\":true");
        assertThat(active.body()).doesNotContain(subject, "email", "requestId", "evidence", "credentials");
    }
    @Test void representativePrefersCurrentValidityThenLatestHistoryAndPreservesAllRows() throws Exception {
        seed(user, NOW.atZone(ZoneOffset.UTC).minusYears(2).toInstant());
        var expired = request("GET", "", token()); assertThat(expired.statusCode()).isEqualTo(200);
        assertThat(expired.body()).contains("\"status\":\"EXPIRED\"", "\"institutionVerified\":false");
        seed(user, NOW); seed(user, NOW.atZone(ZoneOffset.UTC).plusYears(2).toInstant());
        var active = request("GET", "", token());
        assertThat(active.body()).contains("2026-10-08T09:00:00+09:00", "\"isActive\":true").doesNotContain("2028-10-08");
        assertThat(admin().queryForObject("select count(*) from discushion.institution_credentials where user_id=?", Integer.class, user)).isEqualTo(3);
    }
    @Test void anonymousInvalidExpiredMissingAndIncompleteMembersUseExistingErrors() throws Exception {
        for (String access : Arrays.asList(null, "invalid", token(subject, NOW.minusSeconds(1))))
            assertThat(request("GET", "", access).statusCode()).isEqualTo(401);
        assertThat(request("GET", "", token(subject + "-missing", NOW.plusSeconds(120))).statusCode()).isEqualTo(403);
        admin().update("update discushion.users set registration_completed_at=null where id=?", user);
        var incomplete = request("GET", "", token()); assertThat(incomplete.statusCode()).isEqualTo(403);
        assertThat(incomplete.body()).contains("USER_REGISTRATION_REQUIRED");
    }
    @Test void clientUserIdCannotSelectAnotherMemberAndNoPublicGrantApiOrRuntimeWriteExists() throws Exception {
        seed(other, NOW);
        var own = request("GET", "?userId=" + other, token()); assertThat(own.statusCode()).isEqualTo(200);
        assertThat(own.body()).contains("NOT_SUBMITTED", "\"institutionVerified\":false");
        assertThat(request("POST", "", token()).statusCode()).isEqualTo(405);
        try (var connection = source.getConnection(); var statement = connection.createStatement()) {
            assertThatThrownBy(() -> statement.execute("insert into discushion.institution_credentials(user_id) values(1)"))
                    .isInstanceOfSatisfying(SQLException.class, error -> assertThat(error.getSQLState()).isEqualTo("42501"));
        }
        assertThat(admin().queryForObject("select count(*) from discushion.institution_credentials where user_id=?", Integer.class, user)).isZero();
    }
    @Test void missingInstitutionReadPermissionReturnsSafeInternalErrorAndRecoversAfterRestore() throws Exception {
        admin().execute("revoke select on discushion.institutions from discushion_server");
        try {
            var denied = request("GET", "", token()); assertThat(denied.statusCode()).isEqualTo(500);
            assertThat(denied.body()).contains("INTERNAL_ERROR").doesNotContain("permission denied", "discushion", "jdbc", "postgres");
        } finally { admin().execute("grant select on discushion.institutions to discushion_server"); }
        assertThat(request("GET", "", token()).statusCode()).isEqualTo(200);
    }
    private static ECKey key() {
        try { return new ECKeyGenerator(Curve.P_256).generate(); }
        catch (Exception error) { throw new ExceptionInInitializerError(error); }
    }
}
