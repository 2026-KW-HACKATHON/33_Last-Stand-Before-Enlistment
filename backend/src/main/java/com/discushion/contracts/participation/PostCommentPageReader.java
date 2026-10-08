package com.discushion.contracts.participation;

import java.util.Map;

/** Authenticated caller's initial LIKES/20 page, using the same comments contract and transaction. */
public interface PostCommentPageReader {
    Map<String, Object> initialPage(long postId);
}
