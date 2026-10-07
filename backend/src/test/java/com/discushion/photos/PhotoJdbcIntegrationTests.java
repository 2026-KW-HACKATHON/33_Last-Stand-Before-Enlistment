package com.discushion.photos;

import java.io.*;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import com.discushion.contracts.identity.*;
import com.discushion.identity.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static org.assertj.core.api.Assertions.*;
import static com.discushion.photos.PhotoFailure.Reason.*;
import static com.discushion.photos.PhotoContentTests.assertReason;

@EnabledIfEnvironmentVariable(named="DISCUSHION_TEST_JDBC_URL",matches="jdbc:postgresql://127\\.0\\.0\\.1:55432/discushion_migration_test(?:\\?.*)?")
class PhotoJdbcIntegrationTests {
    private final DriverManagerDataSource source=new DriverManagerDataSource(System.getenv("DISCUSHION_TEST_JDBC_URL"),"postgres",System.getenv("DISCUSHION_TEST_DB_PASSWORD"));
    private final JdbcPhotoStore store=new JdbcPhotoStore(source);
    private final TransactionTemplate tx=new TransactionTemplate(new DataSourceTransactionManager(source));
    private final TestClock clock=new TestClock();
    private final Storage storage=new Storage();
    private VerifiedActor actor;
    private final MemberAuthorization authorization=new MemberAuthorization(()->Optional.ofNullable(actor),new JdbcMemberStore(source,clock),clock);
    private final PhotoService service=new PhotoService(store,authorization,storage,tx,clock);
    private final PhotoCleanup cleanup=new PhotoCleanup(store,storage,tx,clock,Duration.ofMinutes(2));
    private final PhotoAttachments attachments=new PhotoAttachments(store,authorization,clock);
    private long owner,region;
    private String marker;
    @BeforeEach void setup() {
        marker="synthetic-photo13-"+UUID.randomUUID();
        String subject="did:privy:"+marker;
        owner=store.jdbc.queryForObject("""
            insert into discushion.users(email,email_verified_at,created_at,updated_at,privy_user_id,registration_completed_at)
            values(?,?::timestamptz,?::timestamptz,?::timestamptz,?,?::timestamptz) returning id
            """,Long.class,marker+"@example.invalid",clock.instant().toString(),clock.instant().toString(),clock.instant().toString(),subject,clock.instant().toString());
        actor=new VerifiedActor(subject,Optional.of(new LocalMember(owner,Optional.of(clock.instant()))));
        region=store.jdbc.queryForObject("insert into discushion.regions(name) values(?) returning id",Long.class,marker);
        store.jdbc.update("insert into discushion.neighbor_verified_regions(user_id,region_id,verified_at) values(?,?,?)",owner,region,Timestamp.from(clock.instant()));
    }
    @AfterEach void cleanupFixtures() {
        tx.executeWithoutResult(status->{
            store.jdbc.update("delete from discushion.post_photos where post_id in (select id from discushion.posts where author_user_id=?)",owner);
            store.jdbc.update("delete from discushion.polls where post_id in (select id from discushion.posts where author_user_id=?)",owner);
            store.jdbc.update("delete from discushion.posts where author_user_id=?",owner);
            store.jdbc.update("delete from discushion.media_files where owner_user_id=?",owner);
            store.jdbc.update("delete from discushion.neighbor_verified_regions where user_id=?",owner);
            store.jdbc.update("delete from discushion.users where id=? and privy_user_id=?",owner,"did:privy:"+marker);
            store.jdbc.update("delete from discushion.regions where id=? and name=?",region,marker);
        });
    }
    private PhotoService.Reservation reserve() {return service.reserve("photo.png","image/png",1000);}
    private long completed() throws Exception {
        var reservation=reserve(); var file=store.find(reservation.fileId(),false).orElseThrow();
        storage.objects.put(file.key(),PhotoContentTests.png()); service.complete(file.id()); return file.id();
    }
    private long post() {
        return store.jdbc.queryForObject("""
            insert into discushion.posts(author_user_id,region_id,type,topic,title,content,status,content_revision,created_at,updated_at)
            values(?,?,'LOCAL_AGENDA','OTHER','synthetic','synthetic','PUBLISHED',1,?,?) returning id
            """,Long.class,owner,region,Timestamp.from(clock.instant()),Timestamp.from(clock.instant()));
    }
    @Test void commitsReservationBeforeProviderAndPreservesBoundOnIssuanceFailure() {
        var reservation=reserve();
        assertThat(storage.issued).isEqualTo(1);
        assertThat(service.get(reservation.fileId()).contentType()).isNull();
        storage.failIssue=true;
        assertReason(this::reserve,PHOTO_STORAGE_UNAVAILABLE);
        assertThat(store.jdbc.queryForObject("select count(*) from discushion.media_files where owner_user_id=? and lifecycle_status='DELETE_PENDING' and upload_authorization_expires_at is not null",Integer.class,owner)).isEqualTo(1);
    }
    @Test void completeChecksActualObjectAndDoesNotExtendFirstCompletion() throws Exception {
        long id=completed(); var first=service.get(id);
        assertThat(first.status()).isEqualTo("UNLINKED"); assertThat(first.canAttach()).isTrue();
        assertThat(first.sizeBytes()).isEqualTo(PhotoContentTests.png().length);
        clock.now=clock.now.plusSeconds(100); var again=service.complete(id);
        assertThat(again.uploadedAt()).isEqualTo(first.uploadedAt()); assertThat(again.linkExpiresAt()).isEqualTo(first.linkExpiresAt());
        clock.now=first.linkExpiresAt(); assertReason(()->service.complete(id),PHOTO_UPLOAD_EXPIRED);
        assertThat(service.get(id).canAttach()).isFalse();
    }
    @Test void missingObjectOutageAndInvalidImageAreDifferentAndFailureIntentCommits() {
        var r=reserve(); assertReason(()->service.complete(r.fileId()),PHOTO_UPLOAD_NOT_READY);
        storage.failRead=true; assertReason(()->service.complete(r.fileId()),PHOTO_STORAGE_UNAVAILABLE);
        assertThat(service.get(r.fileId()).status()).isEqualTo("UPLOADING");
        storage.failRead=false; storage.objects.put(store.find(r.fileId(),false).orElseThrow().key(),"fake png".getBytes());
        assertReason(()->service.complete(r.fileId()),PHOTO_FORMAT_UNSUPPORTED);
        assertThat(service.get(r.fileId()).status()).isEqualTo("DELETE_PENDING");
    }
    @Test void otherOwnerAndMissingFileAreIndistinguishableAndActorSnapshotCannotGrantSignup() {
        var r=reserve();
        store.jdbc.update("update discushion.media_files set purpose='PROFILE_IMAGE' where id=?",r.fileId());
        assertReason(()->service.get(r.fileId()),PHOTO_UPLOAD_NOT_FOUND);
        assertReason(()->service.get(9007199254740991L),PHOTO_UPLOAD_NOT_FOUND);
        store.jdbc.update("update discushion.users set registration_completed_at=null where id=?",owner);
        assertThatThrownBy(this::reserve).isInstanceOfSatisfying(IdentityFailure.class,error->assertThat(error.reason()).isEqualTo(IdentityFailure.Reason.INCOMPLETE));
    }
    @Test void quotaIncludesCancelledAndUnknownReservationsAndCancelDoesNotReturnSlot() {
        for(int index=0;index<20;index++) {var r=reserve();service.cancel(r.fileId());}
        assertReason(this::reserve,PHOTO_UPLOAD_QUOTA_EXCEEDED);
        clock.now=clock.now.plusSeconds(61);
        assertReason(this::reserve,PHOTO_UPLOAD_QUOTA_EXCEEDED);
    }
    @Test void cancelAndWorkerNeverMarkFinalWhileAuthorizationOrInflightUploadsRemain() {
        var r=reserve(); String key=store.find(r.fileId(),false).orElseThrow().key(); storage.objects.put(key,new byte[]{1});
        assertThat(service.cancel(r.fileId()).status()).isEqualTo("DELETE_PENDING");
        cleanup.runBatch(20); assertThat(storage.objects).doesNotContainKey(key);
        assertThat(service.get(r.fileId()).deletionCompleted()).isFalse();
        clock.now=clock.now.plusSeconds(8000); storage.drained=false; storage.objects.put(key,new byte[]{2});
        cleanup.runBatch(20); assertThat(service.get(r.fileId()).deletionCompleted()).isFalse();
        clock.now=clock.now.plusSeconds(4000); storage.drained=true; cleanup.runBatch(20);
        assertThat(service.get(r.fileId()).deletionCompleted()).isTrue();
        assertThat(service.cancel(r.fileId()).deletionCompleted()).isTrue();
    }
    @Test void expiredUploadingIsCleanedButLinkedFilesAreProtected() throws Exception {
        var expired=reserve(); long linked=completed(); long post=post();
        tx.executeWithoutResult(status->attachments.replace(post,List.of(new PhotoAttachments.Reference(null,linked))));
        clock.now=clock.now.plusSeconds(86400); cleanup.runBatch(20);
        assertThat(service.get(expired.fileId()).deletionCompleted()).isTrue();
        assertThat(service.get(linked).status()).isEqualTo("LINKED");
        assertReason(()->service.cancel(linked),PHOTO_ALREADY_LINKED);
    }
    @Test void linkOrderPhotoIdsAndRemovalAreAtomicAndRollbackDoesNotDeleteStorage() throws Exception {
        long a=completed(),b=completed(),post=post();
        tx.executeWithoutResult(status->attachments.replace(post,List.of(new PhotoAttachments.Reference(null,a),new PhotoAttachments.Reference(null,b))));
        var photos=store.jdbc.queryForList("select id from discushion.post_photos where post_id=? order by sort_order",Long.class,post);
        tx.executeWithoutResult(status->attachments.replace(post,List.of(new PhotoAttachments.Reference(photos.get(1),null),new PhotoAttachments.Reference(photos.get(0),null))));
        assertThat(store.jdbc.queryForList("select file_id from discushion.post_photos where post_id=? order by sort_order",Long.class,post)).containsExactly(b,a);
        tx.executeWithoutResult(status->{attachments.replace(post,List.of());status.setRollbackOnly();});
        assertThat(service.get(a).status()).isEqualTo("LINKED"); assertThat(storage.removed).isZero();
        var removed=tx.execute(status->attachments.replace(post,List.of()));
        assertThat(removed).containsExactly(a,b); assertThat(service.get(a).status()).isEqualTo("DELETE_PENDING");
    }
    @Test void duplicateReferencesAndTotalLimitsLeavePreviousStateUntouched() throws Exception {
        long id=completed(),post=post();
        assertReason(()->tx.execute(status->attachments.replace(post,List.of(new PhotoAttachments.Reference(null,id),new PhotoAttachments.Reference(null,id)))),VALIDATION_ERROR);
        store.jdbc.update("update discushion.media_files set size_bytes=10000001 where id=?",id);
        assertReason(()->tx.execute(status->attachments.replace(post,List.of(new PhotoAttachments.Reference(null,id)))),PHOTO_SIZE_EXCEEDED);
        assertThat(service.get(id).status()).isEqualTo("UNLINKED");
    }
    @Test void staleClaimCannotFinalizeOrReleaseNewWorkersClaim() {
        var r=reserve();service.cancel(r.fileId()); UUID old=UUID.randomUUID(),current=UUID.randomUUID();
        tx.executeWithoutResult(status->{store.find(r.fileId(),true);store.claim(r.fileId(),current,clock.instant(),clock.instant().plusSeconds(120));});
        Boolean accepted=tx.execute(status->store.finish(r.fileId(),old,clock.instant(),false,"PHOTO_STORAGE_UNAVAILABLE",clock.instant().plusSeconds(60)));
        assertThat(accepted).isFalse();
        assertThat(store.find(r.fileId(),false).orElseThrow().claim()).isEqualTo(current);
        assertThat(cleanup.runBatch(20)).isZero();
    }
    @Test void rateLimitIncludesReleasedLinkedReservationsAndReturnsRetryAfter() throws Exception {
        long post=post();
        for(int index=0;index<20;index++) {
            var r=reserve();
            store.jdbc.update("update discushion.media_files set lifecycle_status='LINKED',uploaded_at=?,linked_at=? where id=?",Timestamp.from(clock.instant()),Timestamp.from(clock.instant()),r.fileId());
            store.jdbc.update("insert into discushion.post_photos(post_id,file_id,sort_order) values(?,?,?)",post,r.fileId(),index);
        }
        assertThatThrownBy(this::reserve).isInstanceOfSatisfying(PhotoFailure.class,error->{
            assertThat(error.reason()).isEqualTo(PHOTO_UPLOAD_RATE_LIMITED);assertThat(error.retryAfter()).isEqualTo(60);
        });
        clock.now=clock.now.plusSeconds(60); assertThat(reserve().status()).isEqualTo("UPLOADING");
    }
    @Test void concurrentReservationsCannotBypassTwentySlotQuota() throws Exception {
        for(int index=0;index<19;index++) reserve();
        var start=new java.util.concurrent.CountDownLatch(1);
        var pool=java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            java.util.concurrent.Callable<String> attempt=()-> {start.await();try {reserve();return "OK";}catch(PhotoFailure failure){return failure.reason().name();}};
            var first=pool.submit(attempt);var second=pool.submit(attempt);start.countDown();
            assertThat(List.of(first.get(10,java.util.concurrent.TimeUnit.SECONDS),second.get(10,java.util.concurrent.TimeUnit.SECONDS)))
                .containsExactlyInAnyOrder("OK","PHOTO_UPLOAD_QUOTA_EXCEEDED");
            assertThat(store.jdbc.queryForObject("select count(*) from discushion.media_files where owner_user_id=?",Integer.class,owner)).isEqualTo(20);
        } finally {pool.shutdownNow();}
    }
    @Test void cancellationWhileVerifyingCannotResurrectFile() throws Exception {
        var r=reserve();storage.objects.put(store.find(r.fileId(),false).orElseThrow().key(),PhotoContentTests.png());
        storage.afterOpen=()->service.cancel(r.fileId());
        assertReason(()->service.complete(r.fileId()),PHOTO_DELETION_PENDING);
        assertThat(service.get(r.fileId()).status()).isEqualTo("DELETE_PENDING");
    }
    @Test void cancellationDuringGrantPreventsReturningCapabilityAndExtendsActualBoundFirst() {
        storage.grantSeconds=8000;
        storage.duringIssue=()-> {
            long id=store.jdbc.queryForObject("select id from discushion.media_files where owner_user_id=?",Long.class,owner);
            service.cancel(id);
        };
        assertReason(this::reserve,PHOTO_STORAGE_UNAVAILABLE);
        long id=store.jdbc.queryForObject("select id from discushion.media_files where owner_user_id=?",Long.class,owner);
        assertThat(service.get(id).status()).isEqualTo("DELETE_PENDING");
        assertThat(service.get(id).uploadAuthorizationExpiresAt()).isEqualTo(clock.instant().plusSeconds(8000));
    }
    @Test void pollEndingWhileWaitingForFileLockRejectsAllPhotoWrites() throws Exception {
        long file=completed(),post=post();
        Instant end=clock.instant().plusSeconds(10);
        store.jdbc.update("insert into discushion.polls(post_id,question,ends_at) values(?,?,?)",post,"synthetic",Timestamp.from(end));
        var pool=java.util.concurrent.Executors.newSingleThreadExecutor();
        try(var blocker=source.getConnection()) {
            blocker.setAutoCommit(false);
            try(var statement=blocker.prepareStatement("select id from discushion.media_files where id=? for update")) {
                statement.setLong(1,file);statement.executeQuery().close();
            }
            var waiting=pool.submit(()->tx.execute(status->attachments.replace(post,List.of(new PhotoAttachments.Reference(null,file)))));
            long deadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(10);
            boolean observed=false;
            while(System.nanoTime()<deadline) {
                observed=Boolean.TRUE.equals(store.jdbc.queryForObject("select exists(select 1 from pg_stat_activity where datname=current_database() and wait_event_type='Lock' and query like '%media_files%' and pid<>pg_backend_pid())",Boolean.class));
                if(observed) break;
                Thread.sleep(20);
            }
            assertThat(observed).as("actual media row lock wait").isTrue();
            clock.now=end;blocker.rollback();
            assertThatThrownBy(()->waiting.get(10,java.util.concurrent.TimeUnit.SECONDS))
                .hasCauseInstanceOf(PhotoFailure.class).hasRootCauseMessage("VALIDATION_ERROR");
            assertThat(store.jdbc.queryForObject("select count(*) from discushion.post_photos where post_id=?",Integer.class,post)).isZero();
            assertThat(service.get(file).status()).isEqualTo("UNLINKED");
        } finally {pool.shutdownNow();}
    }
    final class Storage implements PhotoStorage {
        final Map<String,byte[]> objects=new HashMap<>();
        boolean failIssue,failRead,drained=true; int issued,removed; long grantSeconds=7200;
        Runnable afterOpen,duringIssue;
        public Instant authorizationUpperBound(Instant now) {return now.plusSeconds(7300);}
        public Upload createUpload(String key,String mime) {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            assertThat(store.jdbc.queryForObject("select count(*) from discushion.media_files where storage_key=? and upload_authorization_expires_at is not null",Integer.class,key)).isEqualTo(1);
            if(failIssue) throw new IllegalStateException("synthetic-secret");issued++;
            if(duringIssue!=null) duringIssue.run();
            return new Upload("https://example.invalid/upload","PUT","RAW",Map.of("Content-Type",mime,"x-upsert","false"),clock.instant().plusSeconds(grantSeconds));
        }
        public Optional<InputStream> open(String key) {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            if(failRead) throw new PhotoFailure(PHOTO_STORAGE_UNAVAILABLE);
            if(afterOpen!=null) afterOpen.run();
            return Optional.ofNullable(objects.get(key)).map(ByteArrayInputStream::new);
        }
        public void remove(String key) {assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();objects.remove(key);removed++;}
        public String publicUrl(String key) {return "https://example.invalid/"+key;}
        public boolean uploadsDrained(String key,Instant expiry) {return drained;}
    }
    static final class TestClock extends Clock {
        volatile Instant now=Instant.parse("2026-10-08T00:00:00Z");
        public ZoneId getZone(){return ZoneOffset.UTC;} public Clock withZone(ZoneId zone){return this;} public Instant instant(){return now;}
    }
}
