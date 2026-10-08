package com.discushion.comments;

import java.util.List;

/** User delegated the initial minimal dictionary; literal inclusion only. */
final class CommentWords {
    static final List<String> WORDS = List.of("씨발", "개새끼", "병신", "좆같");
    static void requireAllowed(String content) {
        if (WORDS.stream().anyMatch(content::contains)) throw new CommentFailure("COMMENT_FORBIDDEN_WORD", 400, "content");
    }
}
