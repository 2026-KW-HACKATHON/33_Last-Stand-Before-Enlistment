package com.discushion.posts;

import com.discushion.contracts.identity.MemberQualification;
import com.discushion.contracts.participation.PostDeletionParticipant;
import com.discushion.contracts.post.PostContext;
import com.discushion.contracts.post.PostStatus;
import com.discushion.contracts.post.PostType;
import com.discushion.identity.MemberAuthorization;
import com.discushion.photos.PhotoAttachments;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.transaction.support.TransactionTemplate;

final class PostManagementService {
    private final MemberAuthorization members;
    private final PostJdbcStore posts;
    private final PhotoAttachments photos;
    private final PostDeletionParticipant deletion;
    private final Supplier<PostDetailLookup> details;
    private final TransactionTemplate transactions;

    PostManagementService(MemberAuthorization members, PostJdbcStore posts, PhotoAttachments photos,
            PostDeletionParticipant deletion, Supplier<PostDetailLookup> details,
            TransactionTemplate transactions) {
        this.members = members; this.posts = posts; this.photos = photos; this.deletion = deletion;
        this.details = details; this.transactions = transactions;
    }

    PatchResult patch(long postId, Map<String, Object> body) {
        return transactions.execute(status -> {
            MemberQualification member = members.lockCurrentCompletedMember();
            PostContext post = posts.findForUpdate(postId).orElseThrow(PostEditFailure::notFound);
            requirePublishedAndOwned(post, member, false);
            PostPatch patch = PostPatch.parse(body, post.type());
            PostJdbcStore.Editable current = posts.editable(postId);
            if (post.poll().isPresent() && !posts.databaseNow().isBefore(post.poll().orElseThrow().endsAt()))
                throw PostEditFailure.notEditable();
            long targetRegion = patch.regionId().supplied() ? patch.regionId().value() : current.regionId();
            if (patch.regionId().supplied() && !posts.regionExists(targetRegion)) throw PostEditFailure.invalid("regionId");
            members.requireVerifiedRegion(member, targetRegion);
            if (targetRegion != current.regionId() && posts.hasActiveAdoption(postId)) throw PostEditFailure.notEditable();
            validateValues(patch);
            if (post.poll().isPresent() && !posts.databaseNow().isBefore(post.poll().orElseThrow().endsAt()))
                throw PostEditFailure.notEditable();
            if (patch.endsAt().supplied() && !patch.endsAt().value().isAfter(posts.databaseNow()))
                throw PostEditFailure.invalid("details.endsAt");
            posts.update(postId, patch, current);
            List<Long> removed = patch.photoOrder().supplied() ? photos.replace(postId, patch.photoOrder().value()) : List.of();
            PostDetailLookup reader = details.get();
            if (reader == null) throw new IllegalStateException("Post detail reader is not available until #15 is integrated");
            Map<String, Object> response = reader.read(postId, member.userId());
            if (response == null) throw new IllegalStateException("Post detail reader returned no data");
            return new PatchResult(response, removed);
        });
    }

    void delete(long postId) {
        transactions.executeWithoutResult(status -> {
            MemberQualification member = members.lockCurrentCompletedMember();
            PostContext post = posts.findForUpdate(postId).orElseThrow(PostEditFailure::notFound);
            requirePublishedAndOwned(post, member, true);
            members.requireVerifiedRegion(member, post.regionId());
            if (post.poll().isPresent() && !posts.databaseNow().isBefore(post.poll().orElseThrow().endsAt()))
                throw PostEditFailure.notDeletable();
            deletion.removeBookmarks(postId);
            photos.replace(postId, List.of());
            posts.delete(postId);
        });
    }

    private void requirePublishedAndOwned(PostContext post, MemberQualification member, boolean deleting) {
        if (post.status() != PostStatus.PUBLISHED) throw PostEditFailure.notFound();
        if (post.type() == PostType.VOTE && post.poll().isEmpty()) throw new IllegalStateException("Published vote has no poll source");
        if (!members.isOwner(member, post.authorUserId())) {
            if (deleting) throw new PostEditFailure("POST_NOT_DELETABLE", 403, "postId");
            throw PostEditFailure.notEditable();
        }
    }

    private static void validateValues(PostPatch patch) {
        if (patch.topic().supplied() && !List.of("TRANSPORTATION", "HOUSING", "SAFETY", "WELFARE",
                "LIVING_INFORMATION", "ENVIRONMENT", "OTHER").contains(patch.topic().value()))
            throw PostEditFailure.invalid("topic");
        if (patch.activityStatus().supplied() && !List.of("SCHEDULED", "IN_PROGRESS", "ENDED", "CANCELED")
                .contains(patch.activityStatus().value())) throw PostEditFailure.invalid("details.activityStatus");
        if (patch.photoOrder().supplied() && patch.photoOrder().value().size() > 10)
            throw PostEditFailure.invalid("photoOrder");
    }

    record PatchResult(Map<String, Object> data, List<Long> removedFileIds) {
        PatchResult { removedFileIds = List.copyOf(removedFileIds); }
    }
}
