package com.discushion.comments;

import java.util.Map;
import java.util.Set;

/** Accepts content and an optional reply target, never a client-supplied author or badge. */
record CommentInput(String content, Long replyToCommentId) {
    static CommentInput parse(Map<String, Object> body, boolean reply) {
        Set<String> allowed = reply ? Set.of("content", "replyToCommentId") : Set.of("content");
        if (body == null || !allowed.containsAll(body.keySet())) throw CommentFailure.invalid("body");
        if (!(body.get("content") instanceof String text) || text.isEmpty() || text.indexOf('\0') >= 0
                || text.codePoints().allMatch(CommentInput::blank)) throw CommentFailure.invalid("content");
        Long target = null;
        if (body.containsKey("replyToCommentId")) {
            Object value = body.get("replyToCommentId");
            if (!(value instanceof Integer || value instanceof Long)) throw CommentFailure.invalid("replyToCommentId");
            target = ((Number) value).longValue();
            validId(target, "replyToCommentId");
        }
        // Preserve submitted text; no case folding, normalization or invented content limit.
        return new CommentInput(text, target);
    }
    static long pathId(String value, String field) {
        if (value == null || !value.matches("[1-9][0-9]{0,15}")) throw CommentFailure.invalid(field);
        try {
            long id = Long.parseLong(value); validId(id, field); return id;
        } catch (NumberFormatException invalid) { throw CommentFailure.invalid(field); }
    }
    private static void validId(long id, String field) {
        if (id < 1 || id > 9007199254740991L) throw CommentFailure.invalid(field);
    }
    private static boolean blank(int c) {
        return Character.isWhitespace(c) || Character.isSpaceChar(c) || c == 0x85 || c == 0xFEFF;
    }
}
