package com.discushion.support;

import com.discushion.contracts.participation.*;
import com.discushion.contracts.post.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;

/** Per-test maps; no operating authentication, authorization, transactions or Spring registration. */
public final class SharedReadFixtures {
    private SharedReadFixtures() {}
    public static final class Summaries implements PostSummaryReader {
        private final Map<Long, PostSummary> data = new HashMap<>();
        public Summaries add(PostSummary value) { data.put(value.postId(), value); return this; }
        @Override public Map<Long, PostSummary> findAll(Set<Long> ids) {
            var result = new HashMap<Long, PostSummary>();
            ids.forEach(id -> { if (data.containsKey(id)) result.put(id, data.get(id)); });
            return Map.copyOf(result);
        }
    }
    public static final class Participation implements ParticipationSnapshotReader {
        private final Map<Long, ParticipationSnapshot> data = new HashMap<>();
        private final Map<Long, Map<Long, ParticipationSnapshot.ViewerState>> viewers = new HashMap<>();
        public Participation add(ParticipationSnapshot value) {
            if (value.viewer().isPresent()) throw new IllegalArgumentException("Register viewer states separately");
            data.put(value.postId(), value); return this;
        }
        public Participation viewer(long userId, long postId, ParticipationSnapshot.ViewerState state) {
            viewers.computeIfAbsent(userId, ignored -> new HashMap<>()).put(postId, state); return this;
        }
        @Override public Map<Long, ParticipationSnapshot> findAll(Set<Long> ids, OptionalLong viewerId) {
            var result = new HashMap<Long, ParticipationSnapshot>();
            ids.forEach(id -> {
                var value = data.get(id);
                if (value != null) {
                    var personal = viewerId.isPresent()
                            ? viewers.getOrDefault(viewerId.getAsLong(), Map.of()).getOrDefault(id,
                                new ParticipationSnapshot.ViewerState(Set.of(), false, OptionalLong.empty()))
                            : null;
                    result.put(id, new ParticipationSnapshot(id, value.parentCommentCount(), value.replyCount(),
                            value.reactions(), value.poll(), Optional.ofNullable(personal), value.evaluatedAt()));
                }
            });
            return Map.copyOf(result);
        }
    }
    public static ParticipationSnapshot emptyParticipation(long postId) {
        return new ParticipationSnapshot(postId, 0, 0, new ParticipationSnapshot.ReactionCounts(0, 0, 0),
                Optional.empty(), Optional.empty(), ContractFixtures.NOW);
    }
    public static PostSummary summary(long postId, PostStatus status) {
        return new PostSummary(postId, PostType.LOCAL_AGENDA, ContractFixtures.REGION_ID,
                ContractFixtures.USER_ID, status, ContractFixtures.NOW,
                status == PostStatus.PUBLISHED ? Optional.of(new PostSummary.Display("합성 제목", "OTHER"))
                        : Optional.empty());
    }
}
