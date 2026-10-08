package com.discushion.photos;

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
import javax.sql.DataSource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.annotation.DirtiesContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;

/** Real Storage, production HTTP/JWT/member adapters, isolated server LOGIN. Never touches shared application DB. */
@EnabledIfEnvironmentVariable(named="DISCUSHION_VERIFY_PHOTO_STORAGE",matches="true")
@EnabledIfEnvironmentVariable(named="DISCUSHION_TEST_JDBC_URL",matches="jdbc:postgresql://127\\.0\\.0\\.1:55432/discushion_migration_test(?:\\?.*)?")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
    "spring.profiles.active=photo-storage-live-test","PRIVY_APP_ID=synthetic-photo-live-app",
    "PHOTO_UPLOADS_ENABLED=true","PHOTO_STORAGE_WIRE_VERIFIED=false","PHOTO_CLEANUP_ENABLED=false"})
@Import(PhotoStorageLiveIntegrationTests.Wiring.class)
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class PhotoStorageLiveIntegrationTests {
    private static final ECKey KEY=key();
    private static HikariDataSource runtime;
    private static boolean activated;
    private static JdbcTemplate admin() {
        return new JdbcTemplate(new DriverManagerDataSource(System.getenv("DISCUSHION_TEST_JDBC_URL"),"postgres",System.getenv("DISCUSHION_TEST_DB_PASSWORD")));
    }
    static final class FixtureClock extends Clock {
        volatile Instant now=Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
        public ZoneId getZone(){return ZoneOffset.UTC;}
        public Clock withZone(ZoneId zone){return Clock.fixed(now,zone);}
        public Instant instant(){return now;}
    }
    @TestConfiguration(proxyBeanMethods=false) static class Wiring {
        @Bean @Primary PhotoService legacyLiveService(JdbcPhotoStore store,MemberAuthorization auth,SupabasePhotoStorage storage,
                org.springframework.transaction.PlatformTransactionManager manager,FixtureClock clock) {
            return new PhotoService(store,auth,storage,new org.springframework.transaction.support.TransactionTemplate(manager),clock);
        }
        @Bean @Primary PhotoCleanup legacyLiveCleanup(JdbcPhotoStore store,SupabasePhotoStorage storage,
                org.springframework.transaction.PlatformTransactionManager manager,FixtureClock clock) {
            return new PhotoCleanup(store,storage,new org.springframework.transaction.support.TransactionTemplate(manager),clock,Duration.ofMinutes(2));
        }
        @Bean @Primary FixtureClock liveClock(){return new FixtureClock();}
        @Bean VerificationKeySource liveKeys() throws Exception {
            return new PemVerificationKeySource("-----BEGIN PUBLIC KEY-----\n"+Base64.getEncoder().encodeToString(KEY.toECPublicKey().getEncoded())+"\n-----END PUBLIC KEY-----");
        }
        @Bean SupabasePhotoStorage liveStorage(Environment env,FixtureClock clock) {
            // Fail closed if ignored local credentials point to a different project/bucket.
            if(!"https://pmhmgqpyvrbbseqelpze.supabase.co".equals(env.getRequiredProperty("SUPABASE_URL"))
                || !"discushion-post-photos".equals(env.getRequiredProperty("SUPABASE_STORAGE_BUCKET")))
                throw new IllegalStateException("Only the designated development Storage may be verified");
            // Test-only bound; this is NOT evidence for an operational issuance/drain guarantee.
            return new SupabasePhotoStorage(URI.create(env.getRequiredProperty("SUPABASE_URL")),
                env.getRequiredProperty("SUPABASE_SECRET_KEY"),env.getRequiredProperty("SUPABASE_STORAGE_BUCKET"),Duration.ofMinutes(1),clock);
        }
        @Bean DataSource liveDataSource() {
            var setup=admin();
            assertThat(setup.queryForObject("select inet_server_addr()='127.0.0.1'::inet and current_database()='discushion_migration_test'",Boolean.class)).isTrue();
            assertThat(setup.queryForObject("select not rolcanlogin from pg_roles where rolname='discushion_server'",Boolean.class)).isTrue();
            String password=UUID.randomUUID().toString().replace("-","");
            setup.execute("alter role discushion_server login password '"+password+"'");activated=true;
            try {
                var config=new HikariConfig();config.setJdbcUrl(System.getenv("DISCUSHION_TEST_JDBC_URL"));
                config.setUsername("discushion_server");config.setPassword(password);config.setMaximumPoolSize(2);config.setConnectionTimeout(5000);
                runtime=new HikariDataSource(config);return runtime;
            } catch(RuntimeException failure) {
                setup.execute("alter role discushion_server nologin password null");activated=false;throw failure;
            }
        }
    }
    @AfterAll static void restoreRole() {
        try {if(runtime!=null) runtime.close();}
        finally {if(activated){admin().execute("alter role discushion_server nologin password null");activated=false;}}
    }
    @LocalServerPort int port;
    @Autowired DataSource source;
    @Autowired FixtureClock clock;
    @Autowired SupabasePhotoStorage storage;
    @Autowired JdbcPhotoStore store;
    @Autowired PhotoCleanup cleanup;
    private final JsonMapper json=JsonMapper.builder().build();
    private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private long owner,other;
    private String subject;
    @BeforeEach void prepare() {
        clock.now=Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS);subject="did:privy:synthetic-photo-live-"+UUID.randomUUID();
        owner=user(subject);other=user(subject+"-other");
        assertThat(new JdbcTemplate(source).queryForObject("select current_user",String.class)).isEqualTo("discushion_server");
    }
    private long user(String sub) {
        return admin().queryForObject("""
            insert into discushion.users(email,email_verified_at,created_at,updated_at,privy_user_id,registration_completed_at)
            values(?,?::timestamptz,?::timestamptz,?::timestamptz,?,?::timestamptz) returning id
            """,Long.class,sub.substring(10)+"@example.invalid",clock.now.toString(),clock.now.toString(),clock.now.toString(),sub,clock.now.toString());
    }
    @AfterEach void removeOnlyOwnFixtures() {
        // Record keys from committed reservations even when issuance/the HTTP response fails.
        // If remote cleanup fails, preserve DB tracking and fail visibly instead of losing the key.
        for(String object:admin().queryForList("select storage_key from discushion.media_files where owner_user_id=?",String.class,owner)) {
            storage.remove(object);assertThat(storage.open(object).isEmpty()).as("Synthetic Storage fixture removed").isTrue();
        }
        admin().update("delete from discushion.media_files where owner_user_id=?",owner);
        for(long id:new long[]{owner,other}) admin().update("delete from discushion.users where id=?",id);
    }
    private String token(String sub) throws Exception {
        var jwt=new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.ES256).type(JOSEObjectType.JWT).build(),
            new JWTClaimsSet.Builder().issuer("privy.io").audience("synthetic-photo-live-app").subject(sub)
                .issueTime(Date.from(clock.now.minusSeconds(60))).expirationTime(Date.from(clock.now.plusSeconds(120))).claim("sid","synthetic-session").build());
        jwt.sign(new ECDSASigner(KEY));return jwt.serialize();
    }
    private HttpResponse<byte[]> send(HttpRequest request) throws Exception {
        try {return http.send(request,HttpResponse.BodyHandlers.ofByteArray());}
        catch(java.io.IOException failure) {throw new AssertionError("Live verification transport failed; signed URL redacted");}
    }
    private HttpResponse<byte[]> request(String method,String path,String body,String sub) throws Exception {
        var builder=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/v1/photo-uploads"+path)).timeout(Duration.ofSeconds(35));
        if(sub!=null) builder.header("Authorization","Bearer "+token(sub));
        if(body!=null) builder.header("Content-Type","application/json");
        return send(builder.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body)).build());
    }
    private JsonNode data(HttpResponse<byte[]> response,int status) {
        assertThat(response.statusCode()).as("App HTTP status (body omitted to protect signed URLs)").isEqualTo(status);
        return json.readTree(response.body()).get("data");
    }
    private JsonNode reserve() throws Exception {
        return data(request("POST","","{\"originalName\":\"synthetic.png\",\"contentType\":\"image/png\",\"sizeBytes\":"+PhotoContentTests.png().length+"}",subject),201);
    }
    private void put(JsonNode reservation) throws Exception {
        var upload=reservation.get("upload");
        assertThat(upload.get("method").asString()).isEqualTo("PUT");assertThat(upload.get("bodyMode").asString()).isEqualTo("RAW");
        var builder=HttpRequest.newBuilder(URI.create(upload.get("url").asString())).timeout(Duration.ofSeconds(35));
        upload.get("headers").properties().forEach(entry->builder.header(entry.getKey(),entry.getValue().asString()));
        assertThat(send(builder.PUT(HttpRequest.BodyPublishers.ofByteArray(PhotoContentTests.png())).build()).statusCode()).isEqualTo(200);
    }
    private String key(long id){return admin().queryForObject("select storage_key from discushion.media_files where id=? and owner_user_id=?",String.class,id,owner);}
    private void pending(long id) throws Exception {
        var view=data(request("GET","/"+id,null,subject),200);
        assertThat(view.get("status").asString()).isEqualTo("DELETE_PENDING");
        assertThat(view.get("deletionCompleted").asBoolean()).isFalse();assertThat(view.get("url").isNull()).isTrue();
        assertThat(admin().queryForObject("select last_delete_error_code from discushion.media_files where id=?",String.class,id)).isEqualTo("UPLOAD_DRAIN_UNCONFIRMED");
    }
    @Test void realUploadPublicReadCompletionAndLateReuploadRemainSafelyPending() throws Exception {
        assertThat(request("POST","","{\"originalName\":\"synthetic.png\",\"contentType\":\"image/png\",\"sizeBytes\":1}",null).statusCode()).isEqualTo(401);
        var reservation=reserve();long id=reservation.get("fileId").asLong();String object=key(id);
        put(reservation);
        // Public Storage read is anonymous, including before /complete.
        var read=send(HttpRequest.newBuilder(URI.create(storage.publicUrl(object)+"?fixture="+UUID.randomUUID())).timeout(Duration.ofSeconds(35)).GET().build());
        assertThat(read.statusCode()).isEqualTo(200);assertThat(read.body()).isEqualTo(PhotoContentTests.png());
        assertThat(request("GET","/"+id,null,subject+"-other").statusCode()).isEqualTo(404);
        var complete=data(request("POST","/"+id+"/complete","{}",subject),200);
        assertThat(complete.get("status").asString()).isEqualTo("UNLINKED");assertThat(complete.get("canAttach").asBoolean()).isTrue();
        assertThat(complete.get("sizeBytes").asLong()).isEqualTo(PhotoContentTests.png().length);
        data(request("DELETE","/"+id,null,subject),202);
        assertThat(cleanup.process(id)).isTrue();assertThat(storage.open(object).isEmpty()).isTrue();pending(id);
        // The same valid capability recreates the deleted object; no new capability is issued.
        put(reservation);assertThat(storage.open(object).isPresent()).isTrue();
        clock.now=clock.now.plusSeconds(61);
        assertThat(cleanup.process(id)).isTrue();assertThat(storage.open(object).isEmpty()).isTrue();pending(id);
        assertThat(request("POST","/"+id+"/complete","{}",subject).statusCode()).isEqualTo(409);
    }
    @Test void realObjectsExpireAtOriginalReservationOrFirstCompletionPlus24Hours() throws Exception {
        var unfinished=reserve();long first=unfinished.get("fileId").asLong();put(unfinished);
        var uploaded=reserve();long second=uploaded.get("fileId").asLong();put(uploaded);
        Instant reservedAt=clock.now;clock.now=reservedAt.plusSeconds(3600);
        var complete=data(request("POST","/"+second+"/complete","{}",subject),200);
        String firstCompletion=complete.get("uploadedAt").asString();
        clock.now=clock.now.plusSeconds(5);
        assertThat(data(request("POST","/"+second+"/complete","{}",subject),200).get("uploadedAt").asString()).isEqualTo(firstCompletion);
        clock.now=reservedAt.plusSeconds(86399);
        assertThat(cleanup.process(first)).isFalse();assertThat(cleanup.process(second)).isFalse();
        clock.now=reservedAt.plusSeconds(86400);
        assertThat(cleanup.process(first)).isTrue();assertThat(storage.open(key(first)).isEmpty()).isTrue();pending(first);
        assertThat(cleanup.process(second)).isFalse();
        clock.now=reservedAt.plusSeconds(90000);
        assertThat(cleanup.process(second)).isTrue();assertThat(storage.open(key(second)).isEmpty()).isTrue();pending(second);
        // A moved test clock does not revoke a provider token or prove in-flight drain.
        assertThat(store.candidates(clock.now.plusSeconds(3600),100)).contains(first,second);
    }
    private static ECKey key(){try{return new ECKeyGenerator(Curve.P_256).generate();}catch(Exception error){throw new ExceptionInInitializerError(error);}}
}
