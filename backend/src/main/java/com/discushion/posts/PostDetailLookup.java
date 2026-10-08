package com.discushion.posts;

import java.util.Map;

/** Implemented by the detailed read feature; called inside the post write transaction. */
public interface PostDetailLookup {
    Map<String, Object> read(long postId, long viewerUserId);
}
