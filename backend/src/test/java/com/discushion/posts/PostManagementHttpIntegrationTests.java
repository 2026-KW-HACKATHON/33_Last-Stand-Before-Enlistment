package com.discushion.posts;

import com.discushion.photos.*;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.*;
import java.net.URI;
import java.net.http.*;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import javax.sql.DataSource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@EnabledIfEnvironmentVariable(named="DISCUSHION_TEST_JDBC_URL",matches="jdbc:postgresql://127\\.0\\.0\\.1:55432/discushion_migration_test(?:\\?.*)?")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
        "spring.profiles.active=management16-http-test","spring.config.import=","PRIVY_APP_ID=synthetic-post14-app",
        "PHOTO_UPLOADS_ENABLED=true","PHOTO_CLEANUP_ENABLED=false",
        "SHARE_TOKEN_SIGNING_KEY=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA","PUBLIC_WEB_BASE_URL=https://example.invalid"})
@Import(PostCreationHttpIntegrationTests.Wiring.class)
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class PostManagementHttpIntegrationTests {
    static final Instant NOW=PostCreationHttpIntegrationTests.NOW;
    static final JsonMapper JSON=JsonMapper.builder().build();
    static JdbcTemplate admin(){return PostCreationHttpIntegrationTests.admin();}
    @AfterAll static void restore(){PostCreationHttpIntegrationTests.restoreServerRole();}
    @LocalServerPort int port;
    @Autowired DataSource source;
    @Autowired PhotoStorage storage;
    @Autowired PlatformTransactionManager manager;
    @Autowired com.discushion.contracts.post.PostSummaryReader summaries;
    String marker; long owner,other,region,target; final List<Long> institutions=new ArrayList<>();
    @BeforeEach void setup(){
        marker="synthetic-management16-"+UUID.randomUUID();
        region=admin().queryForObject("insert into discushion.regions(name) values(?) returning id",Long.class,marker);
        target=admin().queryForObject("insert into discushion.regions(name) values(?) returning id",Long.class,marker+"-target");
        owner=user("owner");other=user("other");
        admin().update("insert into discushion.neighbor_verified_regions(user_id,region_id,verified_at) values(?,?,?)",owner,target,Timestamp.from(NOW));
        reset(storage);when(storage.publicUrl(anyString())).thenAnswer(c->"https://storage.example.invalid/"+c.getArgument(0));
    }
    long user(String suffix){
        long id=admin().queryForObject("insert into discushion.users(email,email_verified_at,created_at,updated_at,privy_user_id,registration_completed_at) values(?,?,?,?,?,?) returning id",Long.class,
                marker+"-"+suffix+"@example.invalid",Timestamp.from(NOW),Timestamp.from(NOW),Timestamp.from(NOW),subject(suffix),Timestamp.from(NOW));
        admin().update("insert into discushion.profiles(user_id,nickname,activity_region_id,updated_at) values(?,?,?,?)",id,"m16"+Long.toString(id,36),region,Timestamp.from(NOW));
        admin().update("insert into discushion.neighbor_verified_regions(user_id,region_id,verified_at) values(?,?,?)",id,region,Timestamp.from(NOW));return id;
    }
    @AfterEach void cleanup(){
        String posts="select id from discushion.posts where author_user_id=?";
        admin().update("delete from discushion.comment_evaluations where comment_id in(select id from discushion.comments where post_id in("+posts+"))",owner);
        for(String table:List.of("comments","post_reactions","bookmarks","post_photos","activity_events","institution_agenda_adoptions"))admin().update("delete from discushion."+table+" where post_id in("+posts+")",owner);
        for(String table:List.of("vote_selections","poll_options"))admin().update("delete from discushion."+table+" where poll_id in(select id from discushion.polls where post_id in("+posts+"))",owner);
        for(String table:List.of("polls","activity_post_details"))admin().update("delete from discushion."+table+" where post_id in("+posts+")",owner);
        admin().update("delete from discushion.posts where author_user_id=?",owner);
        admin().update("delete from discushion.media_files where owner_user_id in(?,?)",owner,other);
        for(long id:new long[]{owner,other}){
            for(String table:List.of("institution_credentials","neighbor_verified_regions","profiles"))admin().update("delete from discushion."+table+" where user_id=?",id);
            admin().update("delete from discushion.users where id=?",id);
        }
        institutions.forEach(id->admin().update("delete from discushion.institutions where id=?",id));institutions.clear();
        admin().update("delete from discushion.regions where id in(?,?)",region,target);
    }
    @Test void patchReturnsRealLatestDetailAndConcurrentPartialEditsPreserveOtherFields() throws Exception {
        long id=create("LOCAL_AGENDA",Map.of(),List.of());
        var changed=patch(id,Map.of("title","새 제목"));assertThat(changed.statusCode()).withFailMessage(changed.body()).isEqualTo(200);
        assertThat(data(changed).path("title").asString()).isEqualTo("새 제목");assertThat(data(changed).path("content").asString()).isEqualTo("본문");
        var executor=Executors.newFixedThreadPool(2);var ready=new CountDownLatch(2);var start=new CountDownLatch(1);
        try{
            var first=executor.submit(()->{ready.countDown();start.await();return patch(id,Map.of("title","동시 제목")).statusCode();});
            var second=executor.submit(()->{ready.countDown();start.await();return patch(id,Map.of("content","동시 본문")).statusCode();});
            assertThat(ready.await(5,TimeUnit.SECONDS)).isTrue();start.countDown();assertThat(first.get(15,TimeUnit.SECONDS)).isEqualTo(200);assertThat(second.get(15,TimeUnit.SECONDS)).isEqualTo(200);
        }finally{start.countDown();executor.shutdownNow();}
        var finalData=data(get(id));assertThat(finalData.path("title").asString()).isEqualTo("동시 제목");assertThat(finalData.path("content").asString()).isEqualTo("동시 본문");
    }
    @Test void activityUpdatesFieldsAndRejectsUnsafeUrlsWithoutPartialSave() throws Exception {
        long id=create("LOCAL_ACTIVITY",Map.of("source","구청","schedule","다음 주","place","주민센터","activityStatus","SCHEDULED","externalParticipationUrl","https://example.invalid/event"),List.of());
        var changed=patch(id,Map.of("details",Map.of("activityStatus","CANCELED","place","공원")));
        assertThat(changed.statusCode()).isEqualTo(200);assertThat(data(changed).path("activity").path("place").asString()).isEqualTo("공원");
        assertThat(data(changed).path("activity").path("source").asString()).isEqualTo("구청");assertThat(data(changed).path("activity").path("externalParticipationEnabled").asBoolean()).isFalse();
        assertThat(patch(id,Map.of("title","실패 제목","details",Map.of("externalParticipationUrl","javascript:alert(1)"))).statusCode()).isEqualTo(400);
        assertThat(data(get(id)).path("title").asString()).isEqualTo(marker);
        var details=new LinkedHashMap<String,Object>();details.put("externalParticipationUrl",null);
        assertThat(patch(id,Map.of("details",details)).statusCode()).isEqualTo(200);assertThat(data(get(id)).path("activity").path("externalParticipationUrl").isNull()).isTrue();
    }
    @Test void voteRejectsPastEndImmutableFieldsAndEndedModificationAndDeletion() throws Exception {
        long id=vote();
        assertThat(patch(id,Map.of("details",Map.of("endsAt",databaseNow().minusSeconds(1).toString()))).statusCode()).isEqualTo(400);
        assertThat(patch(id,Map.of("details",Map.of("question","다른 질문"))).statusCode()).isEqualTo(400);
        assertThat(patch(id,Map.of("regionId",target)).statusCode()).isEqualTo(400);
        assertThat(patch(id,Map.of("details",Map.of("endsAt",databaseNow().plusSeconds(7200).toString()))).statusCode()).isEqualTo(200);
        admin().update("update discushion.polls set ends_at=? where post_id=?",Timestamp.from(NOW.minusSeconds(1)),id);
        assertThat(patch(id,Map.of("title","종료 후 수정")).statusCode()).isEqualTo(403);
        assertThat(call("DELETE",id,"owner",null).statusCode()).isEqualTo(409);
    }
    @Test void rejectsOtherAuthorsMissingRegionAndGuestWrites() throws Exception {
        long id=create("LOCAL_AGENDA",Map.of(),List.of());
        assertThat(call("PATCH",id,"other","{\"title\":\"타인 수정\"}").statusCode()).isEqualTo(403);
        assertThat(call("DELETE",id,"other",null).statusCode()).isEqualTo(403);
        assertThat(call("PATCH",id,null,"{\"title\":\"게스트\"}").statusCode()).isEqualTo(401);
        assertThat(patch(id,Map.of("regionId",9007199254740991L)).statusCode()).isEqualTo(400);
        admin().update("delete from discushion.neighbor_verified_regions where user_id=?",owner);
        assertThat(patch(id,Map.of("title","권한 없음")).statusCode()).isEqualTo(403);assertThat(call("DELETE",id,"owner",null).statusCode()).isEqualTo(403);
    }
    long adopt(long id){
        long institution=admin().queryForObject("insert into discushion.institutions(name,created_at) values(?,?) returning id",Long.class,marker,Timestamp.from(NOW));institutions.add(institution);
        long credential=admin().queryForObject("insert into discushion.institution_credentials(user_id,institution_id,responsible_region_id,completed_at,valid_until) values(?,?,?,?,?) returning id",Long.class,owner,institution,region,Timestamp.from(NOW.minusSeconds(60)),Timestamp.from(NOW.plusSeconds(86400)));
        return admin().queryForObject("insert into discushion.institution_agenda_adoptions(post_id,institution_id,adopted_by_user_id,credential_id,adopted_at) values(?,?,?,?,?) returning id",Long.class,id,institution,owner,credential,Timestamp.from(NOW));
    }
    @Test void activeAdoptionBlocksRegionMoveAndCancellationAllowsVerifiedDestination() throws Exception {
        long id=create("LOCAL_AGENDA",Map.of(),List.of());long adoption=adopt(id);
        assertThat(patch(id,Map.of("regionId",target)).statusCode()).isEqualTo(403);assertThat(data(get(id)).path("region").path("id").asLong()).isEqualTo(region);
        admin().update("update discushion.institution_agenda_adoptions set canceled_at=?,canceled_by_user_id=? where id=?",Timestamp.from(NOW.plusSeconds(1)),owner,adoption);
        assertThat(patch(id,Map.of("regionId",target)).statusCode()).isEqualTo(200);assertThat(data(get(id)).path("region").path("id").asLong()).isEqualTo(target);
    }
    @Test void photoReplacementKeepsPhotoIdAndRecordsDeletionReservationAndFailedReplacementRollsBack() throws Exception {
        long first=photo(100),second=photo(100),next=photo(100);long id=create("LOCAL_AGENDA",Map.of(),List.of(first,second));
        long retained=data(get(id)).path("images").path(1).path("photoId").asLong();
        var changed=patch(id,Map.of("photoOrder",List.of(Map.of("photoId",retained),Map.of("fileId",next))));
        assertThat(changed.statusCode()).withFailMessage(changed.body()).isEqualTo(200);assertThat(data(changed).path("images").path(0).path("photoId").asLong()).isEqualTo(retained);
        assertThat(JSON.readTree(changed.body()).path("meta").path("photoDeletion").path("fileIds").path(0).asLong()).isEqualTo(first);
        assertThat(fileStatus(first)).isEqualTo("DELETE_PENDING");verify(storage,never()).remove(anyString());
        long large=photo(10_000_000);var failed=patch(id,Map.of("title","잘못 저장","photoOrder",List.of(Map.of("photoId",retained),Map.of("fileId",large))));
        assertThat(failed.statusCode()).isEqualTo(413);assertThat(data(get(id)).path("title").asString()).isEqualTo(marker);assertThat(fileStatus(next)).isEqualTo("LINKED");assertThat(fileStatus(large)).isEqualTo("UNLINKED");
    }
    @Test void deleteFailureRollsBackBookmarkPhotoAndPostThenSuccessfulDeleteHidesPreservedRecords() throws Exception {
        long file=photo(100);long id=create("LOCAL_AGENDA",Map.of(),List.of(file));adopt(id);
        var shareResponse=request("GET","/api/v1/posts/"+id+"/share-link","owner",null);
        assertThat(shareResponse.statusCode()).isEqualTo(200);
        String shareToken=URI.create(data(shareResponse).path("shareUrl").asString()).getRawQuery().substring("token=".length());
        admin().update("insert into discushion.bookmarks(post_id,user_id,created_at) values(?,?,?)",id,other,Timestamp.from(NOW));
        admin().update("insert into discushion.comments(post_id,author_user_id,author_kind,content,created_at) values(?,?,'MEMBER','의견',?)",id,other,Timestamp.from(NOW));
        admin().update("insert into discushion.post_reactions(post_id,user_id,reaction_type,created_at) values(?,?,'EMPATHY',?)",id,other,Timestamp.from(NOW));
        admin().update("insert into discushion.activity_events(transition_key,user_id,post_id,event_type,occurred_at) values(?,?,?,'POST_CREATED',?)",marker,owner,id,Timestamp.from(NOW));
        admin().execute("create function discushion.synthetic16_block_delete() returns trigger language plpgsql as $$ begin if NEW.id="+id+" and NEW.status='DELETED' then raise exception 'synthetic failure'; end if; return NEW; end $$");
        admin().execute("create trigger synthetic16_block_delete before update on discushion.posts for each row execute function discushion.synthetic16_block_delete()");
        try{
            assertThat(call("DELETE",id,"owner",null).statusCode()).isEqualTo(500);assertThat(fileStatus(file)).isEqualTo("LINKED");assertThat(count("bookmarks",id)).isEqualTo(1);assertThat(get(id).statusCode()).isEqualTo(200);
        }finally{admin().execute("drop trigger synthetic16_block_delete on discushion.posts");admin().execute("drop function discushion.synthetic16_block_delete()");}
        assertThat(call("DELETE",id,"owner",null).statusCode()).isEqualTo(204);assertThat(get(id).statusCode()).isEqualTo(404);assertThat(count("bookmarks",id)).isZero();assertThat(fileStatus(file)).isEqualTo("DELETE_PENDING");
        for(String table:List.of("comments","post_reactions","activity_events","institution_agenda_adoptions"))assertThat(count(table,id)).as(table).isEqualTo(1);
        assertThat(call("PATCH",id,"owner","{\"title\":\"부활\"}").statusCode()).isEqualTo(404);
        var shared=HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/v1/posts/"+id)).header("X-Post-Share-Token",shareToken).GET().build(),HttpResponse.BodyHandlers.ofString());
        assertThat(shared.statusCode()).isEqualTo(404);
        assertThat(request("POST","/api/v1/posts/"+id+"/comments","owner","{\"content\":\"삭제 후 의견\"}").statusCode()).isEqualTo(404);
        for(String suffix:List.of("/reactions/EMPATHY","/bookmark")) assertThat(request("PUT","/api/v1/posts/"+id+suffix,"owner",null).statusCode()).isEqualTo(404);
    }
    @Test void deletedVotePreservesSelectionsAndStorageFailurePersistsRetryUntilWorkerCompletes() throws Exception {
        long file=photo(100);long id=create("VOTE",Map.of("question","질문","options",List.of("예","아니오"),"endsAt",databaseNow().plusSeconds(7200).toString()),List.of(file));
        long poll=admin().queryForObject("select id from discushion.polls where post_id=?",Long.class,id);
        long option=admin().queryForObject("select id from discushion.poll_options where poll_id=? order by sort_order limit 1",Long.class,poll);
        admin().update("insert into discushion.vote_selections(poll_id,user_id,option_id,first_submitted_at,updated_at) values(?,?,?,?,?)",poll,other,option,Timestamp.from(NOW),Timestamp.from(NOW));
        assertThat(call("DELETE",id,"owner",null).statusCode()).isEqualTo(204);assertThat(get(id).statusCode()).isEqualTo(404);
        assertThat(admin().queryForObject("select count(*) from discushion.vote_selections where poll_id=?",Integer.class,poll)).isEqualTo(1);
        assertThat(admin().queryForObject("select count(*) from discushion.poll_options where poll_id=?",Integer.class,poll)).isEqualTo(2);
        assertThat(request("PUT","/api/v1/posts/"+id+"/vote","other",JSON.writeValueAsString(Map.of("optionId",option,"confirmChange",false))).statusCode()).isEqualTo(404);
        new TransactionTemplate(manager).executeWithoutResult(status->assertThat(summaries.findAll(Set.of(id)).get(id).display()).isEmpty());
        Instant now=databaseNow();doThrow(new RuntimeException("synthetic storage unavailable")).doNothing().when(storage).remove(anyString());when(storage.open(anyString())).thenReturn(Optional.empty());
        var tx=new TransactionTemplate(manager);new PhotoCleanup(new JdbcPhotoStore(source),storage,tx,Clock.fixed(now,ZoneOffset.UTC),Duration.ofMinutes(2)).runBatch(20);
        assertThat(fileStatus(file)).isEqualTo("DELETE_PENDING");assertThat(admin().queryForObject("select last_delete_error_code from discushion.media_files where id=?",String.class,file)).isEqualTo("PHOTO_STORAGE_UNAVAILABLE");
        new PhotoCleanup(new JdbcPhotoStore(source),storage,tx,Clock.fixed(now.plusSeconds(120),ZoneOffset.UTC),Duration.ofMinutes(2)).runBatch(20);
        assertThat(fileStatus(file)).isEqualTo("DELETED");assertThat(admin().queryForObject("select deletion_attempts from discushion.media_files where id=?",Integer.class,file)).isEqualTo(2);
    }
    int count(String table,long id){return admin().queryForObject("select count(*) from discushion."+table+" where post_id=?",Integer.class,id);}
    String fileStatus(long file){return admin().queryForObject("select lifecycle_status from discushion.media_files where id=?",String.class,file);}
    long photo(long bytes){return admin().queryForObject("insert into discushion.media_files(owner_user_id,storage_key,original_name,mime_type,size_bytes,purpose,created_at,lifecycle_status,uploaded_at,upload_transport,upload_authorization_expires_at) values(?,?,'synthetic.png','image/png',?,'POST_PHOTO',clock_timestamp()-interval '1 minute','UNLINKED',clock_timestamp()-interval '30 seconds','SERVER_RELAY',clock_timestamp()-interval '1 minute') returning id",Long.class,owner,marker+"/"+UUID.randomUUID(),bytes);}
    Instant databaseNow(){return admin().queryForObject("select clock_timestamp()",Timestamp.class).toInstant();}
    long vote() throws Exception{return create("VOTE",Map.of("question","질문","options",List.of("예","아니오"),"endsAt",databaseNow().plusSeconds(7200).toString()),List.of());}
    long create(String type,Map<String,Object> details,List<Long> photos) throws Exception{
        var body=new LinkedHashMap<String,Object>();body.put("type",type);body.put("topic","OTHER");body.put("regionId",region);body.put("title",marker);body.put("content","본문");body.put("photoFileIds",photos);if(!details.isEmpty())body.put("details",details);
        var response=request("POST","/api/v1/posts","owner",JSON.writeValueAsString(body));assertThat(response.statusCode()).withFailMessage(response.body()).isEqualTo(201);return JSON.readTree(response.body()).path("data").path("postId").asLong();
    }
    JsonNode data(HttpResponse<String> response){assertThat(response.statusCode()).withFailMessage(response.body()).isEqualTo(200);return JSON.readTree(response.body()).path("data");}
    HttpResponse<String> patch(long id,Map<String,Object> body) throws Exception{return call("PATCH",id,"owner",JSON.writeValueAsString(body));}
    HttpResponse<String> get(long id) throws Exception{return call("GET",id,"owner",null);}
    HttpResponse<String> call(String method,long id,String viewer,String body)throws Exception{return request(method,"/api/v1/posts/"+id,viewer,body);}
    String subject(String suffix){return "did:privy:"+marker+"-"+suffix;}
    HttpResponse<String> request(String method,String path,String viewer,String body)throws Exception{
        var builder=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).timeout(Duration.ofSeconds(20));
        if(viewer!=null){var jwt=new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.ES256).type(JOSEObjectType.JWT).build(),new JWTClaimsSet.Builder().issuer("privy.io").audience("synthetic-post14-app").subject(subject(viewer)).issueTime(Date.from(NOW.minusSeconds(60))).expirationTime(Date.from(NOW.plusSeconds(3600))).claim("sid","synthetic-management16-session").build());jwt.sign(new ECDSASigner(PostCreationHttpIntegrationTests.KEY));builder.header("Authorization","Bearer "+jwt.serialize());}
        if(body!=null)builder.header("Content-Type","application/json");return HttpClient.newHttpClient().send(builder.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
    }
}
