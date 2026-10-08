package com.discushion.reactions;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class ReactionInputTests {
    @Test void acceptsExactlyTheThreeTypesAndJsonSafeIds() {
        for(String type:List.of("EMPATHY","NEEDED","CURIOUS"))assertThat(ReactionInput.parse("101",type,null).type().name()).isEqualTo(type);
        assertThat(ReactionInput.parse("9007199254740991","EMPATHY",null).postId()).isEqualTo(9007199254740991L);
    }
    @Test void rejectsUnsafeIdsUnknownTypesAndClientStateBodies() {
        for(String id:List.of("0","-1","01","1e2","1.0","9007199254740992"))
            assertThatThrownBy(()->ReactionInput.parse(id,"EMPATHY",null)).isInstanceOf(ReactionFailure.class);
        for(String type:List.of("empathy","LIKE","UNKNOWN"))
            assertThatThrownBy(()->ReactionInput.parse("101",type,null)).isInstanceOfSatisfying(ReactionFailure.class,error->assertThat(error.code).isEqualTo("REACTION_TYPE_INVALID"));
        for(String body:List.of("{}","{\"userId\":1}","{\"selected\":true}"))
            assertThatThrownBy(()->ReactionInput.parse("101","EMPATHY",body)).isInstanceOf(ReactionFailure.class);
    }
}
