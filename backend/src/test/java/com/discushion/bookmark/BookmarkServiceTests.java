package com.discushion.bookmark;

import com.discushion.contracts.identity.MemberQualification;
import com.discushion.contracts.post.*;
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

class BookmarkServiceTests {
    private static final Instant NOW=Instant.parse("2026-10-08T00:00:00Z");
    private static TransactionTemplate transactions() {
        var manager=mock(PlatformTransactionManager.class);
        when(manager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        return new TransactionTemplate(manager);
    }
    private static MemberQualification member(){return new MemberQualification(5,Optional.of(NOW),Set.of(),List.of(),NOW);}

    @Test void desiredStateWritesRequireCompletedMemberAndPublishedPostButNotNeighborQualification() {
        var members=mock(MemberAuthorization.class);when(members.lockCurrentCompletedMember()).thenReturn(member());
        var posts=mock(PostContextReader.class);when(posts.findForUpdate(9)).thenReturn(Optional.of(
            new PostContext(9,PostType.LOCAL_AGENDA,77,8,PostStatus.PUBLISHED,Optional.empty())));
        var store=mock(JdbcBookmarkStore.class);
        var service=new BookmarkService(members,()->posts,()->mock(PostSummaryReader.class),store,transactions(),Clock.fixed(NOW,ZoneOffset.UTC));
        assertThat(service.set(9,true)).containsEntry("data",Map.of("postId",9L,"isBookmarked",true));
        assertThat(service.set(9,false)).containsEntry("data",Map.of("postId",9L,"isBookmarked",false));
        verify(store).add(9,5,NOW);verify(store).remove(9,5);
        verify(members,never()).requireVerifiedRegion(any(),anyLong());
    }

    @Test void missingAndDeletedPostsNeverChangeTheBookmarkRelation() {
        var members=mock(MemberAuthorization.class);when(members.lockCurrentCompletedMember()).thenReturn(member());
        var posts=mock(PostContextReader.class);var store=mock(JdbcBookmarkStore.class);
        var service=new BookmarkService(members,()->posts,()->mock(PostSummaryReader.class),store,transactions(),Clock.fixed(NOW,ZoneOffset.UTC));
        assertThatThrownBy(()->service.set(404,true)).isInstanceOf(BookmarkFailure.class);
        when(posts.findForUpdate(9)).thenReturn(Optional.of(
            new PostContext(9,PostType.LOCAL_AGENDA,77,8,PostStatus.DELETED,Optional.empty())));
        assertThatThrownBy(()->service.set(9,false)).isInstanceOf(BookmarkFailure.class);
        verifyNoInteractions(store);
    }

    @Test void listReturnsOnlyPublishedMatchingOwnSummariesAndKeysetCursor() {
        var members=mock(MemberAuthorization.class);when(members.lockCurrentCompletedMember()).thenReturn(member());
        var store=mock(JdbcBookmarkStore.class);
        var first=new JdbcBookmarkStore.Row(11,NOW);
        var second=new JdbcBookmarkStore.Row(12,NOW.minusSeconds(1));
        var third=new JdbcBookmarkStore.Row(13,NOW.minusSeconds(2));
        when(store.page(5,null,100)).thenReturn(List.of(first,second,third));
        var summaries=mock(PostSummaryReader.class);
        when(summaries.findAll(Set.of(11L,12L,13L))).thenReturn(Map.of(
            11L,new PostSummary(11,PostType.VOTE,1,8,PostStatus.PUBLISHED,NOW,Optional.of(new PostSummary.Display("투표","SAFETY"))),
            12L,new PostSummary(12,PostType.LOCAL_AGENDA,2,8,PostStatus.PUBLISHED,NOW.minusSeconds(5),Optional.of(new PostSummary.Display("의견","SAFETY"))),
            13L,new PostSummary(13,PostType.VOTE,3,8,PostStatus.PUBLISHED,NOW.minusSeconds(8),Optional.of(new PostSummary.Display("투표2","SAFETY")))));
        var service=new BookmarkService(members,()->mock(PostContextReader.class),()->summaries,store,transactions(),Clock.fixed(NOW,ZoneOffset.UTC));
        var query=new BookmarkQuery(PostType.VOTE,"SAFETY",1,null);
        var page=service.list(query);
        assertThat(page.data()).hasSize(1).first().satisfies(item->{assertThat(item.postId()).isEqualTo(11);assertThat(item.bookmarkedAt()).isEqualTo(NOW);});
        assertThat(page.meta().hasNext()).isTrue();assertThat(page.meta().nextCursor()).isNotBlank();
    }
}
