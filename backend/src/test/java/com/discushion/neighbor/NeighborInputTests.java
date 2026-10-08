package com.discushion.neighbor;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class NeighborInputTests {
    @Test void omittedTargetMeansListOnly() {assertThat(NeighborService.targetId(null)).isNull();}
    @Test void acceptsSafePositiveRegionIds() {
        assertThat(NeighborService.targetId("1")).isEqualTo(1L);
        assertThat(NeighborService.targetId("9007199254740991")).isEqualTo(9007199254740991L);
    }
    @Test void rejectsInvalidOrAmbiguousIds() {
        for(String id:new String[]{"","0","-1","1.0","1e3","01"," 1","1,2","9007199254740992","9999999999999999999999999","null"})
            assertThatThrownBy(()->NeighborService.targetId(id)).isInstanceOfSatisfying(NeighborFailure.class,
                error->assertThat(error.code).isEqualTo("VALIDATION_ERROR"));
    }
}
