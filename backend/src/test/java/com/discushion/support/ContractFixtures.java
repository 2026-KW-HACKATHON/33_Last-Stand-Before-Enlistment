package com.discushion.support;

import com.discushion.contracts.identity.*;
import com.discushion.contracts.post.*;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Test-only snapshots. No Spring beans, JWT verification or database locking. */
public final class ContractFixtures {
    public static final Instant NOW = Instant.parse("2026-10-07T00:00:00Z");
    public static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    public static final long USER_ID = 1;
    public static final long REGION_ID = 10;

    private ContractFixtures() {}

    public static VerifiedActor actor(boolean localMember, boolean registered) {
        return new VerifiedActor("did:privy:synthetic-test-user", localMember
                ? Optional.of(new LocalMember(USER_ID, registered ? Optional.of(NOW.minusSeconds(60))
                        : Optional.empty())) : Optional.empty());
    }

    public static MemberQualification member(boolean registered, Set<Long> regions,
            List<InstitutionGrant> grants) {
        return new MemberQualification(USER_ID, registered ? Optional.of(NOW.minusSeconds(60))
                : Optional.empty(), regions, grants, NOW);
    }

    /** Expiry at NOW is already expired; the consumer owns this permission decision. */
    public static InstitutionGrant institution(boolean expired) {
        return new InstitutionGrant(100, 200, REGION_ID, NOW.minusSeconds(3600),
                expired ? NOW : NOW.plusSeconds(3600));
    }

    public static PostContext post(long id, PostStatus status) {
        return new PostContext(id, PostType.LOCAL_AGENDA, REGION_ID, USER_ID, status, Optional.empty());
    }

    public static PostContext vote(long id, boolean ended) {
        return new PostContext(id, PostType.VOTE, REGION_ID, USER_ID, PostStatus.PUBLISHED,
                Optional.of(new PollContext(300, ended ? NOW : NOW.plusSeconds(60), List.of(301L, 302L))));
    }

    public static CurrentActorProvider guest() { return Optional::empty; }
    public static CurrentActorProvider authenticated(VerifiedActor actor) { return () -> Optional.of(actor); }

    public static final class Members implements MemberQualificationReader {
        private final Map<Long, MemberQualification> members = new HashMap<>();
        public Members add(MemberQualification member) { members.put(member.userId(), member); return this; }
        @Override public Optional<MemberQualification> find(long userId) {
            return Optional.ofNullable(members.get(userId));
        }
    }

    public static final class Posts implements PostContextReader {
        private final Map<Long, PostContext> posts = new HashMap<>();
        public Posts add(PostContext post) { posts.put(post.postId(), post); return this; }
        @Override public Optional<PostContext> find(long postId) {
            return Optional.ofNullable(posts.get(postId));
        }
        @Override public Optional<PostContext> findForUpdate(long postId) {
            throw new UnsupportedOperationException("Snapshot fake cannot verify JDBC transaction locks");
        }
    }
}
