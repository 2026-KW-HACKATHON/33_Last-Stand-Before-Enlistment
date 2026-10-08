package com.discushion.officer;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.discushion.contracts.identity.*;
import com.discushion.contracts.participation.ParticipationSnapshotReader;
import com.discushion.contracts.post.PostStatus;
import com.discushion.contracts.post.PostSummary;
import com.discushion.contracts.post.PostSummaryReader;
import com.discushion.identity.IdentityFailure;
import com.discushion.identity.MemberAuthorization;
import com.discushion.support.ContractFixtures;
import com.discushion.support.SharedReadFixtures;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

class OfficerAgendaServiceTests {
    private static final Instant NOW = Instant.parse("2026-10-08T10:00:00Z");
    private static final long USER = 43;
    private static final long INSTITUTION = 9;
    private static final long REGION = 15;
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test void returnsPublishedAgendaSummariesAndCurrentInstitutionCapabilities() {
        var actors = actor();
        var qualification = qualification(grant(INSTITUTION, REGION));
        var store = mock(JdbcOfficerAgendaStore.class);
        when(store.page(eq(INSTITUTION), any(), isNull(), eq(21))).thenReturn(List.of(
                new JdbcOfficerAgendaStore.Row(101, NOW, REGION, "노원구", 6,
                        OptionalLong.of(70), Optional.of(NOW.minusSeconds(60))),
                new JdbcOfficerAgendaStore.Row(100, NOW.minusSeconds(1), 16, "도봉구", 2,
                        OptionalLong.empty(), Optional.empty())));
        var summaries = new SharedReadFixtures.Summaries()
                .add(new PostSummary(101, com.discushion.contracts.post.PostType.LOCAL_AGENDA, REGION, 1,
                        PostStatus.PUBLISHED, NOW, Optional.of(new PostSummary.Display("도로 안전", "SAFETY"))))
                .add(new PostSummary(100, com.discushion.contracts.post.PostType.LOCAL_AGENDA, 16, 2,
                        PostStatus.PUBLISHED, NOW.minusSeconds(1), Optional.of(new PostSummary.Display("버스 노선", "TRANSPORTATION"))));
        var participation = new SharedReadFixtures.Participation()
                .add(new com.discushion.contracts.participation.ParticipationSnapshot(101, 2, 3,
                        new com.discushion.contracts.participation.ParticipationSnapshot.ReactionCounts(3, 2, 1),
                        Optional.empty(), Optional.empty(), NOW))
                .add(new com.discushion.contracts.participation.ParticipationSnapshot(100, 0, 0,
                        new com.discushion.contracts.participation.ParticipationSnapshot.ReactionCounts(1, 1, 0),
                        Optional.empty(), Optional.empty(), NOW));
        var service = service(actors, qualification, store, summaries, participation);

        var page = service.list(new OfficerAgendaQuery(OfficerAgendaScope.ALL, null, 20, null));

        assertThat(page.data()).hasSize(2);
        assertThat(page.data().get(0).myInstitutionAdoption()).isEqualTo(new OfficerAgendaPage.InstitutionAdoption(70, NOW.minusSeconds(60)));
        assertThat(page.data().get(0).capabilities()).isEqualTo(new OfficerAgendaPage.Capabilities(false, true));
        assertThat(page.data().get(0).reactionCounts()).isEqualTo(new OfficerAgendaPage.ReactionCounts(3, 2, 1, 6));
        assertThat(page.data().get(0).commentCount()).isEqualTo(5);
        assertThat(page.data().get(1).capabilities()).isEqualTo(new OfficerAgendaPage.Capabilities(false, false));
        verify(store).page(eq(INSTITUTION), any(), isNull(), eq(21));
    }

