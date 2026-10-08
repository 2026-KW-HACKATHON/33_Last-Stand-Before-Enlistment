package com.discushion.posts;

import com.discushion.contracts.identity.*;
import com.discushion.contracts.participation.*;
import com.discushion.contracts.share.SharedPostAccess;
import com.discushion.identity.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import org.springframework.transaction.support.TransactionTemplate;

public final class PostDetailService implements PostDetailLookup {
    private static final ZoneOffset OFFSET = ZoneOffset.ofHours(9);
    private final CurrentActorProvider actors;
    private final JdbcMemberStore members;
    private final SharedPostAccess sharing;
    private final ParticipationSnapshotReader participation;
    private final PostCommentPageReader comments;
    private final JdbcPostDetailStore store;
    private final Function<String, String> photoUrl;
    private final TransactionTemplate reads;
    private final Clock clock;
    PostDetailService(CurrentActorProvider actors, JdbcMemberStore members, SharedPostAccess sharing,
            ParticipationSnapshotReader participation, PostCommentPageReader comments, JdbcPostDetailStore store,
            Function<String, String> photoUrl, TransactionTemplate reads, Clock clock) {
        this.actors = actors; this.members = members; this.sharing = sharing; this.participation = participation;
        this.comments = comments; this.store = store; this.photoUrl = photoUrl; this.reads = reads; this.clock = clock;
    }
    Map<String, Object> get(long postId) { return reads.execute(status -> compose(postId, null)); }
    @Override public Map<String, Object> read(long postId, long viewerUserId) {
        if (viewerUserId < 1 || viewerUserId > 9007199254740991L) throw new IllegalArgumentException("Invalid viewer");
        return reads.execute(status -> compose(postId, viewerUserId));
    }
    private Map<String, Object> compose(long id, Long requiredViewer) {
        if (id < 1 || id > 9007199254740991L) throw new PostDetailFailure("VALIDATION_ERROR", 400);
        MemberQualification member = currentMember();
        if (requiredViewer != null && (member == null || requiredViewer != member.userId())) throw new IllegalStateException("Detail viewer must match the verified actor");
        if (member == null && sharing.guestForRead(id, SharedPostAccess.Action.DETAIL).isEmpty()) throw new IllegalStateException("Missing guest context");
        store.lock(id); Instant now = clock.instant(); var post = store.base(id, now);
        if (post.nickname() == null) throw new IllegalStateException("Post author has no public profile");
        var snapshot = participation.findAll(Set.of(id), member == null ? OptionalLong.empty() : OptionalLong.of(member.userId())).get(id);
        if (snapshot == null || snapshot.postId() != id) throw new IllegalStateException("Published post has no participation snapshot");
        var data = new LinkedHashMap<String, Object>(); data.put("id", id); data.put("type", post.type()); data.put("topic", post.topic());
        data.put("title", post.title()); data.put("content", post.content()); data.put("status", "PUBLISHED");
        data.put("region", Map.of("id", post.regionId(), "name", post.regionName()));
        var author = new LinkedHashMap<String, Object>(); author.put("displayName", post.nickname());
        author.put("profileImageUrl", null); author.put("institutionVerified", post.institution()); data.put("author", author);
        var images = new ArrayList<Map<String, Object>>();
        for (var image : store.images(id)) {
            if (image.ownerId() != post.authorId()) throw new IllegalStateException("Photo owner and post author disagree");
            String url = photoUrl.apply(image.key()); if (url == null || url.isBlank()) throw new IllegalStateException("Public photo URL unavailable");
            var view = new LinkedHashMap<String, Object>(); view.put("photoId", image.photoId()); view.put("url", url);
            view.put("contentType", image.mime()); view.put("sizeBytes", image.bytes());
            if (member != null && member.userId() == post.authorId()) view.put("fileId", image.fileId()); images.add(view);
        }
        data.put("images", images); var counts = snapshot.reactions();
        data.put("reactionCounts", Map.of("EMPATHY", counts.empathy(), "NEEDED", counts.needed(), "CURIOUS", counts.curious(),
                "total", Math.addExact(Math.addExact(counts.empathy(), counts.needed()), counts.curious())));
        data.put("commentCount", Math.addExact(snapshot.parentCommentCount(), snapshot.replyCount()));
        var commentPage = comments.initialPage(id); data.put("comments", commentPage.get("data"));
        @SuppressWarnings("unchecked") var meta = new LinkedHashMap<>((Map<String, Object>) commentPage.get("meta"));
        meta.put("sort", "LIKES"); data.put("commentsMeta", meta);
        var adoptions = "LOCAL_AGENDA".equals(post.type()) ? store.adoptions(id) : List.<JdbcPostDetailStore.Adoption>of();
        data.put("adoptions", adoptions.stream().map(a -> Map.of("institutionName", a.name(), "adoptedAt", a.at().atOffset(OFFSET))).toList());
        boolean voteOpen = false;
        if ("LOCAL_ACTIVITY".equals(post.type())) data.put("activity", activity(store.activity(id), store.organizerEmail(post.authorId())));
        if ("VOTE".equals(post.type())) {
            var poll = store.poll(id); voteOpen = now.isBefore(poll.endsAt());
            var result = snapshot.poll().orElseThrow(() -> new IllegalStateException("Vote result unavailable"));
            if (result.pollId() != poll.id()) throw new IllegalStateException("Vote source mismatch");
            var options = store.options(poll.id());
            if (!options.stream().map(JdbcPostDetailStore.Option::id).toList().equals(result.options().stream().map(ParticipationSnapshot.OptionVotes::optionId).toList()))
                throw new IllegalStateException("Vote options and participation disagree");
            var vote = new LinkedHashMap<String, Object>(); vote.put("question", poll.question());
            vote.put("status", voteOpen ? "OPEN" : "CLOSED"); vote.put("endsAt", poll.endsAt().atOffset(OFFSET)); vote.put("participantCount", result.participantCount());
            if (member != null) { var selected = snapshot.viewer().orElseThrow().selectedOptionId(); vote.put("myOptionId", selected.isPresent() ? selected.getAsLong() : null); }
            var optionViews = new ArrayList<Map<String, Object>>();
            for (int index = 0; index < options.size(); index++) {
                var option = options.get(index); long count = result.options().get(index).count();
                BigDecimal percentage = result.participantCount() == 0 ? BigDecimal.ZERO.setScale(2)
                        : BigDecimal.valueOf(count).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(result.participantCount()), 2, RoundingMode.HALF_UP);
                optionViews.add(Map.of("id", option.id(), "content", option.content(), "voteCount", count, "votePercentage", percentage));
            }
            vote.put("options", optionViews); data.put("vote", vote);
        }
        if (member != null) {
            var viewer = snapshot.viewer().orElseThrow();
            data.put("myState", Map.of("reactions", viewer.reactions().stream().sorted().map(Enum::name).toList(), "isBookmarked", viewer.bookmarked()));
        } else if (snapshot.viewer().isPresent()) throw new IllegalStateException("Guest snapshot contains private state");
        data.put("capabilities", capabilities(post, member, adoptions, voteOpen, now));
        data.put("createdAt", post.createdAt().atOffset(OFFSET)); data.put("updatedAt", post.updatedAt().atOffset(OFFSET));
        if (member == null && sharing.guestForRead(id, SharedPostAccess.Action.DETAIL).isEmpty()) throw new IllegalStateException("Guest context lost");
        return data;
    }
    private MemberQualification currentMember() {
        var actor = actors.current(); if (actor.isEmpty()) return null;
        var local = members.findByVerifiedSubject(actor.get().privySubject()).orElseThrow(() -> new IdentityFailure(IdentityFailure.Reason.NOT_REGISTERED));
        var qualification = members.find(local.userId()).orElseThrow(() -> new IdentityFailure(IdentityFailure.Reason.MEMBER_NOT_FOUND));
        if (qualification.registrationCompletedAt().isEmpty()) throw new IdentityFailure(IdentityFailure.Reason.INCOMPLETE);
        return qualification;
    }
    private static Map<String, Object> activity(JdbcPostDetailStore.Activity row, String email) {
        var out = new LinkedHashMap<String, Object>(); out.put("source", row.source()); out.put("schedule", row.schedule());
        out.put("place", row.place()); out.put("status", row.status());
        boolean allowed = false;
        try { var url = URI.create(row.url()); allowed = url.isAbsolute() && url.getHost() != null && Set.of("http", "https").contains(url.getScheme().toLowerCase(Locale.ROOT)); }
        catch (RuntimeException invalid) { /* Legacy invalid links are disabled. */ }
        out.put("externalParticipationUrl", allowed ? row.url() : null);
        out.put("externalParticipationEnabled", allowed && Set.of("SCHEDULED", "IN_PROGRESS").contains(row.status()));
        // User explicitly approved public activity contact for members and valid shared-link guests.
        // F-UCDVNA / API §5.3: author/adoption DTOs do not expose email.
        out.put("organizerEmail", email);
        return out;
    }
    private static Map<String, Boolean> capabilities(JdbcPostDetailStore.Base post, MemberQualification member,
            List<JdbcPostDetailStore.Adoption> adoptions, boolean voteOpen, Instant now) {
        boolean regional = member != null && member.verifiedRegionIds().contains(post.regionId());
        boolean editable = regional && member.userId() == post.authorId() && (!"VOTE".equals(post.type()) || voteOpen);
        var grants = member == null ? List.<InstitutionGrant>of() : member.institutionGrants().stream()
                .filter(g -> !now.isBefore(g.completedAt()) && now.isBefore(g.validUntil()) && g.responsibleRegionId() == post.regionId()).toList();
        boolean adopted = grants.stream().anyMatch(g -> adoptions.stream().anyMatch(a -> a.institutionId() == g.institutionId()));
        boolean institutional = "LOCAL_AGENDA".equals(post.type()) && !grants.isEmpty();
        return Map.of("canComment", member == null || regional, "canReact", regional, "canEvaluateComment", regional,
                "canVote", regional && "VOTE".equals(post.type()) && voteOpen, "canBookmark", member != null,
                "canEdit", editable, "canDelete", editable, "canAdopt", institutional && !adopted, "canCancelAdoption", institutional && adopted);
    }
}
