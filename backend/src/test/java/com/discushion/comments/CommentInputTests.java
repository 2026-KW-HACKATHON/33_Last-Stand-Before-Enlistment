package com.discushion.comments;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class CommentInputTests {
    @Test void preservesTextAndAcceptsOnlyAgreedFields() {
        assertThat(CommentInput.parse(Map.of("content", " 의견 "), false)).isEqualTo(new CommentInput(" 의견 ", null));
        assertThat(CommentInput.parse(Map.of("content", "답글", "replyToCommentId", 31), true).replyToCommentId()).isEqualTo(31);
        for (String extra : List.of("userId", "authorName", "isGuest", "institutionVerified", "postId", "parentCommentId"))
            assertThatThrownBy(() -> CommentInput.parse(Map.of("content", "의견", extra, "untrusted"), false))
                .isInstanceOf(CommentFailure.class);
        assertThatThrownBy(() -> CommentInput.parse(Map.of("content", "의견", "replyToCommentId", 31), false))
            .isInstanceOf(CommentFailure.class);
    }
    @Test void rejectsMissingNonStringAndUnicodeBlankContent() {
        assertThatThrownBy(() -> CommentInput.parse(null, false)).isInstanceOf(CommentFailure.class);
        assertThatThrownBy(() -> CommentInput.parse(Map.of(), false)).isInstanceOf(CommentFailure.class);
        assertThatThrownBy(() -> CommentInput.parse(Map.of("content", 12), false)).isInstanceOf(CommentFailure.class);
        for (String value : List.of("", " ", "\t\r\n", "\u00a0\u0085\ufeff\u3000", "의견\u0000"))
            assertThatThrownBy(() -> CommentInput.parse(Map.of("content", value), true)).isInstanceOf(CommentFailure.class);
    }
    @Test void rejectsUnsafeOrNonIntegerTargetsAndInvalidPaths() {
        for (Object value : List.of(0, -1, 9007199254740992L, 1.0, "31"))
            assertThatThrownBy(() -> CommentInput.parse(Map.of("content", "의견", "replyToCommentId", value), true))
                .isInstanceOf(CommentFailure.class);
        var body = new HashMap<String, Object>(); body.put("content", "의견"); body.put("replyToCommentId", null);
        assertThatThrownBy(() -> CommentInput.parse(body, true)).isInstanceOf(CommentFailure.class);
        for (String value : List.of("0", "-1", "01", "1e2", "1.0", "9007199254740992"))
            assertThatThrownBy(() -> CommentInput.pathId(value, "postId")).isInstanceOf(CommentFailure.class);
        assertThat(CommentInput.pathId("9007199254740991", "postId")).isEqualTo(9007199254740991L);
    }
}