    @Test void missingActiveInstitutionIsForbiddenAndAmbiguousCredentialsFailClosed() {
        var store = mock(JdbcOfficerAgendaStore.class);
        var noInstitution = service(actor(), qualification(), store,
                mock(PostSummaryReader.class), mock(ParticipationSnapshotReader.class));
        assertThatThrownBy(() -> noInstitution.list(new OfficerAgendaQuery(OfficerAgendaScope.ALL, null, 20, null)))
                .isInstanceOf(IdentityFailure.class)
                .satisfies(error -> assertThat(((IdentityFailure) error).reason())
                        .isEqualTo(IdentityFailure.Reason.INSTITUTION_REQUIRED));

        var ambiguous = service(actor(), qualification(grant(INSTITUTION, REGION), grant(10, REGION)), store,
                mock(PostSummaryReader.class), mock(ParticipationSnapshotReader.class));
        assertThatThrownBy(() -> ambiguous.list(new OfficerAgendaQuery(OfficerAgendaScope.ALL, null, 20, null)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test void adoptedScopeUsesCurrentInstitutionAndRegionFilter() {
        var store = mock(JdbcOfficerAgendaStore.class);
        when(store.page(eq(INSTITUTION), any(), isNull(), eq(1))).thenReturn(List.of());
        var service = service(actor(), qualification(grant(INSTITUTION, REGION)), store,
                mock(PostSummaryReader.class), mock(ParticipationSnapshotReader.class));

        service.list(new OfficerAgendaQuery(OfficerAgendaScope.ADOPTED, REGION, 1, null));

        verify(store).page(eq(INSTITUTION), argThat(query -> query.scope() == OfficerAgendaScope.ADOPTED
                && query.regionId().equals(REGION)), isNull(), eq(2));
    }

    @Test void missingBatchSnapshotsAreNotSilentlyReplacedWithZeros() {
        var store = mock(JdbcOfficerAgendaStore.class);
        when(store.page(anyLong(), any(), isNull(), anyInt())).thenReturn(List.of(
                new JdbcOfficerAgendaStore.Row(101, NOW, REGION, "노원구", 0, OptionalLong.empty(), Optional.empty())));
        var summaries = new SharedReadFixtures.Summaries()
                .add(new PostSummary(101, com.discushion.contracts.post.PostType.LOCAL_AGENDA, REGION, 1,
                        PostStatus.PUBLISHED, NOW, Optional.of(new PostSummary.Display("도로 안전", "SAFETY"))));
        var service = service(actor(), qualification(grant(INSTITUTION, REGION)), store,
                summaries, mock(ParticipationSnapshotReader.class));

        assertThatThrownBy(() -> service.list(new OfficerAgendaQuery(OfficerAgendaScope.ALL, null, 20, null)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test void inconsistentReactionAggregateFailsInsteadOfReturningMisorderedCounts() {
        var store = mock(JdbcOfficerAgendaStore.class);
        when(store.page(anyLong(), any(), isNull(), anyInt())).thenReturn(List.of(
                new JdbcOfficerAgendaStore.Row(101, NOW, REGION, "노원구", 3, OptionalLong.empty(), Optional.empty())));
        var summaries = new SharedReadFixtures.Summaries()
                .add(new PostSummary(101, com.discushion.contracts.post.PostType.LOCAL_AGENDA, REGION, 1,
                        PostStatus.PUBLISHED, NOW, Optional.of(new PostSummary.Display("도로 안전", "SAFETY"))));
        var participation = new SharedReadFixtures.Participation()
                .add(new com.discushion.contracts.participation.ParticipationSnapshot(101, 0, 0,
                        new com.discushion.contracts.participation.ParticipationSnapshot.ReactionCounts(1, 1, 0),
                        Optional.empty(), Optional.empty(), NOW));
        var service = service(actor(), qualification(grant(INSTITUTION, REGION)), store, summaries, participation);

        assertThatThrownBy(() -> service.list(new OfficerAgendaQuery(OfficerAgendaScope.ALL, null, 20, null)))
                .isInstanceOf(IllegalStateException.class);
    }

    private OfficerAgendaService service(CurrentActorProvider actors, MemberQualification qualification,
            JdbcOfficerAgendaStore store, PostSummaryReader summaries, ParticipationSnapshotReader participation) {
        var txManager = mock(PlatformTransactionManager.class);
        when(txManager.getTransaction(any(TransactionDefinition.class))).thenReturn(new SimpleTransactionStatus());
        var qualificationReader = (MemberQualificationReader) userId -> Optional.of(qualification);
        var authorization = new MemberAuthorization(actors, userId -> Optional.empty(), clock);
        return new OfficerAgendaService(actors, qualificationReader, authorization, () -> summaries,
                () -> participation, store, new TransactionTemplate(txManager), clock);
    }

    private CurrentActorProvider actor() {
        return () -> Optional.of(new VerifiedActor("did:privy:officer-test",
                Optional.of(new LocalMember(USER, Optional.of(NOW.minusSeconds(10))))));
    }

    private MemberQualification qualification(InstitutionGrant... grants) {
        return new MemberQualification(USER, Optional.of(NOW.minusSeconds(10)), Set.of(), List.of(grants), NOW);
    }

    private InstitutionGrant grant(long institutionId, long regionId) {
        return new InstitutionGrant(institutionId + 100, institutionId, regionId, NOW.minusSeconds(120), NOW.plusSeconds(120));
    }
}
