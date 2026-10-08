package com.discushion.posts;

import com.discushion.contracts.participation.ParticipationSnapshotReader;
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

/** Real signature filter, production adapters, HTTP and isolated minimum-privilege PostgreSQL role. */
@EnabledIfEnvironmentVariable(named = "DISCUSHION_TEST_JDBC_URL", matches = "jdbc:postgresql://127\\.0\\.0\\.1:55432/discushion_migration_test(?:\\?.*)?")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.profiles.active=detail15-http-test", "spring.config.import=", "PRIVY_APP_ID=synthetic-post14-app",
        "PHOTO_UPLOADS_ENABLED=true", "PHOTO_CLEANUP_ENABLED=false",
        "SHARE_TOKEN_SIGNING_KEY=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "PUBLIC_WEB_BASE_URL=https://example.invalid" })
@Import({PostCreationHttpIntegrationTests.Wiring.class, PostDetailHttpIntegrationTests.ProbeWiring.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PostDetailHttpIntegrationTests {
    static final Instant NOW = PostCreationHttpIntegrationTests.NOW;
    static JdbcTemplate admin() { return PostCreationHttpIntegrationTests.admin(); }
    static final JsonMapper JSON = JsonMapper.builder().build();
    @AfterAll static void restoreRole() {
        try {
            // Only failed setup's unattached synthetic users in the guarded localhost test database.
            admin().update("delete from discushion.users u where privy_user_id like 'did:privy:synthetic-detail15-%' and not exists(select 1 from discushion.profiles p where p.user_id=u.id) and not exists(select 1 from discushion.posts p where p.author_user_id=u.id)");
        } finally { PostCreationHttpIntegrationTests.restoreServerRole(); }
    }
    @LocalServerPort int port;
    @Autowired DataSource source;
    @Autowired com.discushion.photos.PhotoStorage storage;
    @Autowired ParticipationSnapshotReader participation;
    @Autowired PostSummaryReader summaries;
    @Autowired PlatformTransactionManager manager;
    String marker;
    long region, owner, other;
    final List<Long> institutionIds = new ArrayList<>();

    @BeforeEach void setup() {
        marker = "synthetic-detail15-" + UUID.randomUUID();
        region = admin().queryForObject("insert into discushion.regions(name) values(?) returning id", Long.class, marker);
        owner = user("owner"); other = user("other");
        reset(storage); when(storage.publicUrl(anyString())).thenAnswer(call -> "https://storage.example.invalid/" + call.getArgument(0));
    }
    long user(String suffix) {
        long id = admin().queryForObject("""
                insert into discushion.users(email,email_verified_at,created_at,updated_at,privy_user_id,registration_completed_at)
                values(?,?::timestamptz,?::timestamptz,?::timestamptz,?,?::timestamptz) returning id
                """, Long.class, marker + "-" + suffix + "@example.invalid", NOW.toString(), NOW.toString(), NOW.toString(), subject(suffix), NOW.toString());
        admin().update("insert into discushion.profiles(user_id,nickname,activity_region_id,updated_at) values(?,?,?,?)", id, "d15" + Long.toString(id, 36), region, Timestamp.from(NOW));
        admin().update("insert into discushion.neighbor_verified_regions(user_id,region_id,verified_at) values(?,?,?)", id, region, Timestamp.from(NOW));
        return id;
    }
    @AfterEach void cleanup() {
        String posts = "select id from discushion.posts where author_user_id=?";
        admin().update("delete from discushion.comment_evaluations where comment_id in(select id from discushion.comments where post_id in(" + posts + "))", owner);
        admin().update("delete from discushion.comments where post_id in(" + posts + ")", owner);
        for (String table : List.of("post_reactions", "bookmarks", "post_photos", "institution_agenda_adoptions")) admin().update("delete from discushion." + table + " where post_id in(" + posts + ")", owner);
        admin().update("delete from discushion.vote_selections where poll_id in(select id from discushion.polls where post_id in(" + posts + "))", owner);
        admin().update("delete from discushion.poll_options where poll_id in(select id from discushion.polls where post_id in(" + posts + "))", owner);
        admin().update("delete from discushion.polls where post_id in(" + posts + ")", owner);
        admin().update("delete from discushion.activity_post_details where post_id in(" + posts + ")", owner);
        admin().update("delete from discushion.posts where author_user_id=?", owner);
        admin().update("delete from discushion.media_files where owner_user_id in (?,?)", owner, other);
        for (long id : new long[]{owner, other}) {
            admin().update("delete from discushion.institution_credentials where user_id=?", id);
            admin().update("delete from discushion.neighbor_verified_regions where user_id=?", id);
            admin().update("delete from discushion.profiles where user_id=?", id);
            admin().update("delete from discushion.users where id=?", id);
        }
        institutionIds.forEach(id -> admin().update("delete from discushion.institutions where id=?", id)); institutionIds.clear();
        admin().update("delete from discushion.regions where id=?", region);
    }

    @Test void readsAllCreatedTypesFromCanonicalSource() throws Exception {
        long agenda = create("LOCAL_AGENDA", Map.of(), List.of());
        var detail = data(get(agenda, "owner"));
        assertThat(detail.path("title").asString()).isEqualTo(marker);
        assertThat(detail.path("images").size()).isZero();
        assertThat(detail.path("reactionCounts").path("total").asLong()).isZero();
        assertThat(detail.path("capabilities").path("canEdit").asBoolean()).isTrue();
        long activity = create("LOCAL_ACTIVITY", Map.of("source", "구청", "schedule", "다음 주 토요일", "place", "주민센터", "activityStatus", "ENDED", "externalParticipationUrl", "https://example.invalid/event"), List.of());
        var activityData = data(get(activity, "owner"));
        assertThat(activityData.path("activity").path("organizerEmail").asString()).isEqualTo(marker + "-owner@example.invalid");
        assertThat(activityData.path("activity").path("externalParticipationEnabled").asBoolean()).isFalse();
        assertThat(activityData.path("author").has("email")).isFalse();
        var sharedActivity = data(call("GET", "/api/v1/posts/" + activity, null, null, share(activity)));
        assertThat(sharedActivity.path("activity").path("organizerEmail").asString()).isEqualTo(marker + "-owner@example.invalid");
        assertThat(sharedActivity.has("myState")).isFalse();
        admin().update("update discushion.activity_post_details set external_participation_url='javascript:alert(1)' where post_id=?", activity);
        assertThat(data(get(activity, "other")).path("activity").path("externalParticipationUrl").isNull()).isTrue();
        admin().update("update discushion.activity_post_details set external_participation_url='https://example.invalid/event' where post_id=?", activity);
        admin().update("update discushion.activity_post_details set activity_status='IN_PROGRESS' where post_id=?", activity);
        assertThat(data(get(activity, "owner")).path("activity").path("externalParticipationEnabled").asBoolean()).isTrue();
        long vote = create("VOTE", Map.of("question", "질문", "options", List.of("찬성", "반대"), "endsAt", NOW.plusSeconds(864000).toString()), List.of());
        var voteData = data(get(vote, "other")).path("vote");
        assertThat(voteData.path("status").asString()).isEqualTo("OPEN");
        assertThat(voteData.path("options").path(0).path("votePercentage").asDouble()).isZero();
        assertThat(voteData.has("myOptionId")).isTrue(); assertThat(voteData.path("myOptionId").isNull()).isTrue();
        assertThat(new JdbcTemplate(source).queryForObject("select current_user", String.class)).isEqualTo("discushion_server");
    }

    @Test void hidesOwnerFileIdsAndPrivateStateFromOtherMembersAndGuests() throws Exception {
        long file = photo(); long id = create("LOCAL_AGENDA", Map.of(), List.of(file));
        assertThat(data(get(id, "owner")).path("images").path(0).path("fileId").asLong()).isEqualTo(file);
        var otherData = data(get(id, "other")); assertThat(otherData.path("images").path(0).has("fileId")).isFalse();
        String share = share(id); var guest = data(call("GET", "/api/v1/posts/" + id, null, null, share));
        assertThat(guest.has("myState")).isFalse(); assertThat(guest.path("images").path(0).has("fileId")).isFalse();
        assertThat(guest.path("capabilities").path("canComment").asBoolean()).isTrue();
        assertThat(guest.path("capabilities").path("canBookmark").asBoolean()).isFalse();
        assertThat(guest.path("author").size()).isEqualTo(3);
        assertThat(guest.path("author").has("displayName") && guest.path("author").has("profileImageUrl") && guest.path("author").has("institutionVerified")).isTrue();
        assertThat(call("GET", "/api/v1/posts/" + id, null, null, null).statusCode()).isEqualTo(401);
        long foreign = create("LOCAL_AGENDA", Map.of(), List.of());
        assertThat(call("GET", "/api/v1/posts/" + foreign, null, null, share).statusCode()).isEqualTo(403);
        assertThat(call("GET", "/api/v1/posts/" + id, null, null, share + "x").statusCode()).isEqualTo(401);
        assertThat(call("GET", "/api/v1/posts/" + id, "invalid", null, share).statusCode()).isEqualTo(401);
    }

    @Test void detailMatchesRealParticipationWritesAndGuestVotePrivacy() throws Exception {
        long id = create("VOTE", Map.of("question", "질문", "options", List.of("찬성", "반대"), "endsAt", NOW.plusSeconds(864000).toString()), List.of());
        long option = data(get(id, "owner")).path("vote").path("options").path(0).path("id").asLong();
        assertThat(call("PUT", "/api/v1/posts/" + id + "/vote", "owner", JSON.writeValueAsString(Map.of("optionId", option, "confirmChange", false)), null).statusCode()).isEqualTo(200);
        assertThat(call("PUT", "/api/v1/posts/" + id + "/reactions/EMPATHY", "owner", null, null).statusCode()).isEqualTo(200);
        assertThat(call("PUT", "/api/v1/posts/" + id + "/bookmark", "owner", null, null).statusCode()).isEqualTo(200);
        var root = call("POST", "/api/v1/posts/" + id + "/comments", "owner", "{\"content\":\"의견\"}", null);
        assertThat(root.statusCode()).isEqualTo(201);
        var ownerData = data(get(id, "owner")); var otherData = data(get(id, "other"));
        assertThat(ownerData.path("vote").path("myOptionId").asLong()).isEqualTo(option);
        assertThat(ownerData.path("vote").path("participantCount").asLong()).isEqualTo(1);
        assertThat(ownerData.path("vote").path("options").path(0).path("votePercentage").asDouble()).isEqualTo(100);
        assertThat(ownerData.path("myState").path("isBookmarked").asBoolean()).isTrue();
        assertThat(otherData.path("myState").path("isBookmarked").asBoolean()).isFalse();
        assertThat(otherData.path("vote").path("myOptionId").isNull()).isTrue();
        var bookmarks = call("GET", "/api/v1/users/me/bookmarks", "owner", null, null);
        assertThat(bookmarks.statusCode()).withFailMessage(bookmarks.body()).isEqualTo(200);
        assertThat(JSON.readTree(bookmarks.body()).path("data").path(0).path("postId").asLong()).isEqualTo(id);
        var guest = data(call("GET", "/api/v1/posts/" + id, null, null, share(id)));
        assertThat(guest.path("vote").has("myOptionId")).isFalse();
        assertThat(guest.path("comments").path(0).has("myEvaluation")).isFalse();
        assertThat(guest.path("reactionCounts").path("total").asLong()).isEqualTo(1);
        assertThat(guest.path("commentCount").asLong()).isEqualTo(1);
    }

    @Test void initialCommentsUseExactLikesPageAndAllReplies() throws Exception {
        long id = create("LOCAL_AGENDA", Map.of(), List.of()); long first = 0;
        for (int i = 0; i < 21; i++) {
            long comment = admin().queryForObject("insert into discushion.comments(post_id,author_user_id,author_kind,content,created_at) values(?,?,'MEMBER',?,?) returning id", Long.class, id, owner, "comment-" + i, Timestamp.from(NOW.minusSeconds(i)));
            if (i == 0) first = comment;
        }
        for (int i = 0; i < 25; i++) admin().update("insert into discushion.comments(post_id,parent_comment_id,author_kind,content,created_at) values(?,?,'GUEST',?,?)", id, first, "reply-" + i, Timestamp.from(NOW.plusSeconds(i)));
        admin().update("insert into discushion.comment_evaluations(comment_id,user_id,evaluation_type,created_at,updated_at) values(?,?,'LIKE',?,?)", first, other, Timestamp.from(NOW), Timestamp.from(NOW));
        var detail = data(get(id, "owner")); var list = JSON.readTree(call("GET", "/api/v1/posts/" + id + "/comments", "owner", null, null).body());
        assertThat(detail.path("comments")).isEqualTo(list.path("data"));
        assertThat(detail.path("comments").size()).isEqualTo(20);
        assertThat(detail.path("comments").path(0).path("replies").size()).isEqualTo(25);
        assertThat(detail.path("commentCount").asLong()).isEqualTo(46);
        assertThat(detail.path("commentsMeta").path("sort").asString()).isEqualTo("LIKES");
        assertThat(detail.path("commentsMeta").path("nextCursor")).isEqualTo(list.path("meta").path("nextCursor"));
        assertThat(detail.path("commentsMeta").path("hasNext").asBoolean()).isTrue();
    }

    @Test void readsCurrentBadgeAndOnlyPublicActiveAdoptions() throws Exception {
        long id = create("LOCAL_AGENDA", Map.of(), List.of());
        long institution = admin().queryForObject("insert into discushion.institutions(name,created_at) values(?,?) returning id", Long.class, marker + "-office", Timestamp.from(NOW));
        institutionIds.add(institution);
        long credential = admin().queryForObject("insert into discushion.institution_credentials(user_id,institution_id,responsible_region_id,completed_at,valid_until) values(?,?,?,?,?) returning id", Long.class, owner, institution, region, Timestamp.from(NOW.minusSeconds(86400)), Timestamp.from(NOW.plusSeconds(86400)));
        long adoption = admin().queryForObject("insert into discushion.institution_agenda_adoptions(post_id,institution_id,adopted_by_user_id,credential_id,adopted_at) values(?,?,?,?,?) returning id", Long.class, id, institution, owner, credential, Timestamp.from(NOW));
        var current = data(get(id, "owner")); assertThat(current.path("author").path("institutionVerified").asBoolean()).isTrue();
        assertThat(current.path("adoptions").path(0).size()).isEqualTo(2);
        assertThat(current.path("adoptions").path(0).has("institutionName") && current.path("adoptions").path(0).has("adoptedAt")).isTrue();
        assertThat(current.path("capabilities").path("canCancelAdoption").asBoolean()).isTrue();
        var agendas = call("GET", "/api/v1/officer/agendas?scope=ADOPTED", "owner", null, null);
        assertThat(agendas.statusCode()).withFailMessage(agendas.body()).isEqualTo(200);
        assertThat(JSON.readTree(agendas.body()).path("data").path(0).path("postId").asLong()).isEqualTo(id);
        admin().update("update discushion.institution_credentials set valid_until=? where id=?", Timestamp.from(NOW.minusSeconds(1)), credential);
        var expired = data(get(id, "owner")); assertThat(expired.path("author").path("institutionVerified").asBoolean()).isFalse();
        assertThat(expired.path("capabilities").path("canCancelAdoption").asBoolean()).isFalse();
        admin().update("update discushion.institution_agenda_adoptions set canceled_at=?,canceled_by_user_id=? where id=?", Timestamp.from(NOW.plusSeconds(1)), owner, adoption);
        assertThat(data(get(id, "other")).path("adoptions").size()).isZero();
    }

    @Test void deniesDeletedSourcesAndRegistrationFailuresWithoutLeakingBody() throws Exception {
        long id = create("LOCAL_AGENDA", Map.of(), List.of()); String token = share(id);
        admin().update("update discushion.posts set status='DELETED',deleted_at=? where id=?", Timestamp.from(NOW), id);
        var denied = get(id, "owner"); assertThat(denied.statusCode()).isEqualTo(404); assertThat(denied.body()).doesNotContain(marker);
        assertThat(call("GET", "/api/v1/posts/" + id, null, null, token).statusCode()).isEqualTo(404);
        assertThat(get(9007199254740991L, "owner").statusCode()).isEqualTo(404);
        assertThat(call("GET", "/api/v1/posts/9007199254740992", "owner", null, null).statusCode()).isEqualTo(400);
        assertThat(call("GET", "/api/v1/posts/1?userId=" + other, "owner", null, null).statusCode()).isEqualTo(400);
        assertThat(get(id, "unregistered").statusCode()).isEqualTo(403);
        admin().update("update discushion.users set registration_completed_at=null where id=?", other);
        assertThat(get(id, "other").statusCode()).isEqualTo(403);
    }

    @Test void unverifiedMemberCanReadAndBookmarkButCannotParticipate() throws Exception {
        long id = create("LOCAL_AGENDA", Map.of(), List.of());
        admin().update("delete from discushion.neighbor_verified_regions where user_id=?", other);
        var caps = data(get(id, "other")).path("capabilities");
        assertThat(caps.path("canComment").asBoolean()).isFalse(); assertThat(caps.path("canReact").asBoolean()).isFalse();
        assertThat(caps.path("canBookmark").asBoolean()).isTrue(); assertThat(caps.path("canEdit").asBoolean()).isFalse();
    }

    @Test void malformedVoteFailsSafelyAndEndedVoteCannotBeEdited() throws Exception {
        long id = create("VOTE", Map.of("question", "질문", "options", List.of("찬성", "반대"), "endsAt", NOW.plusSeconds(864000).toString()), List.of());
        admin().update("update discushion.polls set ends_at=? where post_id=?", Timestamp.from(NOW), id);
        var closed = data(get(id, "owner")); assertThat(closed.path("vote").path("status").asString()).isEqualTo("CLOSED");
        assertThat(closed.path("capabilities").path("canEdit").asBoolean()).isFalse();
        admin().update("delete from discushion.poll_options where poll_id in(select id from discushion.polls where post_id=?)", id);
        admin().update("delete from discushion.polls where post_id=?", id);
        var failure = get(id, "owner"); assertThat(failure.statusCode()).isEqualTo(500);
        assertThat(failure.body()).contains("INTERNAL_ERROR").doesNotContain("select ", "discushion.", marker);
    }

    @Test void batchSourcesAreRealAndOmitDeletedContentAndGuestState() throws Exception {
        long alive = create("LOCAL_AGENDA", Map.of(), List.of()); long deleted = create("LOCAL_AGENDA", Map.of(), List.of());
        admin().update("update discushion.posts set status='DELETED',deleted_at=? where id=?", Timestamp.from(NOW), deleted);
        var tx = new TransactionTemplate(manager);
        tx.executeWithoutResult(status -> {
            var snapshots = participation.findAll(Set.of(alive, deleted, 9007199254740991L), OptionalLong.empty());
            assertThat(snapshots.keySet()).containsExactly(alive); assertThat(snapshots.get(alive).viewer()).isEmpty();
            var posts = summaries.findAll(Set.of(alive, deleted, 9007199254740991L));
            assertThat(posts.keySet()).containsExactlyInAnyOrder(alive, deleted); assertThat(posts.get(deleted).display()).isEmpty();
        });
        assertThatThrownBy(() -> participation.findAll(Set.of(alive), OptionalLong.empty())).isInstanceOf(IllegalStateException.class);
    }

    @Test void lookupReadsUncommittedWriteAndRollsBackWithCallerTransaction() throws Exception {
        long id = create("LOCAL_AGENDA", Map.of(), List.of());
        var result = call("POST", "/api/v1/synthetic/detail15/" + id, "owner", "{}", null);
        assertThat(data(result).path("title").asString()).isEqualTo("synthetic-updated");
        assertThat(data(get(id, "owner")).path("title").asString()).isEqualTo("synthetic-updated");
        var failed = call("POST", "/api/v1/synthetic/detail15/" + id + "?rollback=true", "owner", "{}", null);
        assertThat(failed.statusCode()).isEqualTo(500);
        assertThat(data(get(id, "owner")).path("title").asString()).isEqualTo("synthetic-updated");
    }

    @org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods=false)
    static class ProbeWiring {
        @org.springframework.context.annotation.Bean
        WriteProbe detailWriteProbe(PostDetailLookup lookup, com.discushion.identity.MemberAuthorization members,
                DataSource source, PlatformTransactionManager manager) {
            return new WriteProbe(lookup, members, source, manager);
        }
    }
    @org.springframework.web.bind.annotation.RestController
    static class WriteProbe {
        final PostDetailLookup lookup;
        final com.discushion.identity.MemberAuthorization members;
        final JdbcTemplate jdbc;
        final TransactionTemplate writes;
        WriteProbe(PostDetailLookup lookup, com.discushion.identity.MemberAuthorization members, DataSource source, PlatformTransactionManager manager) {
            this.lookup=lookup;this.members=members;this.jdbc=new JdbcTemplate(source);this.writes=new TransactionTemplate(manager);
        }
        @org.springframework.web.bind.annotation.PostMapping("/api/v1/synthetic/detail15/{id}")
        Map<String,Object> write(@org.springframework.web.bind.annotation.PathVariable("id") long id,
                @org.springframework.web.bind.annotation.RequestParam(name="rollback",defaultValue="false") boolean rollback) {
            return writes.execute(status -> {
                var member=members.lockCurrentCompletedMember();
                jdbc.query("select id from discushion.posts where id=? and author_user_id=? for update", (rs,row)->rs.getLong(1),id,member.userId());
                jdbc.update("update discushion.posts set title=? where id=? and author_user_id=?",rollback?"synthetic-rollback":"synthetic-updated",id,member.userId());
                var data=lookup.read(id,member.userId());
                if(rollback)throw new IllegalStateException("synthetic caller rollback");
                return Map.of("data",data);
            });
        }
    }

    @Test void postShareLockSerializesDeletionAndNextDetailReturns404() throws Exception {
        long id = create("LOCAL_AGENDA", Map.of(), List.of());
        var locked = new CountDownLatch(1); var release = new CountDownLatch(1); var executor = Executors.newFixedThreadPool(2);
        try {
            var reading = executor.submit(() -> new TransactionTemplate(manager).execute(status -> {
                new JdbcPostDetailStore(source).lock(id); locked.countDown();
                try { if (!release.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Read lock timed out"); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
                return new JdbcPostDetailStore(source).base(id, NOW).content();
            }));
            assertThat(locked.await(5, TimeUnit.SECONDS)).isTrue();
            var deleting = executor.submit(() -> admin().update("update discushion.posts set status='DELETED',deleted_at=? where id=?", Timestamp.from(NOW), id));
            assertThatThrownBy(() -> deleting.get(200, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
            release.countDown(); assertThat(reading.get(5, TimeUnit.SECONDS)).isEqualTo("본문");
            assertThat(deleting.get(5, TimeUnit.SECONDS)).isEqualTo(1); assertThat(get(id, "owner").statusCode()).isEqualTo(404);
        } finally { release.countDown(); executor.shutdownNow(); }
    }

    long photo() {
        return admin().queryForObject("insert into discushion.media_files(owner_user_id,storage_key,original_name,mime_type,size_bytes,purpose,created_at,lifecycle_status,uploaded_at) values(?,?,'synthetic.png','image/png',100,'POST_PHOTO',clock_timestamp()-interval '1 minute','UNLINKED',clock_timestamp()-interval '30 seconds') returning id", Long.class, owner, marker + "/photo");
    }
    long create(String type, Map<String, Object> details, List<Long> photos) throws Exception {
        var body = new LinkedHashMap<String, Object>(); body.put("type", type); body.put("topic", "OTHER"); body.put("regionId", region);
        body.put("title", marker); body.put("content", "본문"); body.put("photoFileIds", photos); if (!details.isEmpty()) body.put("details", details);
        var result = call("POST", "/api/v1/posts", "owner", JSON.writeValueAsString(body), null);
        assertThat(result.statusCode()).withFailMessage(result.body()).isEqualTo(201); return JSON.readTree(result.body()).path("data").path("postId").asLong();
    }
    String subject(String suffix) { return "did:privy:" + marker + "-" + suffix; }
    String bearer(String suffix) throws Exception {
        var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.ES256).type(JOSEObjectType.JWT).build(), new JWTClaimsSet.Builder()
                .issuer("privy.io").audience("synthetic-post14-app").subject(subject(suffix)).issueTime(Date.from(NOW.minusSeconds(60)))
                .expirationTime(Date.from(NOW.plusSeconds(3600))).claim("sid", "synthetic-detail15-session").build());
        jwt.sign(new ECDSASigner(PostCreationHttpIntegrationTests.KEY)); return jwt.serialize();
    }
    String share(long id) throws Exception {
        var response = call("GET", "/api/v1/posts/" + id + "/share-link", "owner", null, null);
        assertThat(response.statusCode()).withFailMessage(response.body()).isEqualTo(200);
        return URI.create(JSON.readTree(response.body()).path("data").path("shareUrl").asString()).getRawQuery().substring("token=".length());
    }
    HttpResponse<String> get(long id, String viewer) throws Exception { return call("GET", "/api/v1/posts/" + id, viewer, null, null); }
    JsonNode data(HttpResponse<String> response) {
        assertThat(response.statusCode()).withFailMessage(response.body()).isEqualTo(200); return JSON.readTree(response.body()).path("data");
    }
    HttpResponse<String> call(String method, String path, String viewer, String body, String share) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).timeout(Duration.ofSeconds(15));
        if (viewer != null) request.header("Authorization", "Bearer " + (viewer.equals("invalid") ? "invalid" : bearer(viewer)));
        if (share != null) request.header("X-Post-Share-Token", share);
        if (body != null) request.header("Content-Type", "application/json");
        return HttpClient.newHttpClient().send(request.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }
}
