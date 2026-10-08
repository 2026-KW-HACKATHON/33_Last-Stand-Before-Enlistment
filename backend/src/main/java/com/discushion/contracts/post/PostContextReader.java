package com.discushion.contracts.post;

import java.util.Optional;

public interface PostContextReader {
    Optional<PostContext> find(long postId);

    /** Requires an existing transaction; locks posts then polls without committing it. */
    Optional<PostContext> findForUpdate(long postId);
}
