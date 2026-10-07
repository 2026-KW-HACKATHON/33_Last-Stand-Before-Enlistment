package com.discushion.support;

import com.discushion.contracts.participation.ParticipationSnapshot;
import com.discushion.contracts.post.PostStatus;
import com.discushion.contracts.post.PostSummary;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class SharedReadFixturesTests {
    @Test void batchReadsKeepMissingAndDeletedSeparateAndHideDeletedDisplay() {
        var reader = new SharedReadFixtures.Summaries()
                .add(SharedReadFixtures.summary(1, PostStatus.PUBLISHED))
                .add(SharedReadFixtures.summary(2, PostStatus.DELETED));
        var result = reader.findAll(Set.of(1L, 2L, 3L));
        assertThat(result.keySet()).containsExactlyInAnyOrder(1L, 2L);
        assertThat(result.get(2L).display()).isEmpty();
        assertThat(new SharedReadFixtures.Summaries().findAll(Set.of(1L))).isEmpty();
        assertThatThrownBy(() -> new PostSummary(2, result.get(2L).type(), 10, 1, PostStatus.DELETED,
                ContractFixtures.NOW, Optional.of(new PostSummary.Display("비공개", "OTHER"))))
                .isInstanceOf(IllegalArgumentException.class);
    }
    @Test void publicAggregateCannotLeakAnotherMembersSelectionOrBookmark() {
        var state = new ParticipationSnapshot.ViewerState(
                Set.of(ParticipationSnapshot.ReactionType.EMPATHY), true, OptionalLong.of(301));
        var reader = new SharedReadFixtures.Participation().add(SharedReadFixtures.emptyParticipation(1))
                .viewer(1, 1, state);
        assertThat(reader.findAll(Set.of(1L), OptionalLong.empty()).get(1L).viewer()).isEmpty();
        assertThat(reader.findAll(Set.of(1L), OptionalLong.of(1)).get(1L).viewer().orElseThrow()).isEqualTo(state);
        assertThat(reader.findAll(Set.of(1L), OptionalLong.of(2)).get(1L).viewer().orElseThrow()
                .selectedOptionId()).isEmpty();
        assertThat(reader.findAll(Set.of(99L), OptionalLong.empty())).isEmpty();
    }
}
