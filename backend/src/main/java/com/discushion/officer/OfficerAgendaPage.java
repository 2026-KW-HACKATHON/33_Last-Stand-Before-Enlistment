package com.discushion.officer;

import com.discushion.contracts.post.PostType;
import java.time.Instant;
import java.util.List;

record OfficerAgendaPage(List<Item> data, Meta meta) {
    OfficerAgendaPage { data = List.copyOf(data); }

    record Item(long postId, PostType type, long regionId, String regionName, String title, String topic,
            ReactionCounts reactionCounts, long commentCount, Instant createdAt,
            InstitutionAdoption myInstitutionAdoption, Capabilities capabilities) {}
    record ReactionCounts(long empathy, long needed, long curious, long total) {}
    record InstitutionAdoption(long id, Instant adoptedAt) {}
    record Capabilities(boolean canAdopt, boolean canCancelAdoption) {}
    record Meta(String nextCursor, boolean hasNext) {}
}
