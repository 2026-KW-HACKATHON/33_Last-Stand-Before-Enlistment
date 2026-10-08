package com.discushion.votes;

import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class VoteInputTests {
    @Test void parsesSafeIdsAndExplicitConfirmation() {
        assertThat(VoteInput.parse("12", Map.of("optionId", 3, "confirmChange", false)))
            .isEqualTo(new VoteInput(12, 3, false));
        assertThat(VoteInput.parse("9007199254740991", Map.of("optionId", 9007199254740991L, "confirmChange", true)))
            .isEqualTo(new VoteInput(9007199254740991L, 9007199254740991L, true));
    }

    @Test void rejectsUnknownOrMissingFieldsAndInvalidOptionIds() {
        assertInvalid(() -> VoteInput.parse("1", Map.of("optionId", 1)));
        assertInvalid(() -> VoteInput.parse("1", Map.of("optionId", 1, "confirmChange", false, "userId", 2)));
        for (Object value : new Object[]{0, -1, 1.5, new BigDecimal("1.1"), 9007199254740992L, "1", null}) {
            Map<String, Object> body = new java.util.HashMap<>();
            body.put("optionId", value);
            body.put("confirmChange", false);
            assertInvalid(() -> VoteInput.parse("1", body));
        }
        assertInvalid(() -> VoteInput.parse("1", Map.of("optionId", 1, "confirmChange", "true")));
    }

    @Test void rejectsNonCanonicalOrUnsafePostIds() {
        for (String id : new String[]{null, "", "0", "01", "-1", "+1", "1.0", "1e2", "9007199254740992", "99999999999999999"}) {
            assertInvalid(() -> VoteInput.parse(id, Map.of("optionId", 1, "confirmChange", false)));
        }
    }

    private static void assertInvalid(Runnable action) {
        assertThatThrownBy(action::run).isInstanceOf(VoteFailure.class)
            .extracting(error -> ((VoteFailure) error).status).isEqualTo(400);
    }
}
