package com.discushion.posts;

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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;

/** #29 uses production post/detail/edit adapters and the actual minimum-privilege role. */
@EnabledIfEnvironmentVariable(named="DISCUSHION_TEST_JDBC_URL",matches="jdbc:postgresql://127\\.0\\.0\\.1:55432/discushion_migration_test(?:\\?.*)?")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
        "spring.profiles.active=adoption29-http-test","spring.config.import=","PRIVY_APP_ID=synthetic-post14-app",
        "PHOTO_UPLOADS_ENABLED=false","PHOTO_CLEANUP_ENABLED=false"})
@Import(PostCreationHttpIntegrationTests.Wiring.class)
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class AdoptionHttpIntegrationTests {
    static final Instant NOW=PostCreationHttpIntegrationTests.NOW;
    static final JsonMapper JSON=JsonMapper.builder().build();
    static JdbcTemplate admin(){return PostCreationHttpIntegrationTests.admin();}
    @AfterAll static void restore(){PostCreationHttpIntegrationTests.restoreServerRole();}
    @LocalServerPort int port;
    @Autowired DataSource source;
    String marker;long region,target,owner,officer,second,colleague,institution,otherInstitution;
    final Map<String,Long> users=new LinkedHashMap<>();
    @BeforeEach void setup(){
        marker="synthetic-adoption29-"+UUID.randomUUID();
        region=admin().queryForObject("insert into discushion.regions(name) values(?) returning id",Long.class,marker);
        target=admin().queryForObject("insert into discushion.regions(name) values(?) returning id",Long.class,marker+"-target");
        owner=user("owner");officer=user("officer");second=user("second");colleague=user("colleague");
        for(long id:new long[]{region,target})admin().update("insert into discushion.neighbor_verified_regions(user_id,region_id,verified_at) values(?,?,?)",owner,id,Timestamp.from(NOW));
        institution=admin().queryForObject("insert into discushion.institutions(name,created_at) values(?,?) returning id",Long.class,marker+"-one",Timestamp.from(NOW));
        otherInstitution=admin().queryForObject("insert into discushion.institutions(name,created_at) values(?,?) returning id",Long.class,marker+"-two",Timestamp.from(NOW));
        credential(officer,institution);credential(colleague,institution);credential(second,otherInstitution);
    }
    long user(String suffix){
        long id=admin().queryForObject("insert into discushion.users(email,email_verified_at,created_at,updated_at,privy_user_id,registration_completed_at) values(?,?,?,?,?,?) returning id",Long.class,
                marker+"-"+suffix+"@example.invalid",Timestamp.from(NOW),Timestamp.from(NOW),Timestamp.from(NOW),subject(suffix),Timestamp.from(NOW));
        users.put(suffix,id);
        admin().update("insert into discushion.profiles(user_id,nickname,activity_region_id,updated_at) values(?,?,?,?)",id,"a29"+Long.toString(id,36),region,Timestamp.from(NOW));return id;
    }
    void credential(long user,long institution){admin().update("insert into discushion.institution_credentials(user_id,institution_id,responsible_region_id,completed_at,valid_until) values(?,?,?,?,?)",user,institution,region,Timestamp.from(NOW.minusSeconds(60)),Timestamp.from(NOW.plusSeconds(86400)));}
    @AfterEach void cleanup(){
        admin().update("delete from discushion.institution_agenda_adoptions where post_id in(select id from discushion.posts where author_user_id=?)",owner);
        admin().update("delete from discushion.activity_post_details where post_id in(select id from discushion.posts where author_user_id=?)",owner);
        admin().update("delete from discushion.posts where author_user_id=?",owner);
        for(long id:users.values()){
            for(String table:List.of("institution_credentials","neighbor_verified_regions","profiles"))admin().update("delete from discushion."+table+" where user_id=?",id);
            admin().update("delete from discushion.users where id=?",id);
        }
        admin().update("delete from discushion.institutions where id in(?,?)",institution,otherInstitution);
        admin().update("delete from discushion.regions where id in(?,?)",region,target);
    }
    @Test void createsAndRetriesSameRelationWithoutChangingPostAndReturnsOnlyPublicAdoptionFields()throws Exception{
        long id=create(region,"LOCAL_AGENDA");var first=adopt(id,"officer");assertThat(first.statusCode()).withFailMessage(first.body()).isEqualTo(201);
        var again=adopt(id,"officer");assertThat(again.statusCode()).isEqualTo(200);assertThat(data(again)).isEqualTo(data(first));assertThat(count(id)).isEqualTo(1);
        var detail=data(request("GET","/api/v1/posts/"+id,"owner",null));assertThat(detail.path("adoptions").size()).isEqualTo(1);
        assertThat(detail.path("adoptions").path(0).size()).isEqualTo(2);assertThat(detail.path("adoptions").path(0).has("institutionName")).isTrue();assertThat(detail.path("adoptions").path(0).has("adoptedAt")).isTrue();
        assertThat(admin().queryForObject("select content_revision from discushion.posts where id=?",Long.class,id)).isEqualTo(1);
        assertThat(new JdbcTemplate(source).queryForObject("select current_user",String.class)).isEqualTo("discushion_server");
        assertThatThrownBy(()->new JdbcTemplate(source).update("delete from discushion.institution_agenda_adoptions where post_id=?",id)).isInstanceOf(org.springframework.dao.DataAccessException.class);
    }
    @Test void institutionsRemainIndependentAndSameInstitutionColleagueCanCancelAndReadoptPreservingHistory()throws Exception{
        long id=create(region,"LOCAL_AGENDA");long first=data(adopt(id,"officer")).path("id").asLong();long other=data(adopt(id,"second")).path("id").asLong();
        assertThat(cancel(id,other,"officer").statusCode()).isEqualTo(404);assertThat(cancel(id,first,"colleague").statusCode()).isEqualTo(204);
        assertThat(cancel(id,first,"officer").statusCode()).isEqualTo(204);assertThat(count(id)).isEqualTo(2);
        assertThat(admin().queryForObject("select canceled_by_user_id from discushion.institution_agenda_adoptions where id=?",Long.class,first)).isEqualTo(colleague);
        assertThat(data(request("GET","/api/v1/posts/"+id,"owner",null)).path("adoptions").size()).isEqualTo(1);
        var reAdopt=adopt(id,"officer");assertThat(reAdopt.statusCode()).isEqualTo(201);assertThat(data(reAdopt).path("id").asLong()).isNotEqualTo(first);assertThat(count(id)).isEqualTo(3);
        var list=request("GET","/api/v1/officer/agendas?scope=ADOPTED","officer",null);assertThat(list.statusCode()).isEqualTo(200);assertThat(data(list).size()).isEqualTo(1);
    }
    @Test void requiresActiveInstitutionMatchingRegionAndAgendaAndRejectsClientSuppliedIdentity()throws Exception{
        long id=create(region,"LOCAL_AGENDA"),elsewhere=create(target,"LOCAL_AGENDA"),activity=create(region,"LOCAL_ACTIVITY");
        assertThat(adopt(id,null).statusCode()).isEqualTo(401);assertThat(adopt(id,"owner").statusCode()).isEqualTo(403);
        assertThat(adopt(elsewhere,"officer").statusCode()).isEqualTo(403);assertThat(adopt(activity,"officer").statusCode()).isEqualTo(403);
        assertThat(request("POST","/api/v1/posts/"+id+"/adoptions","officer","{\"institutionId\":1}").statusCode()).isEqualTo(400);
        assertThat(request("POST","/api/v1/posts/0/adoptions","officer",null).statusCode()).isEqualTo(400);
        admin().update("update discushion.institution_credentials set valid_until=? where user_id=?",Timestamp.from(NOW),officer);
        assertThat(adopt(id,"officer").statusCode()).isEqualTo(403);assertThat(count(id)).isZero();
        admin().update("update discushion.posts set status='DELETED',deleted_at=? where id=?",Timestamp.from(NOW),id);
        assertThat(adopt(id,"second").statusCode()).isEqualTo(404);
    }
    @Test void simultaneousSameInstitutionRequestsCreateOnlyOneRelationAndCancelRetriesAre204()throws Exception{
        long id=create(region,"LOCAL_AGENDA");var statuses=parallel(()->adopt(id,"officer").statusCode(),()->adopt(id,"colleague").statusCode());
        assertThat(statuses).containsExactlyInAnyOrder(201,200);assertThat(count(id)).isEqualTo(1);
        long adoption=admin().queryForObject("select id from discushion.institution_agenda_adoptions where post_id=?",Long.class,id);
        assertThat(parallel(()->cancel(id,adoption,"officer").statusCode(),()->cancel(id,adoption,"colleague").statusCode())).containsOnly(204);
        assertThat(count(id)).isEqualTo(1);assertThat(admin().queryForObject("select canceled_at is not null from discushion.institution_agenda_adoptions where id=?",Boolean.class,adoption)).isTrue();
    }
    @Test void adoptionAndRegionEditSerializeSoAnActiveAdoptionNeverMovesToAnotherRegion()throws Exception{
        long id=create(region,"LOCAL_AGENDA");var statuses=parallel(()->adopt(id,"officer").statusCode(),()->request("PATCH","/api/v1/posts/"+id,"owner",JSON.writeValueAsString(Map.of("regionId",target))).statusCode());
        if(statuses.get(0)==201){assertThat(statuses.get(1)).isEqualTo(403);assertThat(postRegion(id)).isEqualTo(region);assertThat(count(id)).isEqualTo(1);}
        else{assertThat(statuses).containsExactly(403,200);assertThat(postRegion(id)).isEqualTo(target);assertThat(count(id)).isZero();}
        if(count(id)==1){long adoption=admin().queryForObject("select id from discushion.institution_agenda_adoptions where post_id=?",Long.class,id);assertThat(cancel(id,adoption,"officer").statusCode()).isEqualTo(204);assertThat(request("PATCH","/api/v1/posts/"+id,"owner",JSON.writeValueAsString(Map.of("regionId",target))).statusCode()).isEqualTo(200);}
    }
    @Test void postDeletionAndAdoptionSerializeAndDeletionPreservesAuditWithoutCancelEvent()throws Exception{
        long id=create(region,"LOCAL_AGENDA");var statuses=parallel(()->adopt(id,"officer").statusCode(),()->request("DELETE","/api/v1/posts/"+id,"owner",null).statusCode());
        assertThat(statuses.get(1)).isEqualTo(204);assertThat(statuses.get(0)).isIn(201,404);
        assertThat(request("GET","/api/v1/posts/"+id,"owner",null).statusCode()).isEqualTo(404);
        assertThat(data(request("GET","/api/v1/officer/agendas?scope=ADOPTED","officer",null)).size()).isZero();
        if(statuses.get(0)==201){assertThat(count(id)).isEqualTo(1);assertThat(admin().queryForObject("select canceled_at is null from discushion.institution_agenda_adoptions where post_id=?",Boolean.class,id)).isTrue();}
        else assertThat(count(id)).isZero();
    }
    @Test void databaseFailureRollsBackCreateAndCancelAndDoesNotChangePost()throws Exception{
        long id=create(region,"LOCAL_AGENDA");
        admin().execute("create function discushion.synthetic29_failure() returns trigger language plpgsql as $$ begin if NEW.post_id="+id+" then raise exception 'synthetic rollback'; end if; return NEW; end $$");
        admin().execute("create trigger synthetic29_failure before insert on discushion.institution_agenda_adoptions for each row execute function discushion.synthetic29_failure()");
        try{var failure=adopt(id,"officer");assertThat(failure.statusCode()).isEqualTo(500);assertThat(failure.body()).contains("INTERNAL_ERROR").doesNotContain("synthetic rollback","discushion.");assertThat(count(id)).isZero();}
        finally{admin().execute("drop trigger synthetic29_failure on discushion.institution_agenda_adoptions");admin().execute("drop function discushion.synthetic29_failure()");}
        long adoption=data(adopt(id,"officer")).path("id").asLong();
        admin().execute("create function discushion.synthetic29_failure() returns trigger language plpgsql as $$ begin if NEW.post_id="+id+" then raise exception 'synthetic rollback'; end if; return NEW; end $$");
        admin().execute("create trigger synthetic29_failure before update on discushion.institution_agenda_adoptions for each row execute function discushion.synthetic29_failure()");
        try{assertThat(cancel(id,adoption,"officer").statusCode()).isEqualTo(500);assertThat(admin().queryForObject("select canceled_at is null from discushion.institution_agenda_adoptions where id=?",Boolean.class,adoption)).isTrue();}
        finally{admin().execute("drop trigger synthetic29_failure on discushion.institution_agenda_adoptions");admin().execute("drop function discushion.synthetic29_failure()");}
        assertThat(admin().queryForObject("select status from discushion.posts where id=?",String.class,id)).isEqualTo("PUBLISHED");
    }
    List<Integer> parallel(Callable<Integer> first,Callable<Integer> second)throws Exception{
        var executor=Executors.newFixedThreadPool(2);var ready=new CountDownLatch(2);var start=new CountDownLatch(1);
        try{var a=executor.submit(()->{ready.countDown();start.await();return first.call();});var b=executor.submit(()->{ready.countDown();start.await();return second.call();});assertThat(ready.await(5,TimeUnit.SECONDS)).isTrue();start.countDown();return List.of(a.get(20,TimeUnit.SECONDS),b.get(20,TimeUnit.SECONDS));}
        finally{start.countDown();executor.shutdownNow();}
    }
    long postRegion(long id){return admin().queryForObject("select region_id from discushion.posts where id=?",Long.class,id);}
    int count(long id){return admin().queryForObject("select count(*) from discushion.institution_agenda_adoptions where post_id=?",Integer.class,id);}
    long create(long region,String type)throws Exception{
        var body=new LinkedHashMap<String,Object>();body.put("type",type);body.put("topic","OTHER");body.put("regionId",region);body.put("title",marker);body.put("content","본문");
        if(type.equals("LOCAL_ACTIVITY"))body.put("details",Map.of("source","기관","schedule","다음 주","place","주민센터","activityStatus","SCHEDULED"));
        var result=request("POST","/api/v1/posts","owner",JSON.writeValueAsString(body));assertThat(result.statusCode()).withFailMessage(result.body()).isEqualTo(201);return data(result).path("postId").asLong();
    }
    HttpResponse<String> adopt(long id,String viewer)throws Exception{return request("POST","/api/v1/posts/"+id+"/adoptions",viewer,null);}
    HttpResponse<String> cancel(long id,long adoption,String viewer)throws Exception{return request("DELETE","/api/v1/posts/"+id+"/adoptions/"+adoption,viewer,null);}
    JsonNode data(HttpResponse<String> result){assertThat(result.statusCode()).withFailMessage(result.body()).isIn(200,201);return JSON.readTree(result.body()).path("data");}
    String subject(String suffix){return "did:privy:"+marker+"-"+suffix;}
    HttpResponse<String> request(String method,String path,String viewer,String body)throws Exception{
        var request=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).timeout(Duration.ofSeconds(20));
        if(viewer!=null){var token=new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.ES256).type(JOSEObjectType.JWT).build(),new JWTClaimsSet.Builder().issuer("privy.io").audience("synthetic-post14-app").subject(subject(viewer)).issueTime(Date.from(NOW.minusSeconds(60))).expirationTime(Date.from(NOW.plusSeconds(3600))).claim("sid","synthetic-adoption29-session").build());token.sign(new ECDSASigner(PostCreationHttpIntegrationTests.KEY));request.header("Authorization","Bearer "+token.serialize());}
        if(body!=null)request.header("Content-Type","application/json");return HttpClient.newHttpClient().send(request.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
    }
}
