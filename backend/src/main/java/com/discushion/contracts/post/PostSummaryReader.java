package com.discushion.contracts.post;

import java.util.Map;
import java.util.Set;

/** Batch read for internal personal/institution lists; does not authorize public access. */
public interface PostSummaryReader {
    Map<Long, PostSummary> findAll(Set<Long> postIds);
}
