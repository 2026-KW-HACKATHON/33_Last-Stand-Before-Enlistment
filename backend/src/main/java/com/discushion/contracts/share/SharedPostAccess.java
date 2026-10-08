package com.discushion.contracts.share;

import java.time.Instant;
import java.util.Optional;

/** Trusted request adapter. Empty means a member request: use ordinary member authorization, never guest fallback. */
public interface SharedPostAccess {
    enum Action {
        DETAIL, SUMMARY, PUBLIC_AGGREGATES, COMMENTS, CREATE_COMMENT, CREATE_REPLY,
        HOME, LIST, MAP, PERSONAL_RECORDS, REACTION, COMMENT_EVALUATION, VOTE, BOOKMARK, ISSUE_LINK
    }
    record GuestContext(long postId, Instant expiresAt) {}

    /** Caller owns the JDBC transaction shared with its actual detail/aggregate read. */
    Optional<GuestContext> guestForRead(long resolvedPostId, Action action);

    /** Caller owns a writable JDBC transaction. Locks posts/polls, then rechecks token expiry. */
    Optional<GuestContext> guestForWrite(long resolvedPostId, Action action);
}
