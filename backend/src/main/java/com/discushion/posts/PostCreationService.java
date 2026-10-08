package com.discushion.posts;

import com.discushion.identity.MemberAuthorization;
import com.discushion.photos.PhotoAttachments;
import com.discushion.photos.PhotoFailure;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.transaction.support.TransactionTemplate;

final class PostCreationService {
    private final MemberAuthorization members;
    private final PostJdbcRepository posts;
    private final Supplier<PhotoAttachments> photoAttachments;
    private final TransactionTemplate transactions;

    PostCreationService(MemberAuthorization members, PostJdbcRepository posts,
            Supplier<PhotoAttachments> photoAttachments, TransactionTemplate transactions) {
        this.members = members;
        this.posts = posts;
        this.photoAttachments = photoAttachments;
        this.transactions = transactions;
    }

    Map<String, Object> create(Map<String, Object> body) {
        PostCreationCommand command = PostCreationCommand.parse(body);
        long postId = transactions.execute(status -> {
            var member = members.lockCurrentCompletedMember();
            members.requireVerifiedRegion(member, command.regionId());
            var now = posts.databaseNow();
            if (command.vote() != null && !now.isBefore(command.vote().endsAt())) throw new PostCreationFailure();

            long createdId = posts.insertPost(member.userId(), command, now);
            if (command.activity() != null) posts.insertActivity(createdId, command.activity());
            if (command.vote() != null) posts.insertVote(createdId, command.vote());
            if (!command.photoFileIds().isEmpty()) {
                PhotoAttachments attachments = photoAttachments.get();
                if (attachments == null) throw new PhotoFailure(PhotoFailure.Reason.PHOTO_STORAGE_UNAVAILABLE);
                var refs = command.photoFileIds().stream().map(id -> new PhotoAttachments.Reference(null, id)).toList();
                attachments.replace(createdId, refs);
            }
            return createdId;
        });
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("postId", postId);
        return Map.of("data", data);
    }
}
