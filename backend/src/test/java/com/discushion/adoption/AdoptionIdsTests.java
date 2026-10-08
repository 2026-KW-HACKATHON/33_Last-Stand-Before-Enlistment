package com.discushion.adoption;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class AdoptionIdsTests {
    @Test void acceptsOnlyPositiveCanonicalJsonSafeIntegerPathValues() {
        assertThat(AdoptionIds.parse("15", "postId")).isEqualTo(15);
        assertThat(AdoptionIds.parse("9007199254740991", "adoptionId")).isEqualTo(9_007_199_254_740_991L);
        for (String value : new String[] {"0", "-1", "+1", "1.0", "9007199254740992", "12345678901234567", "x"}) {
            assertThatThrownBy(() -> AdoptionIds.parse(value, "postId"))
                    .isInstanceOf(AdoptionFailure.class)
                    .hasFieldOrPropertyWithValue("code", "VALIDATION_ERROR");
        }
        assertThat(AdoptionIds.parse("01", "postId")).isEqualTo(1);
    }
}
