package com.discushion.contracts.post;

import java.time.Instant;
import java.util.Optional;

/** Deleted posts expose metadata only; display fields must be empty. Missing posts are absent from the map. */
public record PostSummary(long postId, PostType type, long regionId, long authorUserId,
        PostStatus status, Instant createdAt, Optional<Display> display) {
    public PostSummary {
        if ((status == PostStatus.PUBLISHED) != display.isPresent()) {
            throw new IllegalArgumentException("Display fields require a published post");
        }
    }
    public record Display(String title, String topic) {}
}
