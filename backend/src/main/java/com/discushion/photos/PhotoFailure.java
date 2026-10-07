package com.discushion.photos;

public final class PhotoFailure extends RuntimeException {
    public enum Reason {
        VALIDATION_ERROR(400), PHOTO_UPLOAD_NOT_FOUND(404), PHOTO_UPLOAD_NOT_READY(409),
        PHOTO_UPLOAD_EXPIRED(409), PHOTO_ALREADY_LINKED(409), PHOTO_DELETION_PENDING(409),
        PHOTO_UPLOAD_QUOTA_EXCEEDED(409), PHOTO_UPLOAD_RATE_LIMITED(429),
        PHOTO_SIZE_EXCEEDED(413), PHOTO_FORMAT_UNSUPPORTED(415), PHOTO_STORAGE_UNAVAILABLE(503);
        public final int status;
        Reason(int status) { this.status = status; }
    }
    private final Reason reason;
    private final Long retryAfter;
    public PhotoFailure(Reason reason) { this(reason, null); }
    public PhotoFailure(Reason reason, Long retryAfter) {
        super(reason.name()); this.reason = reason; this.retryAfter = retryAfter;
    }
    public Reason reason() { return reason; }
    public Long retryAfter() { return retryAfter; }
}
