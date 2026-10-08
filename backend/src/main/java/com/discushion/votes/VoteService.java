package com.discushion.votes;

import com.discushion.contracts.post.*;
import com.discushion.identity.MemberAuthorization;
import java.time.Clock;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.transaction.support.TransactionTemplate;

final class VoteService {
    private final MemberAuthorization members;
    private final Supplier<PostContextReader> posts;
    private final JdbcVoteStore store;
    private final TransactionTemplate writes;
    private final Clock clock;

    VoteService(MemberAuthorization members,Supplier<PostContextReader> posts,JdbcVoteStore store,
            TransactionTemplate writes,Clock clock) {
        this.members=members;this.posts=posts;this.store=store;this.writes=writes;this.clock=clock;
    }

    Map<String,Object> submit(VoteInput input) {
        return writes.execute(status->{
            var member=members.lockCurrentCompletedMember();
            var post=posts.get().findForUpdate(input.postId())
                .orElseThrow(()->new VoteFailure("POST_NOT_FOUND",404,"postId"));
            if(post.postId()!=input.postId())throw new IllegalStateException("Unexpected vote source");
            if(post.type()!=PostType.VOTE||post.status()!=PostStatus.PUBLISHED)
                throw new VoteFailure("POST_NOT_FOUND",404,"postId");
            var poll=post.poll().orElseThrow(()->new IllegalStateException("Published vote has no poll"));
            members.requireVerifiedRegion(member,post.regionId());
            requireOpen(poll.endsAt());
            if(!poll.optionIds().contains(input.optionId()))
                throw new VoteFailure("VOTE_OPTION_INVALID",400,"optionId");

            Long previous=store.selectedOption(poll.pollId(),member.userId());
            if(previous!=null&&previous.longValue()!=input.optionId()&&!input.confirmChange())
                throw new VoteFailure("VOTE_CHANGE_CONFIRMATION_REQUIRED",409,"confirmChange");

            // Re-check immediately before the write; posts→polls remain locked in this transaction.
            requireOpen(poll.endsAt());
            if(previous==null||previous.longValue()!=input.optionId())
                store.select(poll.pollId(),member.userId(),input.optionId(),clock.instant());
            return Map.of("data",store.snapshot(post.postId(),poll.pollId(),poll.optionIds(),member.userId(),poll.endsAt(),clock.instant()));
        });
    }

    private void requireOpen(java.time.Instant endsAt) {
        if(!clock.instant().isBefore(endsAt))throw new VoteFailure("VOTE_ENDED",409,"postId");
    }
}
