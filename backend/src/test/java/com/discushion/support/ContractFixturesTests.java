package com.discushion.support;

import com.discushion.contracts.post.PostStatus;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class ContractFixturesTests {
    @Test void identityAndQualificationRemainIndependent() {
        assertThat(ContractFixtures.guest().current()).isEmpty();
        assertThat(ContractFixtures.authenticated(ContractFixtures.actor(false, false))
                .current().orElseThrow().member()).isEmpty();
        assertThat(ContractFixtures.actor(true, false).member().orElseThrow()
                .registrationCompletedAt()).isEmpty();
        var member = ContractFixtures.member(true, Set.of(99L), List.of(ContractFixtures.institution(true)));
        assertThat(member.verifiedRegionIds()).doesNotContain(ContractFixtures.REGION_ID);
        assertThat(member.institutionGrants().get(0).validUntil()).isEqualTo(ContractFixtures.CLOCK.instant());
    }

    @Test void readersAreIsolatedAndDoNotTurnMissingOrDeletedIntoPublicPosts() {
        var first = new ContractFixtures.Posts().add(ContractFixtures.post(1, PostStatus.DELETED));
        assertThat(first.find(1).orElseThrow().status()).isEqualTo(PostStatus.DELETED);
        assertThat(first.find(2)).isEmpty();
        assertThat(new ContractFixtures.Posts().find(1)).isEmpty();
        assertThat(new ContractFixtures.Members().find(1)).isEmpty();
        assertThatThrownBy(() -> first.findForUpdate(1)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test void voteFixturePreservesTheExpiryBoundaryAndActualOptionIds() {
        var poll = ContractFixtures.vote(1, true).poll().orElseThrow();
        assertThat(poll.endsAt()).isEqualTo(ContractFixtures.CLOCK.instant());
        assertThat(poll.optionIds()).containsExactly(301L, 302L).doesNotContain(999L);
        assertThatThrownBy(() -> poll.optionIds().add(999L)).isInstanceOf(UnsupportedOperationException.class);
    }
}
