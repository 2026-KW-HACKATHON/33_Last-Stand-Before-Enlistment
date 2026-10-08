package com.discushion.bookmark;

import com.discushion.contracts.post.PostType;
import java.time.Instant;
import java.util.List;

record BookmarkPage(List<Item> data,Meta meta) {
    BookmarkPage { data=List.copyOf(data); }
    record Item(long postId,PostType type,long regionId,String title,String topic,Instant createdAt,Instant bookmarkedAt) {}
    record Meta(String nextCursor,boolean hasNext) {}
}
