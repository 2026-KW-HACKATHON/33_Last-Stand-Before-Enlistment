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
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;
import static com.discushion.photos.PhotoContentTests.assertReason;
import static com.discushion.photos.PhotoFailure.Reason.*;

/** Production relay HTTP/JWT/server-role adapters and real Storage; no shared application DB or real OTP. */
@EnabledIfEnvironmentVariable(named="DISCUSHION_VERIFY_PHOTO_STORAGE",matches="true")
@EnabledIfEnvironmentVariable(named="DISCUSHION_TEST_JDBC_URL",matches="jdbc:postgresql://127\\.0\\.0\\.1:55432/discushion_migration_test(?:\\?.*)?")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
    "spring.profiles.active=photo-relay-live-test","PRIVY_APP_ID=synthetic-photo-relay-app",
    "PHOTO_UPLOADS_ENABLED=true","PHOTO_STORAGE_WIRE_VERIFIED=false","PHOTO_CLEANUP_ENABLED=false"})
@Import(PhotoRelayLiveIntegrationTests.Wiring.class)
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class PhotoRelayLiveIntegrationTests {
    static final ECKey KEY=key();
    static HikariDataSource runtime;static boolean activated;
    static JdbcTemplate admin(){return new JdbcTemplate(new DriverManagerDataSource(System.getenv("DISCUSHION_TEST_JDBC_URL"),"postgres",System.getenv("DISCUSHION_TEST_DB_PASSWORD")));}
    @TestConfiguration(proxyBeanMethods=false) static class Wiring {
        @Bean VerificationKeySource relayKeys() throws Exception {
            return new PemVerificationKeySource("-----BEGIN PUBLIC KEY-----\n"+Base64.getEncoder().encodeToString(KEY.toECPublicKey().getEncoded())+"\n-----END PUBLIC KEY-----");
        }
        @Bean SupabasePhotoStorage relayStorage(Environment env,Clock clock) {
            if(!"https://pmhmgqpyvrbbseqelpze.supabase.co".equals(env.getRequiredProperty("SUPABASE_URL"))
                || !"discushion-post-photos".equals(env.getRequiredProperty("SUPABASE_STORAGE_BUCKET")))
                throw new IllegalStateException("Only designated Storage is allowed");
            return new SupabasePhotoStorage(URI.create(env.getRequiredProperty("SUPABASE_URL")),env.getRequiredProperty("SUPABASE_SECRET_KEY"),env.getRequiredProperty("SUPABASE_STORAGE_BUCKET"),clock);
        }
        @Bean DataSource relaySource() {
            var jdbc=admin();assertThat(jdbc.queryForObject("select inet_server_addr()='127.0.0.1'::inet and current_database()='discushion_migration_test'",Boolean.class)).isTrue();
            assertThat(jdbc.queryForObject("select not rolcanlogin from pg_roles where rolname='discushion_server'",Boolean.class)).isTrue();
            String password=UUID.randomUUID().toString().replace("-","");jdbc.execute("alter role discushion_server login password '"+password+"'");activated=true;
            try {
                var config=new HikariConfig();config.setJdbcUrl(System.getenv("DISCUSHION_TEST_JDBC_URL"));config.setUsername("discushion_server");config.setPassword(password);config.setMaximumPoolSize(3);config.setConnectionTimeout(5000);
                runtime=new HikariDataSource(config);return runtime;
            } catch(RuntimeException failure){jdbc.execute("alter role discushion_server nologin password null");activated=false;throw failure;}
        }
    }
    @AfterAll static void restore(){try{if(runtime!=null)runtime.close();}finally{if(activated){admin().execute("alter role discushion_server nologin password null");activated=false;}}}
    @LocalServerPort int port;
    @Autowired DataSource source;
    @Autowired SupabasePhotoStorage storage;
    @Autowired PhotoCleanup cleanup;
    final JsonMapper json=JsonMapper.builder().build();
    final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NEVER).build();
    long owner,other;String subject;
    @BeforeEach void prepare() {
        subject="did:privy:synthetic-photo-relay-live-"+UUID.randomUUID();owner=user(subject);other=user(subject+"-other");
        assertThat(new JdbcTemplate(source).queryForObject("select current_user",String.class)).isEqualTo("discushion_server");
    }
    long user(String sub){return admin().queryForObject("""
        insert into discushion.users(email,email_verified_at,created_at,updated_at,privy_user_id,registration_completed_at)
        values(?,clock_timestamp(),clock_timestamp(),clock_timestamp(),?,clock_timestamp()) returning id
        """,Long.class,sub.substring(10)+"@example.invalid",sub);}
    @AfterEach void clean() {
        for(String object:admin().queryForList("select storage_key from discushion.media_files where owner_user_id=?",String.class,owner)) {
            storage.remove(object);assertThat(storage.open(object).isEmpty()).isTrue();
        }
        admin().update("delete from discushion.media_files where owner_user_id in (?,?)",owner,other);
        admin().update("delete from discushion.users where id in (?,?)",owner,other);
    }
    String token(String sub) throws Exception {
        var now=Instant.now();var jwt=new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.ES256).type(JOSEObjectType.JWT).build(),new JWTClaimsSet.Builder().issuer("privy.io").audience("synthetic-photo-relay-app").subject(sub)
            .issueTime(Date.from(now.minusSeconds(10))).expirationTime(Date.from(now.plusSeconds(600))).claim("sid","synthetic-session").build());
        jwt.sign(new ECDSASigner(KEY.toECPrivateKey()));return jwt.serialize();
    }
    HttpResponse<String> api(String method,String path,byte[] bytes,String mime,String sub) throws Exception {
        var request=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/v1/photo-uploads"+path)).timeout(Duration.ofSeconds(90));
        if(sub!=null)request.header("Authorization","Bearer "+token(sub));if(mime!=null)request.header("Content-Type",mime);
        return http.send(request.method(method,bytes==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofByteArray(bytes)).build(),HttpResponse.BodyHandlers.ofString());
    }
    @Test void realRelayUploadReadCompleteCancelFinalDeleteAndLatePutRejection() throws Exception {lifecycle(PhotoContentTests.png());}
    @Test void exactTenMillionBytePngCrossesLocalRelayAndRealStorage() throws Exception {lifecycle(Arrays.copyOf(PhotoContentTests.png(),PhotoContent.MAX_BYTES));}
    void lifecycle(byte[] bytes) throws Exception {
        var response=api("POST","",("{\"originalName\":\"test.png\",\"contentType\":\"image/png\",\"sizeBytes\":"+bytes.length+"}").getBytes(java.nio.charset.StandardCharsets.UTF_8),"application/json",subject);
        assertThat(response.statusCode()).isEqualTo(201);var data=json.readTree(response.body()).get("data");long id=data.get("fileId").asLong();
        assertThat(data.get("upload").get("url").asString()).isEqualTo("/api/v1/photo-uploads/"+id+"/content");
        assertReason(()->storage.createUpload("post-photos/"+owner+"/"+UUID.randomUUID(),"image/png"),PHOTO_STORAGE_UNAVAILABLE);
        assertThat(api("PUT","/"+id+"/content",PhotoContentTests.png(),"image/png",null).statusCode()).isEqualTo(401);
        assertThat(api("PUT","/"+id+"/content",PhotoContentTests.png(),"image/png",subject+"-other").statusCode()).isEqualTo(404);
        assertThat(api("PUT","/"+id+"/content",bytes,"image/png",subject).statusCode()).isEqualTo(200);
        var jdbc=new JdbcTemplate(source);String object=jdbc.queryForObject("select storage_key from discushion.media_files where id=?",String.class,id);
        assertThat(jdbc.queryForObject("select upload_attempt_status from discushion.media_files where id=?",String.class,id)).isEqualTo("ACKNOWLEDGED");
        var publicRead=http.send(HttpRequest.newBuilder(URI.create(storage.publicUrl(object))).timeout(Duration.ofSeconds(60)).GET().build(),HttpResponse.BodyHandlers.ofByteArray());
        assertThat(publicRead.statusCode()).isEqualTo(200);assertThat(publicRead.body()).isEqualTo(bytes);
        assertThat(api("POST","/"+id+"/complete","{}".getBytes(),"application/json",subject).statusCode()).isEqualTo(200);
        assertThat(api("DELETE","/"+id,null,null,subject).statusCode()).isEqualTo(202);
        assertThat(cleanup.process(id)).isTrue();assertThat(storage.open(object).isEmpty()).isTrue();
        var status=api("GET","/"+id,null,null,subject);assertThat(status.statusCode()).isEqualTo(200);
        assertThat(json.readTree(status.body()).get("data").get("deletionCompleted").asBoolean()).isTrue();
        assertThat(api("PUT","/"+id+"/content",PhotoContentTests.png(),"image/png",subject).statusCode()).isEqualTo(409);
        assertThat(storage.open(object).isEmpty()).isTrue();
    }
    static ECKey key(){try{return new ECKeyGenerator(Curve.P_256).generate();}catch(Exception failure){throw new ExceptionInInitializerError(failure);}}
}
