package com.discushion.posts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.discushion.contracts.identity.MemberQualification;
import com.discushion.contracts.participation.PostDeletionParticipant;
import com.discushion.contracts.post.*;
import com.discushion.identity.MemberAuthorization;
import com.discushion.photos.PhotoAttachments;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

class PostManagementServiceTests {
    private static final Instant NOW = Instant.parse("2026-10-08T10:00:00Z");
    private static final long POST_ID = 31;
    private static final long USER_ID = 8;
    private static final long REGION_ID = 4;

    @Test void blocksRegionChangeWhileAnActiveAdoptionExists() {
        var f = fixture(new PostContext(POST_ID, PostType.LOCAL_AGENDA, REGION_ID, USER_ID,
                PostStatus.PUBLISHED, Optional.empty()));
        when(f.posts.hasActiveAdoption(POST_ID)).thenReturn(true);
        assertThatThrownBy(() -> f.service.patch(POST_ID, Map.of("regionId", 5)))
                .isInstanceOf(PostEditFailure.class).hasMessage("POST_NOT_EDITABLE");
        verify(f.members).requireVerifiedRegion(any(), eq(5L));
        verify(f.posts, never()).update(anyLong(), any(), any());
    }

    @Test void rejectsEditsAfterPollEndsWithoutWriting() {
        var post = new PostContext(POST_ID, PostType.VOTE, REGION_ID, USER_ID, PostStatus.PUBLISHED,
                Optional.of(new PollContext(91, NOW.minusSeconds(1), List.of(1L, 2L))));
        var f = fixture(post);
        assertThatThrownBy(() -> f.service.patch(POST_ID, Map.of("title", "변경")))
                .isInstanceOf(PostEditFailure.class).hasMessage("POST_NOT_EDITABLE");
        verify(f.posts, never()).update(anyLong(), any(), any());
    }

    @Test void deleteSoftDeletesAfterBookmarkAndPhotoCleanupAreRecorded() {
        var f = fixture(new PostContext(POST_ID, PostType.LOCAL_AGENDA, REGION_ID, USER_ID,
                PostStatus.PUBLISHED, Optional.empty()));
        f.service.delete(POST_ID);
        var order = inOrder(f.deletion, f.photos, f.posts);
        order.verify(f.deletion).removeBookmarks(POST_ID);
        order.verify(f.photos).replace(POST_ID, List.of());
        order.verify(f.posts).delete(POST_ID);
    }

    private static Fixture fixture(PostContext post) {
        var members = mock(MemberAuthorization.class);
        var qualification = new MemberQualification(USER_ID, Optional.of(NOW.minusSeconds(100)), Set.of(REGION_ID, 5L),
                List.of(), NOW);
        when(members.lockCurrentCompletedMember()).thenReturn(qualification);
        when(members.isOwner(qualification, USER_ID)).thenReturn(true);
        doNothing().when(members).requireVerifiedRegion(any(), anyLong());
        var posts = mock(PostJdbcStore.class);
        when(posts.findForUpdate(POST_ID)).thenReturn(Optional.of(post));
        when(posts.databaseNow()).thenReturn(NOW);
        when(posts.regionExists(anyLong())).thenReturn(true);
        when(posts.editable(POST_ID)).thenReturn(new PostJdbcStore.Editable("제목", "본문", "SAFETY", REGION_ID,
                post.type(), "기관", "일정", "장소", "SCHEDULED", null, Optional.empty()));
        var photos = mock(PhotoAttachments.class);
        var deletion = mock(PostDeletionParticipant.class);
        var txManager = mock(PlatformTransactionManager.class);
        when(txManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        PostDetailLookup details = (id, viewer) -> Map.of("postId", id);
        var service = new PostManagementService(members, posts, photos, deletion, () -> details,
                new TransactionTemplate(txManager));
        return new Fixture(service, members, posts, photos, deletion);
    }

    private record Fixture(PostManagementService service, MemberAuthorization members, PostJdbcStore posts,
            PhotoAttachments photos, PostDeletionParticipant deletion) {}
}
