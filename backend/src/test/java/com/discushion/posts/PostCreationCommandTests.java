package com.discushion.posts;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class PostCreationCommandTests {
    @Test void parsesAgendaWithoutDetailsAndKeepsUnboundedText() {
        String longText = "주민 의견 ".repeat(20_000);
        var command = PostCreationCommand.parse(Map.of(
                "type", "LOCAL_AGENDA", "topic", "SAFETY", "regionId", 17,
                "title", longText, "content", longText));

        assertThat(command.type()).isEqualTo(com.discushion.contracts.post.PostType.LOCAL_AGENDA);
        assertThat(command.title()).isEqualTo(longText);
        assertThat(command.content()).isEqualTo(longText);
        assertThat(command.photoFileIds()).isEmpty();
    }

    @Test void activityRequiresNonblankRawScheduleAndAbsoluteHttpUrl() {
        var command = PostCreationCommand.parse(Map.of(
                "type", "LOCAL_ACTIVITY", "topic", "OTHER", "regionId", 3,
                "title", "활동", "content", "설명",
                "details", Map.of("source", "구청", "schedule", "임의 일정 문자열", "place", "주민센터",
                        "activityStatus", "SCHEDULED", "externalParticipationUrl", "https://example.org/event")));

        assertThat(command.activity().schedule()).isEqualTo("임의 일정 문자열");
        assertThat(command.activity().externalUrl()).isEqualTo("https://example.org/event");
        assertThatThrownBy(() -> PostCreationCommand.parse(Map.of(
                "type", "LOCAL_ACTIVITY", "topic", "OTHER", "regionId", 3,
                "title", "활동", "content", "설명",
                "details", Map.of("source", "구청", "schedule", "일정", "place", "주민센터",
                        "activityStatus", "SCHEDULED", "externalParticipationUrl", "javascript:alert(1)"))))
                .isInstanceOf(PostCreationFailure.class);
    }

    @Test void normalizesVoteOptionsAndParsesOffsetEndTime() {
        var command = PostCreationCommand.parse(Map.of(
                "type", "VOTE", "topic", "SAFETY", "regionId", 5,
                "title", "투표", "content", "설명",
                "details", Map.of("question", "질문", "options", List.of(" 찬성 ", "반대"),
                        "endsAt", "2026-10-10T18:00:00+09:00")));

        assertThat(command.vote().options()).containsExactly("찬성", "반대");
        assertThat(command.vote().endsAt()).isEqualTo(Instant.parse("2026-10-10T09:00:00Z"));
    }

    @Test void rejectsDuplicateNormalizedOptionsBlankFieldsMissingOffsetAndInvalidPhotoIds() {
        assertThatThrownBy(() -> PostCreationCommand.parse(vote(List.of("찬성", " 찬성 "), "2026-10-10T18:00:00+09:00")))
                .isInstanceOf(PostCreationFailure.class);
        assertThatThrownBy(() -> PostCreationCommand.parse(vote(List.of("찬성", "반대"), "2026-10-10T18:00:00")))
                .isInstanceOf(PostCreationFailure.class);
        var blankSchedule = Map.of("type", "LOCAL_ACTIVITY", "topic", "OTHER", "regionId", 3,
                "title", "활동", "content", "설명", "details", Map.of("source", "구청", "schedule", " ",
                        "place", "주민센터", "activityStatus", "SCHEDULED"));
        assertThatThrownBy(() -> PostCreationCommand.parse(blankSchedule)).isInstanceOf(PostCreationFailure.class);
        var invalidPhoto = new java.util.HashMap<String, Object>(vote(List.of("찬성", "반대"), "2026-10-10T18:00:00+09:00"));
        invalidPhoto.put("photoFileIds", List.of(0));
        assertThatThrownBy(() -> PostCreationCommand.parse(invalidPhoto)).isInstanceOf(PostCreationFailure.class);
    }

    private Map<String, Object> vote(List<String> options, String endsAt) {
        return Map.of("type", "VOTE", "topic", "SAFETY", "regionId", 5,
                "title", "투표", "content", "설명",
                "details", Map.of("question", "질문", "options", options, "endsAt", endsAt));
    }
}
