package com.discushion.contracts.participation;

/** Called by BE2 inside the existing post deletion transaction, after posts/polls locks.
 * BE1 removes bookmarks only, retaining participation/activity/adoption history.
 * Requires an active caller transaction; must not commit or open REQUIRES_NEW.
 */
public interface PostDeletionParticipant {
    void removeBookmarks(long postId);
}
