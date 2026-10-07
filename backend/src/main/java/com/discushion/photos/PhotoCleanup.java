package com.discushion.photos;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;
import org.springframework.transaction.support.TransactionTemplate;

/** Durable retry/lease state. No external request happens inside a database transaction. */
public final class PhotoCleanup {
    private final JdbcPhotoStore store;
    private final PhotoStorage storage;
    private final TransactionTemplate transactions;
    private final Clock clock;
    private final Duration lease;
    public PhotoCleanup(JdbcPhotoStore store,PhotoStorage storage,TransactionTemplate transactions,Clock clock,Duration lease) {
        if(lease.isNegative()||lease.isZero()) throw new IllegalArgumentException("Invalid cleanup lease");
        this.store=store; this.storage=storage; this.transactions=transactions; this.clock=clock; this.lease=lease;
    }
    public int runBatch(int limit) {
        if(limit<1 || limit>100) throw new IllegalArgumentException("Invalid cleanup batch");
        int processed=0;
        for(long id:store.candidates(clock.instant(),limit)) if(process(id)) processed++;
        return processed;
    }
    boolean process(long id) {
        var claimed=transactions.execute(status-> {
            var file=store.find(id,true).orElse(null);
            if(file==null || !"POST_PHOTO".equals(file.purpose()) || store.referenced(id)) return null;
            var now=clock.instant();
            if(file.expired(now)) store.deleteIntent(id,now);
            file=store.find(id,false).orElseThrow();
            if(!"DELETE_PENDING".equals(file.status()) || (file.claimExpires()!=null && now.isBefore(file.claimExpires()))) return null;
            store.claim(id,UUID.randomUUID(),now,now.plus(lease));
            return store.find(id,false).orElseThrow();
        });
        if(claimed==null) return false;
        boolean complete=false;
        String error="UPLOAD_DRAIN_UNCONFIRMED";
        try {
            storage.remove(claimed.key());
            // An expired signed URL and one successful DELETE are insufficient proof of final absence.
            if(claimed.authorizationExpires()!=null && !clock.instant().isBefore(claimed.authorizationExpires())
                    && storage.uploadsDrained(claimed.key(),claimed.authorizationExpires())) {
                var remaining=storage.open(claimed.key());
                if(remaining.isEmpty()) complete=true;
                else {try(var input=remaining.get()) {} error="OBJECT_STILL_PRESENT";}
            }
        } catch(Exception failure) {error="PHOTO_STORAGE_UNAVAILABLE";}
        boolean finalComplete=complete;
        String finalError=complete?null:error;
        transactions.executeWithoutResult(status-> {
            var fresh=store.find(id,true).orElse(null);
            var now=clock.instant();
            if(fresh==null || store.referenced(id) || !claimed.claim().equals(fresh.claim())) return;
            long retry=Math.min(3600L,60L*(1L<<Math.min(6,Math.max(0,fresh.attempts()-1))));
            store.finish(id,claimed.claim(),now,finalComplete,finalError,now.plusSeconds(retry));
        });
        return true;
    }
}
