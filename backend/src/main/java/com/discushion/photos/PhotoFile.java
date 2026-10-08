package com.discushion.photos;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

record PhotoFile(long id, long owner, String key, String mime, long bytes, String purpose, String status,
        Instant created, Instant uploaded, Instant linked, Instant deleteRequested, Instant deleted,
        Instant authorizationExpires, UUID claim, Instant claimExpires, int attempts,
        String transport, String uploadAttemptStatus) {
    boolean relay() {return "SERVER_RELAY".equals(transport);}
    boolean writesFinished() {return uploadAttemptStatus==null || "ACKNOWLEDGED".equals(uploadAttemptStatus);}
    Instant cleanupAt() {
        return switch(status) {
            case "UPLOADING" -> created.plus(Duration.ofHours(24));
            case "UNLINKED" -> uploaded.plus(Duration.ofHours(24));
            default -> null;
        };
    }
    boolean expired(Instant now) { var at=cleanupAt(); return at!=null && !now.isBefore(at); }
    boolean validated() { return uploaded!=null; }
}
