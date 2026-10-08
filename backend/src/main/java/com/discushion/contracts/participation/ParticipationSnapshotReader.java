package com.discushion.contracts.participation;

import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;

/** Internal batch read after caller checks access. Never accepts a client-supplied viewer identity. */
public interface ParticipationSnapshotReader {
    Map<Long, ParticipationSnapshot> findAll(Set<Long> postIds, OptionalLong verifiedViewerUserId);
}
