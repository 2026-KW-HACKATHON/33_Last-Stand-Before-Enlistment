package com.discushion.share;

import com.discushion.contracts.identity.CurrentActorProvider;
import com.discushion.contracts.post.*;
import com.discushion.contracts.share.SharedPostAccess.Action;

/**
 * Internal post-bound guest policy, not token verification or an HTTP access adapter.
 * The future caller must verify the agreed share token first and obtain reply/comment
 * post IDs from the actual source. Production requests must pass the existing bearer
 * filter; an invalid member token may never be converted to an anonymous context.
 */
final class SharedGuestPostPolicy {
    enum Reason { MEMBER_CONTEXT, INVALID_ID, SCOPE_MISMATCH, MEMBER_ONLY, UNAVAILABLE }
    static final class Denied extends RuntimeException {
        final Reason reason;
        Denied(Reason reason) { super(reason.name()); this.reason = reason; }
    }

    private final CurrentActorProvider actors;
    private final PostContextReader posts;

    SharedGuestPostPolicy(CurrentActorProvider actors, PostContextReader posts) {
        this.actors = actors;
        this.posts = posts;
    }

    PostContext check(long verifiedTokenPostId, long targetPostId, Action action) {
        return check(verifiedTokenPostId, targetPostId, action, false);
    }

    PostContext check(long verifiedTokenPostId, long targetPostId, Action action, boolean lock) {
        if (actors.current().isPresent()) throw new Denied(Reason.MEMBER_CONTEXT);
        if (!validId(verifiedTokenPostId) || !validId(targetPostId)) throw new Denied(Reason.INVALID_ID);
        if (verifiedTokenPostId != targetPostId) throw new Denied(Reason.SCOPE_MISMATCH);
        if (action == null) throw new IllegalArgumentException("Missing guest action");
        switch (action) {
            case DETAIL, SUMMARY, PUBLIC_AGGREGATES, COMMENTS, CREATE_COMMENT, CREATE_REPLY -> { }
            default -> throw new Denied(Reason.MEMBER_ONLY);
        }
        var post = (lock ? posts.findForUpdate(targetPostId) : posts.find(targetPostId))
                .orElseThrow(() -> new Denied(Reason.UNAVAILABLE));
        if (post.postId() != targetPostId) throw new IllegalStateException("Post reader returned a different target");
        if (post.status() != PostStatus.PUBLISHED) throw new Denied(Reason.UNAVAILABLE);
        return post;
    }

    private static boolean validId(long id) { return id > 0 && id <= 9007199254740991L; }
}
