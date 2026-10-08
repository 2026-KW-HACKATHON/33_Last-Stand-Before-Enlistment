package com.discushion.reactions;

import com.discushion.contracts.participation.ParticipationSnapshot.ReactionType;

record ReactionInput(long postId, ReactionType type) {
    static ReactionInput parse(String id, String type, String body) {
        if (body != null && !body.isBlank()) throw ReactionFailure.invalid("body");
        if (id == null || !id.matches("[1-9][0-9]{0,15}")) throw ReactionFailure.invalid("postId");
        long postId;
        try { postId = Long.parseLong(id); }
        catch (NumberFormatException error) { throw ReactionFailure.invalid("postId"); }
        if (postId > 9007199254740991L) throw ReactionFailure.invalid("postId");
        try { return new ReactionInput(postId, ReactionType.valueOf(type)); }
        catch (IllegalArgumentException | NullPointerException error) { throw new ReactionFailure("REACTION_TYPE_INVALID", 400, "reactionType"); }
    }
}
