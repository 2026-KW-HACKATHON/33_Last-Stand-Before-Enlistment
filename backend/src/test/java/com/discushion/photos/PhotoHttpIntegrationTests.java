package com.discushion.photos;

import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.util.*;
import javax.sql.DataSource;
import com.discushion.contracts.identity.CurrentActorProvider;
import com.discushion.identity.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import static org.assertj.core.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="DISCUSHION_TEST_JDBC_URL",matches="jdbc:postgresql://127\\.0\\.0\\.1:55432/discushion_migration_test(?:\\?.*)?")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
    "spring.profiles.active=local","spring.config.import=","PHOTO_UPLOADS_ENABLED=true"})
@Import(PhotoHttpIntegrationTests.Wiring.class)
class PhotoHttpIntegrationTests {
    @LocalServerPort int port;
    @Autowired DataSource source;
    @Autowired FakeStorage storage;
    private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private String subject;
    private long owner;
    @TestConfiguration(proxyBeanMethods=false)
    static class Wiring {
        @Bean DataSource photoTestSource() {return new DriverManagerDataSource(System.getenv("DISCUSHION_TEST_JDBC_URL"),"postgres",System.getenv("DISCUSHION_TEST_DB_PASSWORD"));}
        @Bean JdbcMemberStore photoTestMembers(DataSource source,Clock clock) {return new JdbcMemberStore(source,clock);}
        @Bean MemberAuthorization photoTestAuthorization(CurrentActorProvider actors,JdbcMemberStore members,Clock clock) {return new MemberAuthorization(actors,members,clock);}
        @Bean @Primary AccessTokenVerifier photoTestTokens() {return token->{
            if(!token.startsWith("synthetic-photo13-")) throw new IdentityFailure(IdentityFailure.Reason.INVALID_TOKEN);
            return "did:privy:"+token;
        };}
        @Bean FakeStorage photoTestStorage(Clock clock) {return new FakeStorage(clock);}
    }
    static class FakeStorage implements PhotoStorage {
        final Map<String,byte[]> objects=new java.util.concurrent.ConcurrentHashMap<>(); final Clock clock;
        FakeStorage(Clock clock){this.clock=clock;}
        public Instant authorizationUpperBound(Instant now){return now.plusSeconds(7300);}
        public Upload createUpload(String key,String mime){return new Upload("https://example.invalid/upload","PUT","RAW",Map.of("Content-Type",mime,"x-upsert","false"),clock.instant().plusSeconds(7200));}
        public Optional<InputStream> open(String key){return Optional.ofNullable(objects.get(key)).map(ByteArrayInputStream::new);}
        public void remove(String key){objects.remove(key);}
        public String publicUrl(String key){return "https://example.invalid/"+key;}
        public boolean uploadsDrained(String key,Instant expiry){return false;}
    }
    @BeforeEach void prepare() {
        subject="synthetic-photo13-"+UUID.randomUUID(); var jdbc=new JdbcTemplate(source); String now=Instant.now().minusSeconds(1).toString();
        owner=jdbc.queryForObject("""
            insert into discushion.users(email,email_verified_at,created_at,updated_at,privy_user_id,registration_completed_at)
            values(?,?::timestamptz,?::timestamptz,?::timestamptz,?,?::timestamptz) returning id
            """,Long.class,subject+"@example.invalid",now,now,now,"did:privy:"+subject,now);
    }
    @AfterEach void removeFixtures() {
        var jdbc=new JdbcTemplate(source);jdbc.update("delete from discushion.media_files where owner_user_id=?",owner);
        jdbc.update("delete from discushion.users where id=? and privy_user_id=?",owner,"did:privy:"+subject);storage.objects.clear();
    }
    private HttpResponse<String> request(String method,String path,String body,String token) throws Exception {
        var request=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/v1/photo-uploads"+path)).timeout(Duration.ofSeconds(10));
        if(token!=null) request.header("Authorization","Bearer "+token);
        if(body!=null) request.header("Content-Type","application/json");
        return http.send(request.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
    }
    @Test void anonymousAndInvalidTokenNeverGetMemberPhotoRoute() throws Exception {
        assertThat(request("GET","/1",null,null).statusCode()).isEqualTo(401);
        assertThat(request("GET","/1",null,"invalid").statusCode()).isEqualTo(401);
    }
    @Test void exactJsonFieldsRejectOwnerRegionAndNonintegerSize() throws Exception {
        for(String body:List.of("{\"originalName\":\"a.png\",\"contentType\":\"image/png\",\"sizeBytes\":1,\"userId\":99}",
            "{\"originalName\":\"a.png\",\"contentType\":\"image/png\",\"sizeBytes\":1.5}",
            "{\"originalName\":\"a.png\",\"contentType\":\"image/png\",\"sizeBytes\":1,\"regionId\":1}")) {
            assertThat(request("POST","",body,subject).statusCode()).isEqualTo(400);
        }
    }
    @Test void reserveCompleteGetCancelFollowWireWithoutClaimingFinalDeletion() throws Exception {
        var result=request("POST","","{\"originalName\":\"a.png\",\"contentType\":\"image/png\",\"sizeBytes\":1000}",subject);
        assertThat(result.statusCode()).isEqualTo(201); assertThat(result.body()).contains("\"bodyMode\":\"RAW\"","\"method\":\"PUT\"");
        var row=new JdbcTemplate(source).queryForMap("select id,storage_key from discushion.media_files where owner_user_id=?",owner);
        long id=((Number)row.get("id")).longValue();storage.objects.put((String)row.get("storage_key"),PhotoContentTests.png());
        assertThat(request("POST","/"+id+"/complete","{}",subject).statusCode()).isEqualTo(200);
        assertThat(request("GET","/"+id,null,subject).body()).contains("\"canAttach\":true");
        assertThat(request("POST","/"+id+"/complete","{\"sizeBytes\":1}",subject).statusCode()).isEqualTo(400);
        var cancelled=request("DELETE","/"+id,null,subject);
        assertThat(cancelled.statusCode()).isEqualTo(202); assertThat(cancelled.body()).contains("DELETE_PENDING","\"deletionCompleted\":false");
        assertThat(request("POST","/"+id+"/complete","{}",subject).statusCode()).isEqualTo(409);
    }
    @Test void missingAndOtherOwnersFilesShare404Envelope() throws Exception {
        request("POST","","{\"originalName\":\"a.png\",\"contentType\":\"image/png\",\"sizeBytes\":1000}",subject);
        long id=new JdbcTemplate(source).queryForObject("select id from discushion.media_files where owner_user_id=?",Long.class,owner);
        // An authenticated but unregistered identity must not gain access to the owner's upload.
        assertThat(request("GET","/"+id,null,"synthetic-photo13-other").statusCode()).isEqualTo(403);
        var missing=request("GET","/9007199254740991",null,subject);
        assertThat(missing.statusCode()).isEqualTo(404); assertThat(missing.body()).contains("PHOTO_UPLOAD_NOT_FOUND","details","traceId");
    }
}
