package com.discushion.posts;

import com.discushion.contracts.post.PostContextReader;
import com.discushion.identity.PemVerificationKeySource;
import com.discushion.identity.VerificationKeySource;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.annotation.DirtiesContext;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;

/** Executes post creation and the production PostContextReader under discushion_server. */
@EnabledIfEnvironmentVariable(named = "DISCUSHION_TEST_JDBC_URL", matches = "jdbc:postgresql://127\\.0\\.0\\.1:55432/discushion_migration_test(?:\\?.*)?")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.profiles.active=post14-http-test", "spring.config.import=", "PRIVY_APP_ID=synthetic-post14-app",
        "PHOTO_UPLOADS_ENABLED=true", "PHOTO_CLEANUP_ENABLED=false" })
@Import(PostCreationHttpIntegrationTests.Wiring.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PostCreationHttpIntegrationTests {
    static final Instant NOW = Instant.parse("2026-10-08T00:00:00Z");
    static final ECKey KEY = key();
    static HikariDataSource runtime;
    static boolean activated;

    static DriverManagerDataSource adminSource() {
        return new DriverManagerDataSource(System.getenv("DISCUSHION_TEST_JDBC_URL"), "postgres",
                System.getenv("DISCUSHION_TEST_DB_PASSWORD"));
    }
    static JdbcTemplate admin() { return new JdbcTemplate(adminSource()); }

    @TestConfiguration(proxyBeanMethods = false)
    static class Wiring {
        // Creation links verified DB files; Storage transfer is tested by #13.
        @Bean com.discushion.photos.PhotoStorage postCreationStorage() {
            return org.mockito.Mockito.mock(com.discushion.photos.PhotoStorage.class);
        }
        @Bean @Primary java.time.Clock postCreationClock() { return java.time.Clock.fixed(NOW, java.time.ZoneOffset.UTC); }
        @Bean VerificationKeySource postCreationKeys() throws Exception {
            return new PemVerificationKeySource("-----BEGIN PUBLIC KEY-----\n"
                    + java.util.Base64.getEncoder().encodeToString(KEY.toECPublicKey().getEncoded())
                    + "\n-----END PUBLIC KEY-----");
        }
        @Bean DataSource postCreationRuntimeSource() {
            assertThat(admin().queryForObject(
                    "select inet_server_addr()='127.0.0.1'::inet and current_database()='discushion_migration_test'", Boolean.class))
                    .isTrue();
            assertThat(admin().queryForObject("select not rolcanlogin from pg_roles where rolname='discushion_server'", Boolean.class))
                    .isTrue();
            String password = UUID.randomUUID().toString().replace("-", "");
            admin().execute("alter role discushion_server login password '" + password + "'");
            activated = true;
            try {
                var config = new HikariConfig();
                config.setJdbcUrl(System.getenv("DISCUSHION_TEST_JDBC_URL"));
                config.setUsername("discushion_server");
                config.setPassword(password);
                config.setMaximumPoolSize(4);
                runtime = new HikariDataSource(config);
                return runtime;
            } catch (RuntimeException failure) {
                admin().execute("alter role discushion_server nologin password null");
                activated = false;
                throw failure;
            }
        }
    }

    @AfterAll static void restoreServerRole() {
        try { if (runtime != null) runtime.close(); }
        finally {
            if (activated) {
                admin().execute("alter role discushion_server nologin password null");
                activated = false;
            }
        }
    }

    @LocalServerPort int port;
    @Autowired DataSource source;
    @Autowired PostContextReader posts;
    @Autowired com.discushion.photos.PhotoStorage storage;
    String marker;
    String subject;
    long user;
    long region;

    @BeforeEach void setup() {
        marker = "synthetic-post14-" + UUID.randomUUID();
        subject = "did:privy:" + marker;
        region = admin().queryForObject("insert into discushion.regions(name) values(?) returning id", Long.class, marker);
        user = admin().queryForObject("""
                insert into discushion.users(email,email_verified_at,created_at,updated_at,privy_user_id,registration_completed_at)
                values(?,?::timestamptz,?::timestamptz,?::timestamptz,?,?::timestamptz) returning id
                """, Long.class, marker + "@example.invalid", NOW.toString(), NOW.toString(), NOW.toString(), subject, NOW.toString());
        admin().update("insert into discushion.neighbor_verified_regions(user_id,region_id,verified_at) values(?,?,?::timestamptz)",
                user, region, NOW.toString());
    }

    @AfterEach void cleanup() {
        admin().update("delete from discushion.post_photos where post_id in(select id from discushion.posts where author_user_id=?)", user);
        admin().update("delete from discushion.poll_options where poll_id in(select id from discushion.polls where post_id in(select id from discushion.posts where author_user_id=?))", user);
        admin().update("delete from discushion.polls where post_id in(select id from discushion.posts where author_user_id=?)", user);
        admin().update("delete from discushion.activity_post_details where post_id in(select id from discushion.posts where author_user_id=?)", user);
        admin().update("delete from discushion.posts where author_user_id=?", user);
        admin().update("delete from discushion.media_files where owner_user_id=?", user);
        admin().update("delete from discushion.neighbor_verified_regions where user_id=?", user);
        admin().update("delete from discushion.users where id=?", user);
        admin().update("delete from discushion.regions where id=?", region);
    }

    @Test void createsEveryPostTypeAtomicallyAndReturnsPostIdForCanonicalDetailNavigation() throws Exception {
        long agenda = created("""
                {"type":"LOCAL_AGENDA","topic":"SAFETY","regionId":%d,"title":"안건","content":"본문"}
                """.formatted(region));
        long activity = created("""
                {"type":"LOCAL_ACTIVITY","topic":"OTHER","regionId":%d,"title":"활동","content":"본문",
                 "details":{"source":"구청","schedule":"다음 주 토요일","place":"주민센터","activityStatus":"SCHEDULED"}}
                """.formatted(region));
        long vote = created("""
                {"type":"VOTE","topic":"OTHER","regionId":%d,"title":"투표","content":"본문",
                 "details":{"question":"질문","options":[" 찬성 ","반대"],"endsAt":"%s"}}
                """.formatted(region, databaseNow().plusSeconds(3600)));

        assertThat(posts.find(agenda)).isPresent();
        assertThat(posts.find(activity)).isPresent();
        assertThat(posts.find(vote).orElseThrow().poll()).isPresent();
        assertThat(posts.find(vote).orElseThrow().poll().orElseThrow().optionIds()).hasSize(2);
        assertThat(new JdbcTemplate(source).queryForObject("select count(*) from discushion.posts where id in (?,?,?)",
                Integer.class, agenda, activity, vote)).isEqualTo(3);
        assertThat(admin().queryForObject("select count(*) from discushion.activity_post_details where post_id=?", Integer.class, activity)).isEqualTo(1);
        assertThat(admin().queryForObject("select count(*) from discushion.poll_options o join discushion.polls p on p.id=o.poll_id where p.post_id=?", Integer.class, vote)).isEqualTo(2);
    }

    @Test void rejectsInvalidVoteWithoutPartialWritesAndRepeatedPostCreatesANewPost() throws Exception {
        long first = created("""
                {"type":"LOCAL_AGENDA","topic":"SAFETY","regionId":%d,"title":"안건","content":"본문"}
                """.formatted(region));
        long second = created("""
                {"type":"LOCAL_AGENDA","topic":"SAFETY","regionId":%d,"title":"안건","content":"본문"}
                """.formatted(region));
        assertThat(second).isNotEqualTo(first);

        var invalid = request("""
                {"type":"VOTE","topic":"OTHER","regionId":%d,"title":"투표","content":"본문",
                 "details":{"question":"질문","options":["예"," 예 "],"endsAt":"2026-10-10T18:00:00+09:00"}}
                """.formatted(region));
        assertThat(invalid.statusCode()).isEqualTo(400);
        assertThat(admin().queryForObject("select count(*) from discushion.posts where author_user_id=?", Integer.class, user)).isEqualTo(2);
        assertThat(new JdbcTemplate(source).queryForObject("select current_user", String.class)).isEqualTo("discushion_server");
    }

    @Test void requiresVerifiedRegionAtWriteTime() throws Exception {
        admin().update("delete from discushion.neighbor_verified_regions where user_id=?", user);
        var denied = request("""
                {"type":"LOCAL_AGENDA","topic":"SAFETY","regionId":%d,"title":"안건","content":"본문"}
                """.formatted(region));
        assertThat(denied.statusCode()).isEqualTo(403);
        assertThat(admin().queryForObject("select count(*) from discushion.posts where author_user_id=?", Integer.class, user)).isZero();
    }

    @Test void linksExactlyTenMillionBytesInOrderAndRejectsReuseWithoutOrphanPost() throws Exception {
        long first = photo(5_000_000), second = photo(5_000_000);
        long post = created(payload("LOCAL_AGENDA", java.util.List.of(second, first), null));
        assertThat(admin().queryForList("select file_id from discushion.post_photos where post_id=? order by sort_order", Long.class, post))
                .containsExactly(second, first);
        assertThat(admin().queryForList("select lifecycle_status from discushion.media_files where owner_user_id=?", String.class, user))
                .containsOnly("LINKED");
        assertThat(request(payload("LOCAL_AGENDA", java.util.List.of(first), null)).statusCode()).isEqualTo(409);
        assertThat(postCount()).isEqualTo(1);
        org.mockito.Mockito.verifyNoInteractions(storage);
    }

    @Test void photoFailureRollsBackPostPollAndOptionsAndLeavesFilesUnlinked() throws Exception {
        long first = photo(5_000_000), second = photo(5_000_001);
        var failure = request(payload("VOTE", java.util.List.of(first, second), databaseNow().plusSeconds(3600)));
        assertThat(failure.statusCode()).isEqualTo(413);
        assertThat(failure.body()).contains("PHOTO_SIZE_EXCEEDED");
        assertThat(postCount()).isZero();
        assertThat(admin().queryForObject("select count(*) from discushion.polls where question=?", Integer.class, marker)).isZero();
        assertThat(admin().queryForList("select lifecycle_status from discushion.media_files where owner_user_id=?", String.class, user))
                .containsOnly("UNLINKED");
    }

    @Test void pastVoteAndExpiredPhotoAreRejectedWithoutWrites() throws Exception {
        var past = request(payload("VOTE", java.util.List.of(), databaseNow().minusSeconds(1)));
        assertThat(past.statusCode()).isEqualTo(400);
        assertThat(past.body()).contains("VALIDATION_ERROR");
        long file = photo(100);
        admin().update("update discushion.media_files set created_at=clock_timestamp()-interval '26 hours', uploaded_at=clock_timestamp()-interval '25 hours' where id=?", file);
        var expired = request(payload("LOCAL_AGENDA", java.util.List.of(file), null));
        assertThat(expired.statusCode()).isEqualTo(409);
        assertThat(expired.body()).contains("PHOTO_UPLOAD_EXPIRED");
        assertThat(postCount()).isZero();
    }

    @Test void simultaneousRequestsCannotAttachOneFileToTwoPosts() throws Exception {
        long file = photo(100);
        String body = payload("LOCAL_AGENDA", java.util.List.of(file), null);
        var executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        var ready = new java.util.concurrent.CountDownLatch(2);
        var start = new java.util.concurrent.CountDownLatch(1);
        try {
            java.util.concurrent.Callable<Integer> call = () -> { ready.countDown(); start.await(); return request(body).statusCode(); };
            var one = executor.submit(call); var two = executor.submit(call);
            assertThat(ready.await(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue(); start.countDown();
            assertThat(java.util.List.of(one.get(20, java.util.concurrent.TimeUnit.SECONDS), two.get(20, java.util.concurrent.TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(201, 409);
            assertThat(postCount()).isEqualTo(1);
            assertThat(admin().queryForObject("select count(*) from discushion.post_photos where file_id=?", Integer.class, file)).isEqualTo(1);
        } finally { start.countDown(); executor.shutdownNow(); }
    }

    @Test void runtimeRoleCannotPhysicallyDeletePostsOrChangeSchema() {
        var runtimeJdbc = new JdbcTemplate(source);
        for (String sql : java.util.List.of("delete from discushion.posts", "delete from discushion.polls",
                "delete from discushion.poll_options", "delete from discushion.activity_post_details",
                "create table discushion.post14_denied(id bigint)")) {
            assertThatThrownBy(() -> runtimeJdbc.execute(sql)).isInstanceOf(org.springframework.dao.DataAccessException.class);
        }
        assertThatThrownBy(() -> posts.findForUpdate(1)).isInstanceOf(IllegalStateException.class);
    }

    @Test void rejectsAnotherMembersFileAndUnauthenticatedCreation() throws Exception {
        long other = admin().queryForObject("""
                insert into discushion.users(email,email_verified_at,created_at,updated_at,privy_user_id,registration_completed_at)
                values(?,?::timestamptz,?::timestamptz,?::timestamptz,?,?::timestamptz) returning id
                """, Long.class, marker + "-other@example.invalid", NOW.toString(), NOW.toString(), NOW.toString(), subject + "-other", NOW.toString());
        long file = photo(100);
        try {
            admin().update("update discushion.media_files set owner_user_id=? where id=?", other, file);
            var denied = request(payload("LOCAL_AGENDA", java.util.List.of(file), null));
            assertThat(denied.statusCode()).isEqualTo(404);
            assertThat(denied.body()).contains("PHOTO_UPLOAD_NOT_FOUND");
            var anonymous = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/posts"))
                    .timeout(Duration.ofSeconds(15)).header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload("LOCAL_AGENDA", java.util.List.of(), null))).build(), HttpResponse.BodyHandlers.ofString());
            assertThat(anonymous.statusCode()).isEqualTo(401);
            assertThat(postCount()).isZero();
        } finally {
            admin().update("delete from discushion.media_files where id=?", file);
            admin().update("delete from discushion.users where id=?", other);
        }
    }

    private Instant databaseNow() { return admin().queryForObject("select clock_timestamp()", java.sql.Timestamp.class).toInstant(); }
    private int postCount() { return admin().queryForObject("select count(*) from discushion.posts where author_user_id=?", Integer.class, user); }
    private long photo(long bytes) {
        return admin().queryForObject("""
                insert into discushion.media_files(owner_user_id,storage_key,original_name,mime_type,size_bytes,purpose,
                    created_at,lifecycle_status,uploaded_at)
                values(?,?,'synthetic.png','image/png',?,'POST_PHOTO',clock_timestamp()-interval '1 minute',
                    'UNLINKED',clock_timestamp()-interval '30 seconds') returning id
                """, Long.class, user, marker + "/" + UUID.randomUUID(), bytes);
    }
    private String payload(String type, java.util.List<Long> photos, Instant endsAt) {
        var body = new java.util.LinkedHashMap<String, Object>();
        body.put("type", type); body.put("topic", "OTHER"); body.put("regionId", region);
        body.put("title", marker); body.put("content", "synthetic body"); body.put("photoFileIds", photos);
        if (endsAt != null) body.put("details", java.util.Map.of("question", marker,
                "options", java.util.List.of("one", "two"), "endsAt", endsAt.toString()));
        return JsonMapper.builder().build().writeValueAsString(body);
    }

    private long created(String body) throws Exception {
        var response = request(body);
        assertThat(response.statusCode()).isEqualTo(201);
        var json = JsonMapper.builder().build().readTree(response.body());
        assertThat(json.path("data").size()).isEqualTo(1);
        assertThat(json.path("data").has("postId")).isTrue();
        return json.path("data").path("postId").asLong();
    }

    private HttpResponse<String> request(String body) throws Exception {
        String token = token();
        return HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/posts"))
                .timeout(Duration.ofSeconds(15)).header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private String token() throws Exception {
        var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.ES256).type(JOSEObjectType.JWT).build(),
                new JWTClaimsSet.Builder().issuer("privy.io").audience("synthetic-post14-app").subject(subject)
                        .issueTime(Date.from(NOW.minusSeconds(60))).expirationTime(Date.from(NOW.plusSeconds(3600)))
                        .claim("sid", "synthetic-post14-session").build());
        jwt.sign(new ECDSASigner(KEY));
        return jwt.serialize();
    }

    private static ECKey key() {
        try { return new ECKeyGenerator(Curve.P_256).generate(); }
        catch (Exception failure) { throw new ExceptionInInitializerError(failure); }
    }
}
