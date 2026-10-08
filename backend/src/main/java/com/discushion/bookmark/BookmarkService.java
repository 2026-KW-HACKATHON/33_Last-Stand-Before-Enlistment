package com.discushion.bookmark;

import com.discushion.contracts.post.PostContextReader;
import com.discushion.contracts.post.PostStatus;
import com.discushion.contracts.post.PostSummary;
import com.discushion.contracts.post.PostSummaryReader;
import com.discushion.identity.MemberAuthorization;
import java.time.Clock;
import java.util.*;
import java.util.function.Supplier;
import org.springframework.transaction.support.TransactionTemplate;

final class BookmarkService {
    private static final int SCAN_SIZE=100;
    private final MemberAuthorization members;
    private final Supplier<PostContextReader> posts;
    private final Supplier<PostSummaryReader> summaries;
    private final JdbcBookmarkStore store;
    private final TransactionTemplate transactions;
    private final Clock clock;
    BookmarkService(MemberAuthorization members,Supplier<PostContextReader> posts,Supplier<PostSummaryReader> summaries,
            JdbcBookmarkStore store,TransactionTemplate transactions,Clock clock) {
        this.members=members;this.posts=posts;this.summaries=summaries;this.store=store;this.transactions=transactions;this.clock=clock;
    }
    Map<String,Object> set(long postId,boolean selected) {
        return transactions.execute(status->{
            var member=members.lockCurrentCompletedMember();
            var post=posts.get().findForUpdate(postId).orElseThrow(()->new BookmarkFailure("POST_NOT_FOUND",404,"postId"));
            if(post.postId()!=postId)throw new IllegalStateException("Unexpected bookmark post source");
            if(post.status()!=PostStatus.PUBLISHED)throw new BookmarkFailure("POST_NOT_FOUND",404,"postId");
            if(selected)store.add(postId,member.userId(),clock.instant());else store.remove(postId,member.userId());
            return Map.of("data",Map.of("postId",postId,"isBookmarked",selected));
        });
    }
    BookmarkPage list(BookmarkQuery query) {
        return transactions.execute(status->{
            var member=members.lockCurrentCompletedMember();
            var filter=new BookmarkCursor.Filter(query.type()==null?null:query.type().name(),query.topic());
            var selected=new ArrayList<Selected>();
            var after=query.after();boolean exhausted=false;
            while(selected.size()<=query.size()&&!exhausted) {
                var rows=store.page(member.userId(),after,SCAN_SIZE);
                if(rows.isEmpty()){exhausted=true;break;}
                var ids=new HashSet<Long>();rows.forEach(row->ids.add(row.postId()));
                var source=summaries.get();
                var found=source.findAll(Set.copyOf(ids));
                if(found==null)throw new IllegalStateException("Post summary source returned null");
                for(var row:rows) {
                    after=new BookmarkCursor.Position(row.bookmarkedAt(),row.postId());
                    PostSummary summary=found.get(row.postId());
                    if(summary==null||summary.status()!=PostStatus.PUBLISHED)continue;
                    var display=summary.display().orElseThrow(()->new IllegalStateException("Published post has no display fields"));
                    if(query.type()!=null&&summary.type()!=query.type())continue;
                    if(query.topic()!=null&&!query.topic().equals(display.topic()))continue;
                    selected.add(new Selected(row,summary,display));
                    if(selected.size()>query.size())break;
                }
                exhausted=rows.size()<SCAN_SIZE;
            }
            boolean hasNext=selected.size()>query.size();
            if(hasNext)selected.remove(selected.size()-1);
            String cursor=null;
            if(hasNext&&!selected.isEmpty()) {
                var last=selected.get(selected.size()-1).row();
                cursor=BookmarkCursor.encode(filter,new BookmarkCursor.Position(last.bookmarkedAt(),last.postId()));
            }
            var data=selected.stream().map(item->new BookmarkPage.Item(item.summary().postId(),item.summary().type(),
                item.summary().regionId(),item.display().title(),item.display().topic(),item.summary().createdAt(),item.row().bookmarkedAt())).toList();
            return new BookmarkPage(data,new BookmarkPage.Meta(cursor,hasNext));
        });
    }
    private record Selected(JdbcBookmarkStore.Row row,PostSummary summary,PostSummary.Display display) {}
}
