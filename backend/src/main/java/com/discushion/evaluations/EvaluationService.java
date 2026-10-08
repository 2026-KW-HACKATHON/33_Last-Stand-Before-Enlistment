package com.discushion.evaluations;

import com.discushion.contracts.post.*;
import com.discushion.identity.MemberAuthorization;
import java.time.Clock;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.transaction.support.TransactionTemplate;

final class EvaluationService {
    private final MemberAuthorization members;private final Supplier<PostContextReader> posts;
    private final JdbcEvaluationStore store;private final TransactionTemplate writes;private final Clock clock;
    EvaluationService(MemberAuthorization members,Supplier<PostContextReader> posts,JdbcEvaluationStore store,TransactionTemplate writes,Clock clock){
        this.members=members;this.posts=posts;this.store=store;this.writes=writes;this.clock=clock;
    }
    Map<String,Object> set(long commentId,EvaluationInput.Type selected) {
        return writes.execute(status->{
            var member=members.lockCurrentCompletedMember();long postId=store.postId(commentId);
            var post=posts.get().findForUpdate(postId).orElseThrow(()->new EvaluationFailure("POST_NOT_FOUND",404,"commentId"));
            if(post.postId()!=postId)throw new IllegalStateException("Post source returned a different evaluation target");
            if(post.status()!=PostStatus.PUBLISHED)throw new EvaluationFailure("POST_NOT_FOUND",404,"commentId");
            members.requireVerifiedRegion(member,post.regionId());
            if(store.postId(commentId)!=postId)throw new IllegalStateException("Comment source changed during evaluation");
            store.set(commentId,member.userId(),selected,clock.instant());
            // #5 is deferred. Current participation remains the actual evaluation relation.
            return Map.of("data",store.snapshot(commentId,member.userId()));
        });
    }
}
