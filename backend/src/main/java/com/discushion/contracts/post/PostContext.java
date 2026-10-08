package com.discushion.contracts.post;

import java.util.Optional;

public record PostContext(long postId, PostType type, long regionId, long authorUserId,
        PostStatus status, Optional<PollContext> poll) {}
