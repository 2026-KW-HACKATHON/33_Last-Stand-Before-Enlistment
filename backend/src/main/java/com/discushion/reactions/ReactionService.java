package com.discushion.reactions;

import com.discushion.contracts.post.*;
import com.discushion.identity.MemberAuthorization;
import java.time.Clock;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.transaction.support.TransactionTemplate;

final class ReactionService {
    private final MemberAuthorization members;private final Supplier<PostContextReader> posts;
    private final JdbcReactionStore store;private final TransactionTemplate writes;private final Clock clock;
    ReactionService(MemberAuthorization members,Supplier<PostContextReader> posts,JdbcReactionStore store,TransactionTemplate writes,Clock clock) {
        this.members=members;this.posts=posts;this.store=store;this.writes=writes;this.clock=clock;
    }
    Map<String,Object> set(ReactionInput input,boolean selected) {
        return writes.execute(status->{
            var member=members.lockCurrentCompletedMember();
            var post=posts.get().findForUpdate(input.postId()).orElseThrow(()->new ReactionFailure("POST_NOT_FOUND",404,"postId"));
            if(post.postId()!=input.postId())throw new IllegalStateException("Unexpected reaction source");
            if(post.status()!=PostStatus.PUBLISHED)throw new ReactionFailure("POST_NOT_FOUND",404,"postId");
            members.requireVerifiedRegion(member,post.regionId());
            store.set(post.postId(),member.userId(),input.type(),selected,clock.instant());
            // #5 is deferred by user decision; do not claim activity +1 is implemented.
            return Map.of("data",store.snapshot(post.postId(),member.userId()));
        });
    }
}
