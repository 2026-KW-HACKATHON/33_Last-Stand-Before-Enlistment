package com.discushion.photos;

import java.time.Clock;
import java.time.Instant;
import java.util.function.Supplier;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import com.discushion.identity.MemberAuthorization;
import org.springframework.transaction.support.TransactionTemplate;
import static com.discushion.photos.PhotoFailure.Reason.*;

public final class PhotoService {
    public record Reservation(long fileId,String status,Instant createdAt,Instant cleanupEligibleAt,PhotoStorage.Upload upload) {}
    private final JdbcPhotoStore store;
    private final MemberAuthorization authorization;
    private final PhotoStorage storage;
    private final TransactionTemplate transactions;
    private final Clock clock;
    private final boolean relay;
    private final Semaphore uploadSlots=new Semaphore(2);
    public PhotoService(JdbcPhotoStore store,MemberAuthorization authorization,PhotoStorage storage,
            TransactionTemplate transactions,Clock clock) {
        this(store,authorization,storage,transactions,clock,false);
    }
    public PhotoService(JdbcPhotoStore store,MemberAuthorization authorization,PhotoStorage storage,
            TransactionTemplate transactions,Clock clock,boolean relay) {
        this.store=store; this.authorization=authorization; this.storage=storage; this.transactions=transactions; this.clock=clock;this.relay=relay;
    }
    public Reservation reserve(String name,String mime,long bytes) {
        if(name==null || name.isBlank() || name.indexOf('\0')>=0 || name.indexOf('\r')>=0 || name.indexOf('\n')>=0)
            throw new PhotoFailure(VALIDATION_ERROR);
        if(!"image/jpeg".equals(mime) && !"image/png".equals(mime)) throw new PhotoFailure(PHOTO_FORMAT_UNSUPPORTED);
        if(bytes<=0) throw new PhotoFailure(VALIDATION_ERROR);
        if(bytes>PhotoContent.MAX_BYTES) throw new PhotoFailure(PHOTO_SIZE_EXCEEDED);
        var file=tx(()-> {
            long owner=authorization.lockCurrentCompletedMember().userId();
            var now=relay?store.databaseNow():clock.instant();
            var bound=relay?now.plusSeconds(7200):storage.authorizationUpperBound(now);
            if(bound==null || !bound.isAfter(now)) throw new PhotoFailure(PHOTO_STORAGE_UNAVAILABLE);
            return store.reserve(owner,name,mime,bytes,now,bound,relay);
        });
        if(relay) return new Reservation(file.id(),file.status(),file.created(),file.cleanupAt(),
            new PhotoStorage.Upload("/api/v1/photo-uploads/"+file.id()+"/content","PUT","RAW",
                Map.of("Content-Type",mime),file.authorizationExpires()));
        // Reservation and capability bound are committed before the one external issuance attempt.
        PhotoStorage.Upload upload;
        try { upload=storage.createUpload(file.key(),mime); }
        catch(RuntimeException failure) {
            tx(()-> { var fresh=store.find(file.id(),true).orElseThrow();
                if(!store.referenced(fresh.id()) && "UPLOADING".equals(fresh.status())) store.deleteIntent(fresh.id(),clock.instant());
                return null; });
            throw new PhotoFailure(PHOTO_STORAGE_UNAVAILABLE);
        }
        Reservation result=tx(()-> {
            long owner=authorization.lockCurrentCompletedMember().userId();
            var fresh=store.owned(file.id(),owner);
            if(upload.expiresAt()==null) {store.deleteIntent(fresh.id(),clock.instant()); return null;}
            store.authorizationBound(fresh.id(),upload.expiresAt());
            // Preserve the observed expiry even when the provider violates the precommitted bound.
            // Never expose such a capability or silently treat the configured allowance as verified.
            if(!"UPLOADING".equals(fresh.status()) || fresh.expired(clock.instant()) || !"PUT".equals(upload.method())
                || !"RAW".equals(upload.bodyMode()) || !upload.expiresAt().isAfter(clock.instant())
                || upload.expiresAt().isAfter(file.authorizationExpires())) {
                if("UPLOADING".equals(fresh.status())) store.deleteIntent(fresh.id(),clock.instant());
                return null;
            }
            return new Reservation(fresh.id(),fresh.status(),fresh.created(),fresh.cleanupAt(),upload);
        });
        return result!=null?result:unavailable();
    }
    private static Reservation unavailable() {throw new PhotoFailure(PHOTO_STORAGE_UNAVAILABLE);}
    /** Admit and persist exactly one write before dispatch. Unknown results are never expired by a lease. */
    public PhotoView upload(long id,String mime,InputStream input) {
        validId(id);
        var initial=tx(()-> {var file=current(id);requireUploadable(file,mime);return file;});
        if(!uploadSlots.tryAcquire()) throw new PhotoFailure(PHOTO_STORAGE_UNAVAILABLE);
        try {
            long deadline=System.nanoTime()+java.time.Duration.ofSeconds(30).toNanos();
            byte[] bytes=PhotoUploadBody.read(input,deadline);
            if(bytes.length!=initial.bytes()) throw new PhotoFailure(VALIDATION_ERROR);
            PhotoContent.inspectBeforeDeadline(new ByteArrayInputStream(bytes),mime,deadline);
            UUID attempt=UUID.randomUUID();
            var started=tx(()-> {
                var file=current(id);requireUploadable(file,mime);
                store.startUpload(id,attempt,store.databaseNow());return file;
            });
            try {storage.write(started.key(),mime,bytes);}
            catch(RuntimeException failure) {
                tx(()-> {var file=store.find(id,true).orElseThrow();store.finishUpload(id,attempt,false);
                    if("UPLOADING".equals(file.status())) store.deleteIntent(id,store.databaseNow());return null;});
                throw new PhotoFailure(PHOTO_STORAGE_UNAVAILABLE);
            }
            // Persist known completion even if membership or the HTTP connection changed during the write.
            tx(()-> {store.find(id,true).orElseThrow();store.finishUpload(id,attempt,true);return null;});
            return tx(()-> {var file=current(id);requireCompletable(file);return view(file);});
        } finally {uploadSlots.release();}
    }
    private void requireUploadable(PhotoFile file,String mime) {
        if(!file.relay()) throw new PhotoFailure(PHOTO_UPLOAD_NOT_FOUND);
        requireCompletable(file);
        if(!"UPLOADING".equals(file.status()) || file.uploadAttemptStatus()!=null) throw new PhotoFailure(PHOTO_UPLOAD_NOT_READY);
        if(!file.authorizationExpires().isAfter(store.databaseNow())) throw new PhotoFailure(PHOTO_UPLOAD_EXPIRED);
        if(!file.mime().equals(mime)) throw new PhotoFailure(PHOTO_FORMAT_UNSUPPORTED);
    }
    public PhotoView get(long id) { validId(id); return tx(()->view(current(id))); }
    public PhotoView cancel(long id) {
        validId(id);
        return tx(()-> {
            var file=current(id);
            if("LINKED".equals(file.status()) || store.referenced(id)) throw new PhotoFailure(PHOTO_ALREADY_LINKED);
            if("LEGACY".equals(file.status())) throw new PhotoFailure(PHOTO_UPLOAD_NOT_READY);
            store.deleteIntent(id,clock.instant());
            return view(store.find(id,false).orElseThrow());
        });
    }
    public PhotoView complete(long id) {
        validId(id);
        var file=tx(()-> {
            var fresh=current(id); requireCompletable(fresh);
            if(fresh.relay() && !"ACKNOWLEDGED".equals(fresh.uploadAttemptStatus())) throw new PhotoFailure(PHOTO_UPLOAD_NOT_READY);
            return fresh;
        });
        if(file.validated()) return get(id); // Never refresh uploadedAt or link expiry.
        PhotoContent.Verified verified;
        try {
            var input=storage.open(file.key()).orElseThrow(()->new PhotoFailure(PHOTO_UPLOAD_NOT_READY));
            verified=PhotoContent.inspect(input,file.mime());
        } catch(PhotoFailure failure) {
            if(failure.reason()==PHOTO_SIZE_EXCEEDED || failure.reason()==PHOTO_FORMAT_UNSUPPORTED) {
                tx(()-> { var fresh=current(id);
                    if("UPLOADING".equals(fresh.status()) && !store.referenced(id)) store.deleteIntent(id,clock.instant());
                    return null; });
            }
            throw failure;
        } catch(RuntimeException failure) { throw new PhotoFailure(PHOTO_STORAGE_UNAVAILABLE); }
        return tx(()-> {
            var fresh=current(id); requireCompletable(fresh);
            if(!fresh.validated()) store.verified(id,verified,clock.instant());
            return view(store.find(id,false).orElseThrow());
        });
    }
    private void requireCompletable(PhotoFile file) {
        if("DELETE_PENDING".equals(file.status()) || "DELETED".equals(file.status())) throw new PhotoFailure(PHOTO_DELETION_PENDING);
        if("LEGACY".equals(file.status())) throw new PhotoFailure(PHOTO_UPLOAD_NOT_READY);
        if(file.expired(clock.instant())) throw new PhotoFailure(PHOTO_UPLOAD_EXPIRED);
        if(store.referenced(file.id()) && !"LINKED".equals(file.status())) throw new PhotoFailure(PHOTO_ALREADY_LINKED);
    }
    private PhotoFile current(long id) {return store.owned(id,authorization.lockCurrentCompletedMember().userId());}
    private PhotoView view(PhotoFile file) {
        boolean deleted="DELETED".equals(file.status()), pending="DELETE_PENDING".equals(file.status());
        boolean canAttach="UNLINKED".equals(file.status()) && !file.expired(clock.instant()) && !store.referenced(file.id());
        return new PhotoView(file.id(),file.status(),file.validated()?file.mime():null,file.validated()?file.bytes():null,
            file.validated()&&!deleted&&!pending?storage.publicUrl(file.key()):null,file.created(),file.uploaded(),
            file.authorizationExpires(),file.cleanupAt(),"UNLINKED".equals(file.status())?file.cleanupAt():null,
            canAttach,deleted,file.deleteRequested(),file.deleted());
    }
    private <T> T tx(Supplier<T> work) {return transactions.execute(status->work.get());}
    static void validId(long id) {if(id<1 || id>9007199254740991L) throw new PhotoFailure(VALIDATION_ERROR);}
}
