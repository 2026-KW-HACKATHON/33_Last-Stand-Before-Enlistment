package com.discushion.bookmark;

import com.discushion.contracts.post.PostType;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.util.LinkedMultiValueMap;
import static org.assertj.core.api.Assertions.*;

class BookmarkQueryTests {
    @Test void defaultsAndFilterPageBoundariesAreExplicit() {
        var params=new LinkedMultiValueMap<String,String>();
        assertThat(BookmarkQuery.parse(params)).isEqualTo(new BookmarkQuery(null,null,20,null));
        params.set("type","VOTE");params.set("topic","SAFETY");params.set("size","100");
        assertThat(BookmarkQuery.parse(params)).isEqualTo(new BookmarkQuery(PostType.VOTE,"SAFETY",100,null));
        for(String bad:List.of("0","101","-1","1.0","")) {
            params.set("size",bad);
            assertThatThrownBy(()->BookmarkQuery.parse(params)).isInstanceOf(BookmarkFailure.class);
        }
    }
    @Test void unknownFiltersDuplicateParametersAndUnsafeIdsAreRejected() {
        var params=new LinkedMultiValueMap<String,String>();params.set("type","OTHER");
        assertThatThrownBy(()->BookmarkQuery.parse(params)).isInstanceOf(BookmarkFailure.class);
        params.clear();params.set("topic","other");
        assertThatThrownBy(()->BookmarkQuery.parse(params)).isInstanceOf(BookmarkFailure.class);
        params.clear();params.add("size","1");params.add("size","2");
        assertThatThrownBy(()->BookmarkQuery.parse(params)).isInstanceOf(BookmarkFailure.class);
        params.clear();params.set("userId","99");
        assertThatThrownBy(()->BookmarkQuery.parse(params)).isInstanceOf(BookmarkFailure.class);
        for(String bad:List.of("0","-1","01x","9007199254740992","99999999999999999"))
            assertThatThrownBy(()->BookmarkInput.postId(bad)).isInstanceOf(BookmarkFailure.class);
    }
    @Test void cursorRoundTripBindsFiltersAndPreservesTimestampPrecision() {
        var filter=new BookmarkCursor.Filter("VOTE","SAFETY");
        var position=new BookmarkCursor.Position(Instant.parse("2026-10-08T00:00:00.123456789Z"),BookmarkInput.MAX_ID);
        String cursor=BookmarkCursor.encode(filter,position);
        var params=new LinkedMultiValueMap<String,String>();params.set("type","VOTE");params.set("topic","SAFETY");params.set("cursor",cursor);
        assertThat(BookmarkQuery.parse(params).after()).isEqualTo(position);
        params.set("topic","HOUSING");assertThatThrownBy(()->BookmarkQuery.parse(params)).isInstanceOf(BookmarkFailure.class);
        params.set("topic","SAFETY");params.set("cursor",cursor+"=");assertThatThrownBy(()->BookmarkQuery.parse(params)).isInstanceOf(BookmarkFailure.class);
    }
}
