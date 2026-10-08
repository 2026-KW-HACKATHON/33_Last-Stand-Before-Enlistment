package com.discushion.posts;

import com.discushion.contracts.post.PostSummaryReader;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.*;
import java.net.URI;
import java.net.http.*;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sql.DataSource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="DISCUSHION_TEST_JDBC_URL",matches="jdbc:postgresql://127\\.0\\.0\\.1:55432/discushion_migration_test(?:\\?.*)?")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
        "spring.profiles.active=personal27-http-test","spring.config.import=","PRIVY_APP_ID=synthetic-post14-app",
        "PHOTO_UPLOADS_ENABLED=false","PHOTO_CLEANUP_ENABLED=false"})
@Import({PostCreationHttpIntegrationTests.Wiring.class,PersonalHttpIntegrationTests.SnapshotProbe.class})
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class PersonalHttpIntegrationTests {
    static final Instant NOW=PostCreationHttpIntegrationTests.NOW;
    static final JsonMapper JSON=JsonMapper.builder().build();
    static JdbcTemplate admin(){return PostCreationHttpIntegrationTests.admin();}
    @AfterAll static void restore(){PostCreationHttpIntegrationTests.restoreServerRole();}
    @LocalServerPort int port;
    @Autowired DataSource source;
    String marker;long region,owner,viewer,other;
    final Map<String,Long> users=new LinkedHashMap<>();
    @BeforeEach void setup(){
        SnapshotProbe.pause.set(false);marker="synthetic-personal27-"+UUID.randomUUID();
        region=admin().queryForObject("insert into discushion.regions(name) values(?) returning id",Long.class,marker);
        owner=user("owner");viewer=user("viewer");other=user("other");
        for(long id:users.values())admin().update("insert into discushion.neighbor_verified_regions(user_id,region_id,verified_at) values(?,?,?)",id,region,Timestamp.from(NOW));
    }
    long user(String suffix){
        long id=admin().queryForObject("insert into discushion.users(email,email_verified_at,created_at,updated_at,privy_user_id,registration_completed_at) values(?,?,?,?,?,?) returning id",Long.class,
                marker+"-"+suffix+"@example.invalid",Timestamp.from(NOW),Timestamp.from(NOW),Timestamp.from(NOW),subject(suffix),Timestamp.from(NOW));users.put(suffix,id);
        admin().update("insert into discushion.profiles(user_id,nickname,activity_region_id,updated_at) values(?,?,?,?)",id,"p27"+Long.toString(id,36),region,Timestamp.from(NOW));return id;
    }
    @AfterEach void cleanup(){
        SnapshotProbe.pause.set(false);SnapshotProbe.release.countDown();
        for(String table:List.of("comment_evaluations","post_reactions","bookmarks")){
            if(table.equals("comment_evaluations"))admin().update("delete from discushion.comment_evaluations where comment_id in(select c.id from discushion.comments c join discushion.posts p on p.id=c.post_id where p.region_id=?)",region);
            else admin().update("delete from discushion."+table+" where post_id in(select id from discushion.posts where region_id=?)",region);
        }
        admin().update("delete from discushion.comments where post_id in(select id from discushion.posts where region_id=?)",region);
        admin().update("delete from discushion.vote_selections where poll_id in(select poll.id from discushion.polls poll join discushion.posts p on p.id=poll.post_id where p.region_id=?)",region);
        admin().update("delete from discushion.poll_options where poll_id in(select poll.id from discushion.polls poll join discushion.posts p on p.id=poll.post_id where p.region_id=?)",region);
        admin().update("delete from discushion.polls where post_id in(select id from discushion.posts where region_id=?)",region);
        admin().update("delete from discushion.activity_post_details where post_id in(select id from discushion.posts where region_id=?)",region);
        admin().update("delete from discushion.posts where region_id=?",region);
        for(long id:users.values()){admin().update("delete from discushion.neighbor_verified_regions where user_id=?",id);admin().update("delete from discushion.profiles where user_id=?",id);admin().update("delete from discushion.users where id=?",id);}
        admin().update("delete from discushion.regions where id=?",region);
    }
    @Test void ownPostsAreScopedTypedPagedAndCapabilitiesMatchActualRights()throws Exception{
        long agenda=create("owner","LOCAL_AGENDA"),activity=create("owner","LOCAL_ACTIVITY"),vote=create("owner","VOTE");create("other","LOCAL_AGENDA");
        var first=json(get("posts?size=2","owner"));assertThat(first.path("data").size()).isEqualTo(2);
        assertThat(first.path("data").path(0).path("id").asLong()).isEqualTo(vote);assertThat(first.path("data").path(1).path("id").asLong()).isEqualTo(activity);
        assertThat(first.path("meta").path("hasNext").asBoolean()).isTrue();String cursor=first.path("meta").path("nextCursor").asText();
        var last=json(get("posts?size=2&cursor="+cursor,"owner"));assertThat(last.path("data").size()).isEqualTo(1);assertThat(last.path("data").path(0).path("id").asLong()).isEqualTo(agenda);assertThat(last.path("meta").path("hasNext").asBoolean()).isFalse();
        assertThat(json(get("posts?type=LOCAL_ACTIVITY","owner")).path("data").size()).isEqualTo(1);
        assertThat(json(get("posts","viewer")).path("data").isEmpty()).isTrue();
        assertThat(first.path("data").path(0).path("capabilities").path("canEdit").asBoolean()).isTrue();
        admin().update("update discushion.polls set ends_at=? where post_id=?",Timestamp.from(NOW),vote);
        assertThat(json(get("posts?type=VOTE","owner")).path("data").path(0).path("capabilities").path("canDelete").asBoolean()).isFalse();
        admin().update("delete from discushion.neighbor_verified_regions where user_id=?",owner);
        assertThat(json(get("posts?type=LOCAL_AGENDA","owner")).path("data").path(0).path("capabilities").path("canEdit").asBoolean()).isFalse();
    }
    @Test void combinesAllOwnActionsOnceAndExcludesBookmarkOnlyAuthorshipAndOtherActors()throws Exception{
        long id=create("owner","VOTE"),bookmark=create("owner","LOCAL_AGENDA"),own=create("viewer","LOCAL_AGENDA");
        react(id,"EMPATHY","viewer");react(id,"NEEDED","viewer");react(id,"CURIOUS","viewer");long comment=comment(id,"owner");
        reply(comment,"viewer");evaluate(comment,"LIKE","viewer");select(id,options(id).get(1),"viewer");
        assertThat(request("PUT","/api/v1/posts/"+bookmark+"/bookmark","viewer",null).statusCode()).isEqualTo(200);react(own,"EMPATHY","other");
        var list=json(get("participations","viewer")).path("data");assertThat(list.size()).isEqualTo(1);var item=list.path(0);assertThat(item.path("id").asLong()).isEqualTo(id);
        var flags=item.path("myParticipation");assertThat(flags.path("reactions").size()).isEqualTo(3);assertThat(flags.path("hasCommentOrReply").asBoolean()).isTrue();assertThat(flags.path("hasCommentOrReplyEvaluation").asBoolean()).isTrue();assertThat(flags.path("hasVote").asBoolean()).isTrue();
        assertThat(item.path("vote").path("myOptionId").asLong()).isEqualTo(options(id).get(1));
        assertThat(json(get("participations?type=LOCAL_AGENDA","viewer")).path("data").isEmpty()).isTrue();
    }
    @Test void canceledCurrentActionsDisappearButRemainingParticipationKeepsCard()throws Exception{
        long id=create("owner","LOCAL_AGENDA"),c=comment(id,"owner");react(id,"EMPATHY","viewer");react(id,"CURIOUS","viewer");evaluate(c,"LIKE","viewer");
        assertThat(request("DELETE","/api/v1/posts/"+id+"/reactions/EMPATHY","viewer",null).statusCode()).isEqualTo(200);
        var flags=json(get("participations","viewer")).path("data").path(0).path("myParticipation");assertThat(flags.path("reactions").size()).isEqualTo(1);
        assertThat(request("DELETE","/api/v1/posts/"+id+"/reactions/CURIOUS","viewer",null).statusCode()).isEqualTo(200);assertThat(json(get("participations","viewer")).path("data").size()).isEqualTo(1);
        assertThat(request("DELETE","/api/v1/comments/"+c+"/evaluation","viewer",null).statusCode()).isEqualTo(200);assertThat(json(get("participations","viewer")).path("data").isEmpty()).isTrue();
    }
    @Test void participationOrderUsesLatestRemainingActionAndTiesUsePostId()throws Exception{
        long older=create("owner","LOCAL_AGENDA"),newer=create("owner","LOCAL_AGENDA");react(older,"EMPATHY","viewer");react(newer,"CURIOUS","viewer");
        admin().update("update discushion.post_reactions set created_at=? where post_id=?",Timestamp.from(NOW.minusSeconds(10)),newer);
        assertThat(json(get("participations?size=1","viewer")).path("data").path(0).path("id").asLong()).isEqualTo(older);
        admin().update("update discushion.post_reactions set created_at=? where post_id=?",Timestamp.from(NOW),newer);
        var first=json(get("participations?size=1","viewer"));assertThat(first.path("data").path(0).path("id").asLong()).isEqualTo(newer);
        var last=json(get("participations?size=1&cursor="+first.path("meta").path("nextCursor").asText(),"viewer"));assertThat(last.path("data").path(0).path("id").asLong()).isEqualTo(older);
    }
    @Test void votesShowActualChoiceCurrentCountsRoundingStatusAndChanges()throws Exception{
        long id=create("owner","VOTE");var choices=options(id);select(id,choices.get(0),"owner");select(id,choices.get(0),"other");select(id,choices.get(1),"viewer");
        var item=json(get("votes?status=OPEN","viewer")).path("data").path(0);var vote=item.path("vote");
        assertThat(item.path("availability").asText()).isEqualTo("AVAILABLE");assertThat(vote.path("myOptionId").asLong()).isEqualTo(choices.get(1));
        assertThat(vote.path("participantCount").asLong()).isEqualTo(3);assertThat(vote.path("options").path(0).path("votePercentage").asDouble()).isEqualTo(66.67);
        assertThat(vote.path("options").path(1).path("votePercentage").asDouble()).isEqualTo(33.33);
        assertThat(request("PUT","/api/v1/posts/"+id+"/vote","viewer",JSON.writeValueAsString(Map.of("optionId",choices.get(0),"confirmChange",true))).statusCode()).isEqualTo(200);
        assertThat(json(get("votes","viewer")).path("data").path(0).path("vote").path("myOptionId").asLong()).isEqualTo(choices.get(0));
        admin().update("update discushion.polls set ends_at=? where post_id=?",Timestamp.from(NOW),id);
        assertThat(json(get("votes?status=OPEN","viewer")).path("data").isEmpty()).isTrue();assertThat(json(get("votes?status=CLOSED","viewer")).path("data").size()).isEqualTo(1);
    }
    @Test void deletedVoteKeepsOnlyMinimumHistoryInAllWithoutAnyContentChoiceOrResults()throws Exception{
        long id=create("owner","VOTE");select(id,options(id).get(1),"viewer");
        assertThat(request("DELETE","/api/v1/posts/"+id,"owner",null).statusCode()).isEqualTo(204);
        var item=json(get("votes","viewer")).path("data").path(0);assertThat(item.size()).isEqualTo(5);assertThat(item.path("availability").asText()).isEqualTo("UNAVAILABLE");
        assertThat(item.path("post").isNull()).isTrue();assertThat(item.path("vote").isNull()).isTrue();assertThat(item.path("postId").asLong()).isEqualTo(id);
        assertThat(json(get("votes?status=OPEN","viewer")).path("data").isEmpty()).isTrue();assertThat(json(get("votes?status=CLOSED","viewer")).path("data").isEmpty()).isTrue();
        assertThat(json(get("participations","viewer")).path("data").isEmpty()).isTrue();assertThat(json(get("posts","owner")).path("data").isEmpty()).isTrue();
        assertThat(admin().queryForObject("select count(*) from discushion.vote_selections where user_id=?",Integer.class,viewer)).isEqualTo(1);
    }
    @Test void voteOrderFollowsChangedAtAndPreservesFirstParticipationTimestamp()throws Exception{
        long older=create("owner","VOTE"),newer=create("owner","VOTE");select(older,options(older).get(0),"viewer");select(newer,options(newer).get(0),"viewer");
        admin().update("update discushion.vote_selections set first_submitted_at=?,updated_at=? where user_id=?",Timestamp.from(NOW.minusSeconds(60)),Timestamp.from(NOW.minusSeconds(30)),viewer);
        assertThat(json(get("votes?size=1","viewer")).path("data").path(0).path("postId").asLong()).isEqualTo(newer);
        assertThat(request("PUT","/api/v1/posts/"+older+"/vote","viewer",JSON.writeValueAsString(Map.of("optionId",options(older).get(1),"confirmChange",true))).statusCode()).isEqualTo(200);
        var first=json(get("votes?size=1","viewer"));assertThat(first.path("data").path(0).path("postId").asLong()).isEqualTo(older);
        assertThat(Instant.parse(first.path("data").path(0).path("participatedAt").asText())).isEqualTo(NOW.minusSeconds(60));
    }
    @Test void rejectsGuestUnregisteredIncompleteAndUntrustedQueryOrReusedCursor()throws Exception{
        assertThat(get("posts",null).statusCode()).isEqualTo(401);assertThat(get("votes","missing").statusCode()).isEqualTo(403);
        admin().update("update discushion.users set registration_completed_at=null where id=?",viewer);assertThat(get("participations","viewer").statusCode()).isEqualTo(403);
        admin().update("update discushion.users set registration_completed_at=? where id=?",Timestamp.from(NOW),viewer);
        for(String path:List.of("posts?userId=1","posts?status=OPEN","votes?type=VOTE","votes?status=NOPE","posts?size=101","posts?size=1&size=2","posts?cursor=bad!"))assertThat(get(path,"owner").statusCode()).isEqualTo(400);
        create("owner","LOCAL_AGENDA");create("owner","LOCAL_AGENDA");String cursor=json(get("posts?size=1","owner")).path("meta").path("nextCursor").asText();
        assertThat(get("posts?cursor="+cursor,"viewer").statusCode()).isEqualTo(400);assertThat(get("participations?cursor="+cursor,"owner").statusCode()).isEqualTo(400);assertThat(get("posts?type=VOTE&cursor="+cursor,"owner").statusCode()).isEqualTo(400);
    }
    @Test void repeatableSnapshotPreventsMixedDeletionContentAndResults()throws Exception{
        long id=create("owner","VOTE");select(id,options(id).get(1),"viewer");
        SnapshotProbe.entered=new CountDownLatch(1);SnapshotProbe.release=new CountDownLatch(1);SnapshotProbe.pause.set(true);var executor=Executors.newSingleThreadExecutor();
        try{var pending=executor.submit(()->get("votes","viewer"));assertThat(SnapshotProbe.entered.await(10,TimeUnit.SECONDS)).isTrue();
            assertThat(request("DELETE","/api/v1/posts/"+id,"owner",null).statusCode()).isEqualTo(204);SnapshotProbe.release.countDown();
            assertThat(json(pending.get(15,TimeUnit.SECONDS)).path("data").path(0).path("availability").asText()).isEqualTo("AVAILABLE");
            assertThat(json(get("votes","viewer")).path("data").path(0).path("availability").asText()).isEqualTo("UNAVAILABLE");
        }finally{SnapshotProbe.release.countDown();SnapshotProbe.pause.set(false);executor.shutdownNow();}
    }
    @Test void runtimeRoleIsUsedAndReadsDoNotWriteActivityEventsOrChangeCurrentRelations()throws Exception{
        long id=create("owner","VOTE");select(id,options(id).get(0),"viewer");
        int events=admin().queryForObject("select count(*) from discushion.activity_events",Integer.class);
        assertThat(new JdbcTemplate(source).queryForObject("select current_user",String.class)).isEqualTo("discushion_server");
        for(String path:List.of("posts","participations","votes"))assertThat(get(path,"viewer").statusCode()).isEqualTo(200);
        assertThat(admin().queryForObject("select count(*) from discushion.activity_events",Integer.class)).isEqualTo(events);
        assertThat(admin().queryForObject("select count(*) from discushion.vote_selections where user_id=?",Integer.class,viewer)).isEqualTo(1);
    }
    @Test void adapterFailureIsGeneric500AndNeverConvertedToEmptySuccess()throws Exception{
        create("owner","LOCAL_AGENDA");SnapshotProbe.fail.set(true);
        try{var response=get("posts","owner");assertThat(response.statusCode()).isEqualTo(500);assertThat(response.body()).contains("INTERNAL_ERROR").doesNotContain("synthetic27","discushion.");}
        finally{SnapshotProbe.fail.set(false);}
    }
    @TestConfiguration(proxyBeanMethods=false) static class SnapshotProbe {
        static final AtomicBoolean pause=new AtomicBoolean(),fail=new AtomicBoolean();
        static volatile CountDownLatch entered=new CountDownLatch(0),release=new CountDownLatch(0);
        @Bean @Primary PostSummaryReader personalSummaryProbe(DataSource source){var delegate=new JdbcPostSummaryReader(source);return ids->{
            if(fail.get())throw new IllegalStateException("synthetic27 source failure");var rows=delegate.findAll(ids);
            if(pause.compareAndSet(true,false)){entered.countDown();try{if(!release.await(15,TimeUnit.SECONDS))throw new IllegalStateException("Snapshot probe timeout");}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException(e);}}
            return rows;
        };}
    }
    List<Long> options(long id){return admin().queryForList("select o.id from discushion.poll_options o join discushion.polls p on p.id=o.poll_id where p.post_id=? order by o.sort_order",Long.class,id);}
    void select(long id,long option,String who)throws Exception{assertThat(request("PUT","/api/v1/posts/"+id+"/vote",who,JSON.writeValueAsString(Map.of("optionId",option,"confirmChange",false))).statusCode()).isEqualTo(200);}
    void react(long id,String type,String who)throws Exception{assertThat(request("PUT","/api/v1/posts/"+id+"/reactions/"+type,who,null).statusCode()).isEqualTo(200);}
    long comment(long id,String who)throws Exception{var response=request("POST","/api/v1/posts/"+id+"/comments",who,"{\"content\":\"의견\"}");assertThat(response.statusCode()).isEqualTo(201);return JSON.readTree(response.body()).path("data").path("id").asLong();}
    void reply(long comment,String who)throws Exception{assertThat(request("POST","/api/v1/comments/"+comment+"/replies",who,"{\"content\":\"답글\"}").statusCode()).isEqualTo(201);}
    void evaluate(long comment,String type,String who)throws Exception{assertThat(request("PUT","/api/v1/comments/"+comment+"/evaluation",who,JSON.writeValueAsString(Map.of("type",type))).statusCode()).isEqualTo(200);}
    long create(String who,String type)throws Exception{
        var body=new LinkedHashMap<String,Object>();body.put("type",type);body.put("topic","OTHER");body.put("regionId",region);body.put("title",marker);body.put("content","개인 기록 본문");
        if(type.equals("LOCAL_ACTIVITY"))body.put("details",Map.of("source","기관","schedule","다음 주","place","주민센터","activityStatus","SCHEDULED"));
        if(type.equals("VOTE"))body.put("details",Map.of("question","어느 안이 좋나요?","options",List.of("첫 번째","두 번째"),"endsAt",admin().queryForObject("select current_timestamp",Timestamp.class).toInstant().plusSeconds(3600).toString()));
        var response=request("POST","/api/v1/posts",who,JSON.writeValueAsString(body));assertThat(response.statusCode()).withFailMessage(response.body()).isEqualTo(201);return JSON.readTree(response.body()).path("data").path("postId").asLong();
    }
    HttpResponse<String> get(String path,String who)throws Exception{return request("GET","/api/v1/users/me/"+path,who,null);}
    JsonNode json(HttpResponse<String> response){assertThat(response.statusCode()).withFailMessage(response.body()).isEqualTo(200);return JSON.readTree(response.body());}
    String subject(String suffix){return "did:privy:"+marker+"-"+suffix;}
    HttpResponse<String> request(String method,String path,String who,String body)throws Exception{
        var request=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).timeout(Duration.ofSeconds(20));
        if(who!=null){var token=new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.ES256).type(JOSEObjectType.JWT).build(),new JWTClaimsSet.Builder().issuer("privy.io").audience("synthetic-post14-app").subject(subject(who)).issueTime(Date.from(NOW.minusSeconds(60))).expirationTime(Date.from(NOW.plusSeconds(3600))).claim("sid","synthetic-personal27-session").build());token.sign(new ECDSASigner(PostCreationHttpIntegrationTests.KEY));request.header("Authorization","Bearer "+token.serialize());}
        if(body!=null)request.header("Content-Type","application/json");return HttpClient.newHttpClient().send(request.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
    }
}
