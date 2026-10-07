package com.discushion.photos;

import java.time.Instant;

public record PhotoView(long fileId, String status, String contentType, Long sizeBytes, String url,
        Instant createdAt, Instant uploadedAt, Instant uploadAuthorizationExpiresAt,
        Instant cleanupEligibleAt, Instant linkExpiresAt, boolean canAttach, boolean deletionCompleted,
        Instant deleteRequestedAt, Instant deletedAt) {}
