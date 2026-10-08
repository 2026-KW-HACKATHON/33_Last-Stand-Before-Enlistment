package com.discushion.comments;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.util.LinkedMultiValueMap;
import static org.assertj.core.api.Assertions.*;

class CommentQueryTests {
    @Test void defaultsAndPageBoundariesUseTheAdoptedContract() {
        var params = new LinkedMultiValueMap<String, String>();
        assertThat(CommentQuery.parse(101, params)).isEqualTo(new CommentQuery(CommentQuery.Sort.LIKES, 20, null));
        params.set("sort", "LATEST"); params.set("size", "100");
        assertThat(CommentQuery.parse(101, params).size()).isEqualTo(100);
        for (String bad : List.of("0", "101", "-1", "1.0", "")) {
            params.set("size", bad); assertThatThrownBy(() -> CommentQuery.parse(101, params)).isInstanceOf(CommentFailure.class);
        }
    }
    @Test void cursorRoundTripIsBoundToSourceAndSortAndPreservesMicrosecondPrecision() {
        var p = new CommentQuery.Position(2, Instant.parse("2026-10-08T00:00:00.123456Z"), 31);
        var params = new LinkedMultiValueMap<String, String>();
        String cursor = CommentQuery.encode(101, CommentQuery.Sort.LIKES, p); params.set("cursor", cursor);
        assertThat(CommentQuery.parse(101, params).after()).isEqualTo(p);
        assertThatThrownBy(() -> CommentQuery.parse(102, params)).isInstanceOf(CommentFailure.class);
        params.set("sort", "LATEST"); assertThatThrownBy(() -> CommentQuery.parse(101, params)).isInstanceOf(CommentFailure.class);
        params.remove("sort");
        for (String bad : List.of("", "invalid", cursor + "=", cursor + "AA", "x".repeat(129))) {
            params.set("cursor", bad); assertThatThrownBy(() -> CommentQuery.parse(101, params)).isInstanceOf(CommentFailure.class);
        }
    }
    @Test void unknownSortExtraAndDuplicateParametersAreRejected() {
        var params = new LinkedMultiValueMap<String, String>(); params.set("sort", "likes");
        assertThatThrownBy(() -> CommentQuery.parse(101, params)).isInstanceOf(CommentFailure.class);
        params.clear(); params.add("size", "1"); params.add("size", "2");
        assertThatThrownBy(() -> CommentQuery.parse(101, params)).isInstanceOf(CommentFailure.class);
        params.clear(); params.set("userId", "31");
        assertThatThrownBy(() -> CommentQuery.parse(101, params)).isInstanceOf(CommentFailure.class);
    }
    @Test void minimalDictionaryOnlyBlocksLiteralInclusion() {
        for (String word : CommentWords.WORDS) {
            assertThatThrownBy(() -> CommentWords.requireAllowed("앞 " + word + " 뒤"))
                .isInstanceOfSatisfying(CommentFailure.class, error -> assertThat(error.code).isEqualTo("COMMENT_FORBIDDEN_WORD"));
        }
        for (String allowed : List.of("시발점에 대해 이야기합니다", "바보 같은 의견", "미친 일정", "씨 발", "ㅆㅂ"))
            assertThatCode(() -> CommentWords.requireAllowed(allowed)).doesNotThrowAnyException();
    }
}
