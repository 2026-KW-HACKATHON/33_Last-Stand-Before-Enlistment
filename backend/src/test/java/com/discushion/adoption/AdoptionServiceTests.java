package com.discushion.adoption;

import com.discushion.contracts.identity.InstitutionGrant;
import com.discushion.contracts.identity.MemberQualification;
import com.discushion.contracts.post.*;
import com.discushion.identity.IdentityFailure;
import com.discushion.identity.MemberAuthorization;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AdoptionServiceTests {
    private static final Instant NOW = Instant.parse("2026-10-08T00:00:00Z");
    private static final long POST_ID = 12;
    private static final long USER_ID = 7;
    private static final long INSTITUTION_ID = 4;
    private static final long REGION_ID = 15;
    private static final long CREDENTIAL_ID = 18;

    private static TransactionTemplate transactions() {
        var manager = mock(PlatformTransactionManager.class);
        when(manager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        return new TransactionTemplate(manager);
    }

    private static MemberQualification member(List<InstitutionGrant> grants) {
        return new MemberQualification(USER_ID, Optional.of(NOW.minusSeconds(3600)), Set.of(), grants, NOW);
    }

    private static InstitutionGrant activeGrant(long institutionId, long regionId) {
        return new InstitutionGrant(CREDENTIAL_ID, institutionId, regionId,
                NOW.minusSeconds(3600), NOW.plusSeconds(3600));
    }

    private static PostContext publishedAgenda() {
        return new PostContext(POST_ID, PostType.LOCAL_AGENDA, REGION_ID, 99, PostStatus.PUBLISHED, Optional.empty());
    }

    private static AdoptionService service(MemberAuthorization members, PostContextReader posts, JdbcAdoptionStore store) {
        return new AdoptionService(members, () -> posts, store, transactions(), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test void createsOneInstitutionRelationOnlyForAnActiveGrantResponsibleForThePublishedAgendaRegion() {
        var members = mock(MemberAuthorization.class);
        when(members.lockCurrentCompletedMember()).thenReturn(member(List.of(activeGrant(INSTITUTION_ID, REGION_ID))));
        var posts = mock(PostContextReader.class);
        when(posts.findForUpdate(POST_ID)).thenReturn(Optional.of(publishedAgenda()));
        var store = mock(JdbcAdoptionStore.class);
        when(store.findCurrent(POST_ID, INSTITUTION_ID)).thenReturn(Optional.empty(),
                Optional.of(new AdoptionResponse(70, POST_ID, "노원구청", NOW)));
        when(store.insert(POST_ID, INSTITUTION_ID, USER_ID, CREDENTIAL_ID, NOW)).thenReturn(1);

        var result = service(members, posts, store).adopt(POST_ID);

        assertThat(result.created()).isTrue();
        assertThat(result.adoption()).isEqualTo(new AdoptionResponse(70, POST_ID, "노원구청", NOW));
        verify(posts).findForUpdate(POST_ID);
        verify(store).insert(POST_ID, INSTITUTION_ID, USER_ID, CREDENTIAL_ID, NOW);
    }

    @Test void repeatedAdoptionReturnsTheCurrentInstitutionRelationWithoutInsertingAnother() {
        var members = mock(MemberAuthorization.class);
        when(members.lockCurrentCompletedMember()).thenReturn(member(List.of(activeGrant(INSTITUTION_ID, REGION_ID))));
        var posts = mock(PostContextReader.class);
        when(posts.findForUpdate(POST_ID)).thenReturn(Optional.of(publishedAgenda()));
        var current = new AdoptionResponse(70, POST_ID, "노원구청", NOW.minusSeconds(30));
        var store = mock(JdbcAdoptionStore.class);
        when(store.findCurrent(POST_ID, INSTITUTION_ID)).thenReturn(Optional.of(current));

        var result = service(members, posts, store).adopt(POST_ID);

        assertThat(result.created()).isFalse();
        assertThat(result.adoption()).isEqualTo(current);
        verify(store, never()).insert(anyLong(), anyLong(), anyLong(), anyLong(), any());
    }

    @Test void regionMismatchOrNonAgendaDoesNotCreateAnAdoption() {
        var members = mock(MemberAuthorization.class);
        when(members.lockCurrentCompletedMember()).thenReturn(member(List.of(activeGrant(INSTITUTION_ID, REGION_ID + 1))));
        var posts = mock(PostContextReader.class);
        when(posts.findForUpdate(POST_ID)).thenReturn(Optional.of(publishedAgenda()));
        var store = mock(JdbcAdoptionStore.class);
        assertThatThrownBy(() -> service(members, posts, store).adopt(POST_ID))
                .isInstanceOf(AdoptionFailure.class).hasFieldOrPropertyWithValue("code", "ADOPTION_NOT_ALLOWED");
        when(posts.findForUpdate(POST_ID)).thenReturn(Optional.of(new PostContext(POST_ID, PostType.VOTE,
                REGION_ID, 99, PostStatus.PUBLISHED, Optional.empty())));
        assertThatThrownBy(() -> service(members, posts, store).adopt(POST_ID))
                .isInstanceOf(AdoptionFailure.class).hasFieldOrPropertyWithValue("code", "ADOPTION_NOT_ALLOWED");
        verifyNoInteractions(store);
    }

    @Test void expiredInstitutionGrantAndDeletedPostCannotChangeAdoptions() {
        var expired = new InstitutionGrant(CREDENTIAL_ID, INSTITUTION_ID, REGION_ID,
                NOW.minusSeconds(7200), NOW);
        var members = mock(MemberAuthorization.class);
        when(members.lockCurrentCompletedMember()).thenReturn(member(List.of(expired)));
        var posts = mock(PostContextReader.class);
        when(posts.findForUpdate(POST_ID)).thenReturn(Optional.of(publishedAgenda()));
        var store = mock(JdbcAdoptionStore.class);
        assertThatThrownBy(() -> service(members, posts, store).adopt(POST_ID))
                .isInstanceOf(IdentityFailure.class)
                .hasFieldOrPropertyWithValue("reason", IdentityFailure.Reason.INSTITUTION_REQUIRED);

        when(posts.findForUpdate(POST_ID)).thenReturn(Optional.of(new PostContext(POST_ID, PostType.LOCAL_AGENDA,
                REGION_ID, 99, PostStatus.DELETED, Optional.empty())));
        when(members.lockCurrentCompletedMember()).thenReturn(member(List.of(activeGrant(INSTITUTION_ID, REGION_ID))));
        assertThatThrownBy(() -> service(members, posts, store).adopt(POST_ID))
                .isInstanceOf(AdoptionFailure.class).hasFieldOrPropertyWithValue("code", "POST_NOT_FOUND");
        verifyNoInteractions(store);
    }

    @Test void cancellationPreservesTheRelationAndRepeatedCancellationIsA204NoOp() {
        var members = mock(MemberAuthorization.class);
        when(members.lockCurrentCompletedMember()).thenReturn(member(List.of(activeGrant(INSTITUTION_ID, REGION_ID))));
        var posts = mock(PostContextReader.class);
        when(posts.findForUpdate(POST_ID)).thenReturn(Optional.of(publishedAgenda()));
        var store = mock(JdbcAdoptionStore.class);
        when(store.findForUpdate(POST_ID, 70, INSTITUTION_ID)).thenReturn(
                Optional.of(new JdbcAdoptionStore.Row(70, NOW.minusSeconds(60), Optional.empty())),
                Optional.of(new JdbcAdoptionStore.Row(70, NOW.minusSeconds(60), Optional.of(NOW))));
        when(store.cancel(70, INSTITUTION_ID, USER_ID, NOW)).thenReturn(1);

        var service = service(members, posts, store);
        service.cancel(POST_ID, 70);
        service.cancel(POST_ID, 70);

        verify(store).cancel(70, INSTITUTION_ID, USER_ID, NOW);
        verify(store, times(2)).findForUpdate(POST_ID, 70, INSTITUTION_ID);
    }
}
