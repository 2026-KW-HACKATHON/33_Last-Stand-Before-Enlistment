package com.discushion.photos;

import java.io.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import com.discushion.contracts.identity.*;
import com.discushion.identity.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.support.*;
import static org.assertj.core.api.Assertions.*;
import static com.discushion.photos.PhotoFailure.Reason.*;
import static com.discushion.photos.PhotoContentTests.assertReason;

@EnabledIfEnvironmentVariable(named="DISCUSHION_TEST_JDBC_URL",matches="jdbc:postgresql://127\\.0\\.0\\.1:55432/discushion_migration_test(?:\\?.*)?")
class PhotoRelayJdbcIntegrationTests {
    final DriverManagerDataSource source=new DriverManagerDataSource(System.getenv("DISCUSHION_TEST_JDBC_URL"),"postgres",System.getenv("DISCUSHION_TEST_DB_PASSWORD"));
    final JdbcPhotoStore store=new JdbcPhotoStore(source);
    final TransactionTemplate tx=new TransactionTemplate(new DataSourceTransactionManager(source));
    final Clock clock=new PhotoDatabaseClock(store);
    final Storage storage=new Storage();
    VerifiedActor actor;
    final MemberAuthorization auth=new MemberAuthorization(()->Optional.ofNullable(actor),new JdbcMemberStore(source,clock),clock);
    final PhotoService service=new PhotoService(store,auth,storage,tx,clock,true);
    final PhotoCleanup cleanup=new PhotoCleanup(store,storage,tx,clock,Duration.ofMinutes(2));
    long owner,other;
    byte[] png;
    @BeforeEach void prepare() throws Exception {
        png=PhotoContentTests.png();owner=user();other=user();actor=actor(owner);
    }
    long user() {
        String marker="synthetic-photo-relay-"+UUID.randomUUID();
        return store.jdbc.queryForObject("""
            insert into discushion.users(email,email_verified_at,created_at,updated_at,privy_user_id,registration_completed_at)
            values(?,clock_timestamp(),clock_timestamp(),clock_timestamp(),?,clock_timestamp()) returning id
            """,Long.class,marker+"@example.invalid","did:privy:"+marker);
    }
    VerifiedActor actor(long id) {
        return new VerifiedActor(store.jdbc.queryForObject("select privy_user_id from discushion.users where id=?",String.class,id),
            Optional.of(new LocalMember(id,Optional.of(clock.instant()))));
    }
    @AfterEach void remove() {
        store.jdbc.update("delete from discushion.media_files where owner_user_id in (?,?)",owner,other);
        store.jdbc.update("delete from discushion.users where id in (?,?)",owner,other);
    }
    PhotoService.Reservation reserve(){return service.reserve("test.png","image/png",png.length);}
    PhotoView upload(long id){return service.upload(id,"image/png",new ByteArrayInputStream(png));}
    String key(long id){return store.find(id,false).orElseThrow().key();}
    @Test void relayHasNoExternalGrantAndCanFinallyDeleteBeforeAdmissionDeadline() {
        var r=reserve();
        assertThat(r.upload().url()).isEqualTo("/api/v1/photo-uploads/"+r.fileId()+"/content");
        assertThat(r.upload().headers()).containsExactlyEntriesOf(Map.of("Content-Type","image/png"));
        assertThat(Duration.between(r.createdAt(),r.upload().expiresAt())).isEqualTo(Duration.ofHours(2));
        assertReason(()->service.complete(r.fileId()),PHOTO_UPLOAD_NOT_READY);
        assertThat(upload(r.fileId()).status()).isEqualTo("UPLOADING");
        assertThat(service.complete(r.fileId()).status()).isEqualTo("UNLINKED");
        assertReason(()->upload(r.fileId()),PHOTO_UPLOAD_NOT_READY);
        service.cancel(r.fileId());cleanup.process(r.fileId());
        assertThat(service.get(r.fileId()).deletionCompleted()).isTrue();
        assertThat(service.get(r.fileId()).deletedAt()).isBefore(r.upload().expiresAt());
        assertReason(()->upload(r.fileId()),PHOTO_DELETION_PENDING);
        assertThat(storage.objects).doesNotContainKey(key(r.fileId()));assertThat(storage.writes).isEqualTo(1);
    }
    @Test void neverStartedRelayReservationCanBeDeletedImmediately() {
        var r=reserve();service.cancel(r.fileId());cleanup.process(r.fileId());
        assertThat(service.get(r.fileId()).deletionCompleted()).isTrue();assertThat(storage.writes).isZero();
    }
    @Test void cancellationDuringWriteWaitsForAcknowledgementThenRemovesLateObject() {
        var r=reserve();
        storage.duringWrite=()-> {
            service.cancel(r.fileId());cleanup.process(r.fileId());
            assertThat(service.get(r.fileId()).deletionCompleted()).isFalse();
            assertThat(store.find(r.fileId(),false).orElseThrow().uploadAttemptStatus()).isEqualTo("RUNNING");
        };
        assertReason(()->upload(r.fileId()),PHOTO_DELETION_PENDING);
        assertThat(storage.objects).containsKey(key(r.fileId()));
        assertThat(store.find(r.fileId(),false).orElseThrow().uploadAttemptStatus()).isEqualTo("ACKNOWLEDGED");
        cleanup.process(r.fileId());assertThat(service.get(r.fileId()).deletionCompleted()).isTrue();
        assertThat(storage.objects).isEmpty();
    }
    @Test void lostWriteResponseRemainsUnknownAcrossExpiryCleanupAndLateRecreation() {
        var r=reserve();storage.loseResponse=true;
        assertReason(()->upload(r.fileId()),PHOTO_STORAGE_UNAVAILABLE);
        assertThat(store.find(r.fileId(),false).orElseThrow().uploadAttemptStatus()).isEqualTo("UNKNOWN");
        store.jdbc.update("update discushion.media_files set created_at=created_at-interval '3 hours',upload_authorization_expires_at=created_at-interval '1 hour' where id=?",r.fileId());
        cleanup.process(r.fileId());assertThat(service.get(r.fileId()).deletionCompleted()).isFalse();
        storage.objects.put(key(r.fileId()),png);cleanup.process(r.fileId());
        assertThat(storage.objects).isEmpty();assertThat(service.get(r.fileId()).deletionCompleted()).isFalse();
        assertReason(()->upload(r.fileId()),PHOTO_DELETION_PENDING);assertThat(storage.writes).isEqualTo(1);
        assertThatThrownBy(()->store.jdbc.update("update discushion.media_files set upload_attempt_id=null,upload_attempt_status=null,upload_attempt_started_at=null where id=?",r.fileId()))
            .hasRootCauseInstanceOf(java.sql.SQLException.class);
        assertThatThrownBy(()->store.jdbc.update("update discushion.media_files set lifecycle_status='DELETED',deleted_at=clock_timestamp() where id=?",r.fileId()))
            .hasRootCauseInstanceOf(java.sql.SQLException.class);
    }
    @Test void crashAfterStartingAttemptIsNotClearedByCleanupOrLeaseRecovery() {
        var r=reserve();tx.executeWithoutResult(status->{store.find(r.fileId(),true);store.startUpload(r.fileId(),UUID.randomUUID(),clock.instant());});
        service.cancel(r.fileId());cleanup.process(r.fileId());cleanup.process(r.fileId());
        assertThat(service.get(r.fileId()).deletionCompleted()).isFalse();
        assertThat(store.find(r.fileId(),false).orElseThrow().uploadAttemptStatus()).isEqualTo("RUNNING");
        assertThat(storage.writes).isZero();
    }
    @Test void wrongOwnerInvalidBodyAndExpiryDoNotDispatchStorageWrites() {
        var r=reserve();actor=actor(other);assertReason(()->upload(r.fileId()),PHOTO_UPLOAD_NOT_FOUND);actor=actor(owner);
        assertReason(()->service.upload(r.fileId(),"image/jpeg",new ByteArrayInputStream(png)),PHOTO_FORMAT_UNSUPPORTED);
        assertReason(()->service.upload(r.fileId(),"image/png",new ByteArrayInputStream(new byte[1])),VALIDATION_ERROR);
        assertReason(()->service.upload(r.fileId(),"image/png",new ByteArrayInputStream(new byte[PhotoContent.MAX_BYTES+1])),PHOTO_SIZE_EXCEEDED);
        store.jdbc.update("update discushion.media_files set created_at=created_at-interval '3 hours',upload_authorization_expires_at=created_at-interval '1 hour' where id=?",r.fileId());
        assertReason(()->upload(r.fileId()),PHOTO_UPLOAD_EXPIRED);assertThat(storage.writes).isZero();
        assertThat(store.find(r.fileId(),false).orElseThrow().uploadAttemptStatus()).isNull();
    }
    @Test void directReservationsCannotBeRelabelledAsRelaySafe() {
        long id=tx.execute(status->store.reserve(owner,"a.png","image/png",png.length,clock.instant(),clock.instant().plusSeconds(7200)).id());
        assertReason(()->upload(id),PHOTO_UPLOAD_NOT_FOUND);
        assertThatThrownBy(()->store.jdbc.update("update discushion.media_files set upload_transport='SERVER_RELAY' where id=?",id))
            .hasRootCauseInstanceOf(java.sql.SQLException.class);
    }
    @Test void secondUploadWhileFirstWriteIsRunningCannotDispatchAgain() {
        var r=reserve();
        storage.duringWrite=()->assertReason(()->upload(r.fileId()),PHOTO_UPLOAD_NOT_READY);
        upload(r.fileId());
        assertThat(storage.writes).isEqualTo(1);
        assertThat(store.find(r.fileId(),false).orElseThrow().uploadAttemptStatus()).isEqualTo("ACKNOWLEDGED");
    }
    @Test void acknowledgedUnlinkedFileExpiresFromFirstCompletionNotReservation() {
        var r=reserve();upload(r.fileId());service.complete(r.fileId());
        store.jdbc.update("update discushion.media_files set created_at=created_at-interval '25 hours' where id=?",r.fileId());
        cleanup.runBatch(20);
        assertThat(service.get(r.fileId()).status()).isEqualTo("UNLINKED");
        assertThat(service.complete(r.fileId()).uploadedAt()).isEqualTo(service.get(r.fileId()).uploadedAt());
        store.jdbc.update("update discushion.media_files set uploaded_at=uploaded_at-interval '24 hours' where id=?",r.fileId());
        cleanup.runBatch(20);
        assertThat(service.get(r.fileId()).deletionCompleted()).isTrue();
    }
    final class Storage implements PhotoStorage {
        final Map<String,byte[]> objects=new ConcurrentHashMap<>();int writes;boolean loseResponse;Runnable duringWrite;
        public Instant authorizationUpperBound(Instant now){throw new AssertionError("Relay must not issue external capabilities");}
        public Upload createUpload(String key,String mime){throw new AssertionError("Relay must not issue external capabilities");}
        public boolean uploadsDrained(String key,Instant at){throw new AssertionError("Relay uses durable DB write evidence");}
        public void write(String key,String mime,byte[] bytes) {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            assertThat(store.jdbc.queryForObject("select upload_attempt_status from discushion.media_files where storage_key=?",String.class,key)).isEqualTo("RUNNING");
            writes++;if(duringWrite!=null)duringWrite.run();objects.put(key,bytes);
            if(loseResponse)throw new PhotoFailure(PHOTO_STORAGE_UNAVAILABLE);
        }
        public Optional<InputStream> open(String key){return Optional.ofNullable(objects.get(key)).map(ByteArrayInputStream::new);}
        public void remove(String key){objects.remove(key);}
        public String publicUrl(String key){return "https://example.invalid/"+key;}
    }
}
