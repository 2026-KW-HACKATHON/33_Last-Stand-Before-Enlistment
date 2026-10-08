package com.discushion.share;

import com.discushion.contracts.post.*;
import com.discushion.contracts.share.SharedPostAccess.Action;
import com.discushion.support.ContractFixtures;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static com.discushion.share.SharedGuestPostPolicy.*;

/** Snapshot policies only; no signed tokens, DB locks or actual guest HTTP authorization. */
class SharedGuestPostPolicyTests {
    private static PostContextReader reader(AtomicReference<PostContext> current) {
        return new PostContextReader() {
            @Override public Optional<PostContext> find(long id) { return Optional.ofNullable(current.get()); }
            @Override public Optional<PostContext> findForUpdate(long id) { throw new AssertionError("Read policy cannot claim write locks"); }
        };
    }

    @Test void onlyTheExplicitPostBoundGuestActionsAreAllowed() {
        var posts = new ContractFixtures.Posts().add(ContractFixtures.post(101, PostStatus.PUBLISHED));
        var policy = new SharedGuestPostPolicy(ContractFixtures.guest(), posts);
        for (var action : new Action[]{Action.DETAIL, Action.SUMMARY, Action.PUBLIC_AGGREGATES,
                Action.COMMENTS, Action.CREATE_COMMENT, Action.CREATE_REPLY}) {
            assertThat(policy.check(101, 101, action).postId()).isEqualTo(101);
        }
    }

    @Test void guestMemberOnlyActionsNeverReadOrExposePostData() {
        var reads = new AtomicInteger();
        var posts = new PostContextReader() {
            @Override public Optional<PostContext> find(long id) { reads.incrementAndGet(); return Optional.of(ContractFixtures.post(id, PostStatus.PUBLISHED)); }
            @Override public Optional<PostContext> findForUpdate(long id) { throw new AssertionError(); }
        };
        var policy = new SharedGuestPostPolicy(ContractFixtures.guest(), posts);
        for (var action : new Action[]{Action.HOME, Action.LIST, Action.MAP, Action.PERSONAL_RECORDS,
                Action.REACTION, Action.COMMENT_EVALUATION, Action.VOTE, Action.BOOKMARK, Action.ISSUE_LINK}) {
            assertDenied(() -> policy.check(101, 101, action), Reason.MEMBER_ONLY);
        }
        assertThat(reads).hasValue(0);
    }

    @Test void currentPublicationIsRecheckedOnEveryReadAndMissingOrDeletedPostsAreDenied() {
        var current = new AtomicReference<>(ContractFixtures.post(101, PostStatus.PUBLISHED));
        var policy = new SharedGuestPostPolicy(ContractFixtures.guest(), reader(current));
        assertThat(policy.check(101, 101, Action.DETAIL).status()).isEqualTo(PostStatus.PUBLISHED);
        current.set(ContractFixtures.post(101, PostStatus.DELETED));
        assertDenied(() -> policy.check(101, 101, Action.DETAIL), Reason.UNAVAILABLE);
        current.set(null);
        assertDenied(() -> policy.check(101, 101, Action.COMMENTS), Reason.UNAVAILABLE);
    }

    @Test void boundTargetAndActualReaderIdentityMustMatch() {
        var current = new AtomicReference<>(ContractFixtures.post(102, PostStatus.PUBLISHED));
        var policy = new SharedGuestPostPolicy(ContractFixtures.guest(), reader(current));
        assertDenied(() -> policy.check(101, 102, Action.DETAIL), Reason.SCOPE_MISMATCH);
        for (long bad : new long[]{0, -1, 9007199254740992L})
            assertDenied(() -> policy.check(bad, bad, Action.DETAIL), Reason.INVALID_ID);
        assertThatThrownBy(() -> policy.check(101, 101, Action.DETAIL)).isInstanceOf(IllegalStateException.class);
    }

    @Test void validUnregisteredIncompleteAndUnqualifiedMembersCannotFallBackToGuestPolicy() {
        var posts = new ContractFixtures.Posts().add(ContractFixtures.post(101, PostStatus.PUBLISHED));
        for (var member : new boolean[]{false, true}) {
            for (var complete : new boolean[]{false, true}) {
                var policy = new SharedGuestPostPolicy(ContractFixtures.authenticated(ContractFixtures.actor(member, complete)), posts);
                assertDenied(() -> policy.check(101, 101, Action.CREATE_COMMENT), Reason.MEMBER_CONTEXT);
            }
        }
    }

    private static void assertDenied(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, Reason reason) {
        assertThatThrownBy(call).isInstanceOfSatisfying(Denied.class, failure -> assertThat(failure.reason).isEqualTo(reason));
    }
}
