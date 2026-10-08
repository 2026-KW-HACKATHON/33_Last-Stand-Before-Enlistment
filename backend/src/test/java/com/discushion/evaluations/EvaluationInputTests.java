package com.discushion.evaluations;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class EvaluationInputTests {
    @Test void acceptsOnlyLikeAndDislikeWithJsonSafeTarget() {
        for(String type:List.of("LIKE","DISLIKE"))assertThat(EvaluationInput.select("301",Map.of("type",type)).type().name()).isEqualTo(type);
        assertThat(EvaluationInput.pathId("9007199254740991")).isEqualTo(9007199254740991L);
    }
    @Test void rejectsUnknownTypesAndIdentityOrToggleFields() {
        for(String type:List.of("NONE","like","UNKNOWN"))assertThatThrownBy(()->EvaluationInput.select("301",Map.of("type",type)))
            .isInstanceOfSatisfying(EvaluationFailure.class,error->assertThat(error.code).isEqualTo("COMMENT_EVALUATION_TYPE_INVALID"));
        for(String field:List.of("userId","postId","isGuest","selected"))assertThatThrownBy(()->EvaluationInput.select("301",Map.of("type","LIKE",field,"untrusted"))).isInstanceOf(EvaluationFailure.class);
        assertThatThrownBy(()->EvaluationInput.select("301",Map.of())).isInstanceOf(EvaluationFailure.class);
        assertThatThrownBy(()->EvaluationInput.select("301",null)).isInstanceOf(EvaluationFailure.class);
        assertThatThrownBy(()->EvaluationInput.select("301",Map.of("type",1))).isInstanceOf(EvaluationFailure.class);
    }
    @Test void rejectsNonCanonicalAndUnsafeIds() {
        for(String id:List.of("0","-1","01","1.0","1e3","9007199254740992"))assertThatThrownBy(()->EvaluationInput.pathId(id)).isInstanceOf(EvaluationFailure.class);
    }
}
