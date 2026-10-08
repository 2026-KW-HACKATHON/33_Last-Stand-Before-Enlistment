package com.discushion.posts;

import com.discushion.photos.PhotoStorage;
import java.net.http.HttpResponse;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.*;
import javax.sql.DataSource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import tools.jackson.databind.JsonNode;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@EnabledIfEnvironmentVariable(named="DISCUSHION_TEST_JDBC_URL",matches="jdbc:postgresql://127\\.0\\.0\\.1:55432/discushion_migration_test(?:\\?.*)?")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"spring.profiles.active=board17-http-test","spring.config.import=","PRIVY_APP_ID=synthetic-post14-app","PHOTO_UPLOADS_ENABLED=false","PHOTO_CLEANUP_ENABLED=false"})
@Import({PostCreationHttpIntegrationTests.Wiring.class,PersonalHttpIntegrationTests.SnapshotProbe.class})
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class BoardHttpIntegrationTests {
    @LocalServerPort int port;@Autowired DataSource source;@Autowired PhotoStorage photos;
    PersonalHttpIntegrationTests fixture;long target;
    @BeforeEach void setup(){fixture=new PersonalHttpIntegrationTests();fixture.port=port;fixture.source=source;fixture.setup();reset(photos);
        target=PersonalHttpIntegrationTests.admin().queryForObject("insert into discushion.regions(name) values(?) returning id",Long.class,fixture.marker+"-target");
        PersonalHttpIntegrationTests.admin().update("insert into discushion.neighbor_verified_regions(user_id,region_id,verified_at) values(?,?,?)",fixture.owner,target,Timestamp.from(PersonalHttpIntegrationTests.NOW));}
    @AfterEach void cleanup(){
        var admin=PersonalHttpIntegrationTests.admin();
        admin.update("delete from discushion.post_photos where post_id in(select id from discushion.posts where region_id in(?,?))",fixture.region,target);
        admin.update("delete from discushion.media_files where owner_user_id=?",fixture.owner);
        admin.update("update discushion.posts set region_id=? where region_id=?",fixture.region,target);
        fixture.cleanup();admin.update("delete from discushion.regions where id=?",target);
    }
    @AfterAll static void restore(){PersonalHttpIntegrationTests.restore();}
    HttpResponse<String> list(String query,String who)throws Exception{return fixture.request("GET","/api/v1/posts"+query,who,null);}
    JsonNode data(HttpResponse<String> response){return fixture.json(response).path("data");}
    @Test void defaultRegionReturnsThreeTypesAndNoPlaceholderOrPrivateAuthorFields()throws Exception{
        fixture.create("owner","LOCAL_AGENDA");fixture.create("owner","LOCAL_ACTIVITY");fixture.create("owner","VOTE");
        var items=data(list("","viewer"));assertThat(items.size()).isEqualTo(3);
        for(var item:items){assertThat(item.path("thumbnailUrl").isNull()).isTrue();assertThat(item.path("author").has("email")).isFalse();assertThat(item.has("fileId")).isFalse();}
        assertThat(items.path(0).path("vote").path("options").size()).isEqualTo(2);
    }
    @Test void regionTypeTopicFiltersUseTheActualSourceAndNeverWriteProfileRegion()throws Exception{
        long id=fixture.create("owner","LOCAL_AGENDA");fixture.create("owner","LOCAL_ACTIVITY");
        assertThat(data(list("?type=LOCAL_ACTIVITY&topic=OTHER","viewer")).size()).isEqualTo(1);
        assertThat(data(list("?topic=SAFETY","viewer")).isEmpty()).isTrue();
        assertThat(fixture.request("PATCH","/api/v1/posts/"+id,"owner",PersonalHttpIntegrationTests.JSON.writeValueAsString(Map.of("regionId",target))).statusCode()).isEqualTo(200);
        assertThat(data(list("?regionId="+target,"viewer")).path(0).path("id").asLong()).isEqualTo(id);
        assertThat(PersonalHttpIntegrationTests.admin().queryForObject("select activity_region_id from discushion.profiles where user_id=?",Long.class,fixture.viewer)).isEqualTo(fixture.region);
    }
    @Test void cursorPagesTieByIdAndBindMemberRegionTypeAndTopic()throws Exception{
        long one=fixture.create("owner","LOCAL_AGENDA"),two=fixture.create("owner","LOCAL_AGENDA"),three=fixture.create("owner","LOCAL_AGENDA");
        PersonalHttpIntegrationTests.admin().update("update discushion.posts set created_at=? where region_id=?",Timestamp.from(PersonalHttpIntegrationTests.NOW),fixture.region);
        var first=fixture.json(list("?size=2","viewer"));assertThat(first.path("data").path(0).path("id").asLong()).isEqualTo(three);assertThat(first.path("data").path(1).path("id").asLong()).isEqualTo(two);
        String cursor=first.path("meta").path("nextCursor").asText();var last=fixture.json(list("?size=2&cursor="+cursor,"viewer"));assertThat(last.path("data").path(0).path("id").asLong()).isEqualTo(one);assertThat(last.path("meta").path("hasNext").asBoolean()).isFalse();
        for(String query:List.of("?regionId="+target+"&cursor="+cursor,"?type=LOCAL_AGENDA&cursor="+cursor,"?topic=OTHER&cursor="+cursor))assertThat(list(query,"viewer").statusCode()).isEqualTo(400);
        assertThat(list("?cursor="+cursor,"other").statusCode()).isEqualTo(400);assertThat(fixture.get("posts?cursor="+cursor,"viewer").statusCode()).isEqualTo(400);
    }
    @Test void newEditsReactionsAndDeletionAppearWithoutCopiedCards()throws Exception{
        long id=fixture.create("owner","LOCAL_AGENDA");assertThat(data(list("","viewer")).size()).isEqualTo(1);
        assertThat(fixture.request("PATCH","/api/v1/posts/"+id,"owner","{\"title\":\"수정된 제목\",\"topic\":\"SAFETY\"}").statusCode()).isEqualTo(200);
        fixture.react(id,"EMPATHY","viewer");fixture.comment(id,"other");var item=data(list("?topic=SAFETY","viewer")).path(0);
        assertThat(item.path("title").asText()).isEqualTo("수정된 제목");assertThat(item.path("reactionCounts").path("EMPATHY").asLong()).isEqualTo(1);assertThat(item.path("commentCount").asLong()).isEqualTo(1);
        assertThat(fixture.request("DELETE","/api/v1/posts/"+id,"owner",null).statusCode()).isEqualTo(204);assertThat(data(list("","viewer")).isEmpty()).isTrue();
    }
    @Test void firstOrderedLinkedPhotoUsesPublicAdapterAndNoFileIdIsReturned()throws Exception{
        long id=fixture.create("owner","LOCAL_AGENDA");var admin=PersonalHttpIntegrationTests.admin();
        long file=admin.queryForObject("insert into discushion.media_files(owner_user_id,storage_key,original_name,mime_type,size_bytes,purpose,created_at,lifecycle_status,uploaded_at,linked_at) values(?,?,?,'image/png',68,'POST_PHOTO',?,'LINKED',?,?) returning id",Long.class,fixture.owner,"synthetic17/first","test.png",Timestamp.from(PersonalHttpIntegrationTests.NOW),Timestamp.from(PersonalHttpIntegrationTests.NOW),Timestamp.from(PersonalHttpIntegrationTests.NOW));
        admin.update("insert into discushion.post_photos(post_id,file_id,sort_order) values(?,?,0)",id,file);when(photos.publicUrl("synthetic17/first")).thenReturn("https://example.invalid/public/first.png");
        var item=data(list("","viewer")).path(0);assertThat(item.path("thumbnailUrl").asText()).isEqualTo("https://example.invalid/public/first.png");assertThat(item.has("fileId")).isFalse();
    }
    @Test void guestUnregisteredIncompleteInvalidInputsAndMissingRegionsAreRejected()throws Exception{
        assertThat(list("",null).statusCode()).isEqualTo(401);assertThat(list("","missing").statusCode()).isEqualTo(403);
        PersonalHttpIntegrationTests.admin().update("update discushion.users set registration_completed_at=null where id=?",fixture.viewer);assertThat(list("","viewer").statusCode()).isEqualTo(403);
        for(String query:List.of("?q=text","?type=ALL","?size=101","?regionId=0","?regionId=1&regionId=1","?topic=NOPE","?cursor=bad!"))assertThat(list(query,"owner").statusCode()).isEqualTo(400);
        assertThat(list("?regionId=9007199254740991","owner").statusCode()).isEqualTo(404);
    }
    @Test void changingDefaultRegionInvalidatesPriorCursorAndEmptyRegionHasNoFallback()throws Exception{
        fixture.create("owner","LOCAL_AGENDA");fixture.create("owner","LOCAL_AGENDA");String cursor=fixture.json(list("?size=1","viewer")).path("meta").path("nextCursor").asText();
        PersonalHttpIntegrationTests.admin().update("update discushion.profiles set activity_region_id=? where user_id=?",target,fixture.viewer);
        assertThat(list("?cursor="+cursor,"viewer").statusCode()).isEqualTo(400);assertThat(data(list("","viewer")).isEmpty()).isTrue();
    }
    @Test void deletedDuringReadDoesNotMixSourceAndNextReadOmitsDeletedPost()throws Exception{
        long id=fixture.create("owner","LOCAL_AGENDA");
        PersonalHttpIntegrationTests.SnapshotProbe.entered=new CountDownLatch(1);PersonalHttpIntegrationTests.SnapshotProbe.release=new CountDownLatch(1);PersonalHttpIntegrationTests.SnapshotProbe.pause.set(true);var executor=Executors.newSingleThreadExecutor();
        try{var pending=executor.submit(()->list("","viewer"));assertThat(PersonalHttpIntegrationTests.SnapshotProbe.entered.await(10,TimeUnit.SECONDS)).isTrue();
            assertThat(fixture.request("DELETE","/api/v1/posts/"+id,"owner",null).statusCode()).isEqualTo(204);PersonalHttpIntegrationTests.SnapshotProbe.release.countDown();
            assertThat(data(pending.get(15,TimeUnit.SECONDS)).size()).isEqualTo(1);assertThat(data(list("","viewer")).isEmpty()).isTrue();
        }finally{PersonalHttpIntegrationTests.SnapshotProbe.release.countDown();executor.shutdownNow();}
    }
    @Test void adapterFailureIsSafe500AndRuntimeRoleDoesNotNeedNewPermissions()throws Exception{
        fixture.create("owner","LOCAL_AGENDA");assertThat(new org.springframework.jdbc.core.JdbcTemplate(source).queryForObject("select current_user",String.class)).isEqualTo("discushion_server");
        PersonalHttpIntegrationTests.SnapshotProbe.fail.set(true);try{var response=list("","viewer");assertThat(response.statusCode()).isEqualTo(500);assertThat(response.body()).contains("INTERNAL_ERROR").doesNotContain("synthetic27","discushion.");}finally{PersonalHttpIntegrationTests.SnapshotProbe.fail.set(false);}
    }
}
