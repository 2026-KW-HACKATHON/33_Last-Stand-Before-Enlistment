package com.discushion.posts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import com.discushion.contracts.post.PostType;

class PostPatchTests {
    @Test void voteAllowsOnlyCommonFieldsPhotosAndEndTime() {
        var accepted = PostPatch.parse(Map.of("title", "수정 제목", "photoOrder", List.of(),
                "details", Map.of("endsAt", "2026-10-09T10:00:00+09:00")), PostType.VOTE);
        assertThat(accepted.title().value()).isEqualTo("수정 제목");
        assertThat(accepted.photoOrder().value()).isEmpty();
        assertThat(accepted.endsAt().value()).isNotNull();
        assertThatThrownBy(() -> PostPatch.parse(Map.of("regionId", 11), PostType.VOTE))
                .isInstanceOf(PostEditFailure.class).hasMessage("POST_TYPE_INVALID");
        assertThatThrownBy(() -> PostPatch.parse(Map.of("details", Map.of("question", "변경")), PostType.VOTE))
                .isInstanceOf(PostEditFailure.class).hasMessage("POST_TYPE_INVALID");
    }

    @Test void activityAllowsDetailFieldsAndNullableExternalUrlOnly() {
        var patch = PostPatch.parse(Map.of("details", Map.of("activityStatus", "CANCELED")), PostType.LOCAL_ACTIVITY);
        assertThat(patch.activityStatus().value()).isEqualTo("CANCELED");
        var nullable = new HashMap<String, Object>();
        nullable.put("details", new HashMap<>(Map.of("source", "주민센터")));
        ((Map<String, Object>) nullable.get("details")).put("externalParticipationUrl", null);
        assertThat(PostPatch.parse(nullable, PostType.LOCAL_ACTIVITY).externalParticipationUrl()).isEqualTo(
                new PostPatch.Field<>(true, null));
        assertThatThrownBy(() -> PostPatch.parse(Map.of("details", Map.of("question", "변경")), PostType.LOCAL_ACTIVITY))
                .isInstanceOf(PostEditFailure.class);
    }

    @Test void photoOrderRequiresUniqueSupportedPositiveSafeIdsAndNoUnknownFields() {
        assertThat(PostPatch.parse(Map.of("photoOrder", List.of(Map.of("fileId", 17L))), PostType.LOCAL_AGENDA)
                .photoOrder().value()).containsExactly(new com.discushion.photos.PhotoAttachments.Reference(null, 17L));
        for (Object invalid : List.of(Map.of("photoId", 0), Map.of("photoId", 1, "fileId", 2), Map.of("other", 1),
                Map.of("photoId", 9_007_199_254_740_992L))) {
            assertThatThrownBy(() -> PostPatch.parse(Map.of("photoOrder", List.of(invalid)), PostType.LOCAL_AGENDA))
                    .isInstanceOf(PostEditFailure.class);
        }
        assertThatThrownBy(() -> PostPatch.parse(Map.of("userId", 4), PostType.LOCAL_AGENDA))
                .isInstanceOf(PostEditFailure.class);
    }
    @Test void rejectsFractionalIdsAndUnsafeExternalUrls() {
        for (String value : List.of("1.00000000000000000001", "9007199254740991.1")) {
            var id = new java.math.BigDecimal(value);
            assertThatThrownBy(() -> PostPatch.parse(Map.of("regionId", id), PostType.LOCAL_AGENDA)).isInstanceOf(PostEditFailure.class);
            assertThatThrownBy(() -> PostPatch.parse(Map.of("photoOrder", List.of(Map.of("fileId", id))), PostType.LOCAL_AGENDA)).isInstanceOf(PostEditFailure.class);
        }
        for (String url : List.of("javascript:alert(1)", "/relative", "https://"))
            assertThatThrownBy(() -> PostPatch.parse(Map.of("details", Map.of("externalParticipationUrl", url)), PostType.LOCAL_ACTIVITY)).isInstanceOf(PostEditFailure.class);
    }
}
