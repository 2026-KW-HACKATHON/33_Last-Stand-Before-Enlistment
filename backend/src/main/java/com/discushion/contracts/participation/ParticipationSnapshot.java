package com.discushion.contracts.participation;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;

public record ParticipationSnapshot(long postId, long parentCommentCount, long replyCount,
        ReactionCounts reactions, Optional<PollResult> poll, Optional<ViewerState> viewer,
        Instant evaluatedAt) {
    public record ReactionCounts(long empathy, long needed, long curious) {}
    public record OptionVotes(long optionId, long count) {}
    public record PollResult(long pollId, long participantCount, List<OptionVotes> options) {
        public PollResult { options = List.copyOf(options); }
    }
    public enum ReactionType { EMPATHY, NEEDED, CURIOUS }
    public record ViewerState(Set<ReactionType> reactions, boolean bookmarked, OptionalLong selectedOptionId) {
        public ViewerState { reactions = Set.copyOf(reactions); }
    }
}
