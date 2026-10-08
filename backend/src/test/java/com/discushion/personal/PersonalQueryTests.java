package com.discushion.personal;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.util.LinkedMultiValueMap;
import static org.assertj.core.api.Assertions.*;

class PersonalQueryTests {
    @Test void defaultsAndEndpointSpecificFilters() {
        var params=new LinkedMultiValueMap<String,String>();
        var query=PersonalQuery.parse(PersonalQuery.Kind.POSTS,params);
        assertThat(query.size()).isEqualTo(20);assertThat(query.type()).isNull();
        params.add("status","CLOSED");
        assertThat(PersonalQuery.parse(PersonalQuery.Kind.VOTES,params).status()).isEqualTo(PersonalQuery.Status.CLOSED);
        assertThatThrownBy(()->PersonalQuery.parse(PersonalQuery.Kind.POSTS,params)).isInstanceOf(PersonalFailure.class);
    }
    @Test void rejectsUnknownDuplicateBlankAndOutOfRangeInput() {
        for(String raw:List.of("0","101","-1","1.2","x","")){
            var params=new LinkedMultiValueMap<String,String>();params.add("size",raw);
            assertThatThrownBy(()->PersonalQuery.parse(PersonalQuery.Kind.PARTICIPATIONS,params)).isInstanceOf(PersonalFailure.class);
        }
        var params=new LinkedMultiValueMap<String,String>();params.add("userId","2");
        assertThatThrownBy(()->PersonalQuery.parse(PersonalQuery.Kind.POSTS,params)).isInstanceOf(PersonalFailure.class);
        params.clear();params.add("type","VOTE");params.add("type","VOTE");
        assertThatThrownBy(()->PersonalQuery.parse(PersonalQuery.Kind.POSTS,params)).isInstanceOf(PersonalFailure.class);
        params.clear();params.add("type","NOPE");
        assertThatThrownBy(()->PersonalQuery.parse(PersonalQuery.Kind.POSTS,params)).isInstanceOf(PersonalFailure.class);
    }
    @Test void cursorRoundTripsAndBindsMemberEndpointAndFilters() {
        var position=new PersonalCursor.Position(Instant.parse("2026-10-08T01:00:00.123456Z"),99);
        String cursor=PersonalCursor.encode(7,"POSTS:ALL:ALL",position);
        assertThat(PersonalCursor.decode(cursor,7,"POSTS:ALL:ALL")).isEqualTo(position);
        assertThatThrownBy(()->PersonalCursor.decode(cursor,8,"POSTS:ALL:ALL")).isInstanceOf(PersonalFailure.class);
        assertThatThrownBy(()->PersonalCursor.decode(cursor,7,"PARTICIPATIONS:ALL:ALL")).isInstanceOf(PersonalFailure.class);
        assertThatThrownBy(()->PersonalCursor.decode(cursor,7,"POSTS:VOTE:ALL")).isInstanceOf(PersonalFailure.class);
    }
    @Test void cursorRejectsMalformedNoncanonicalAndTrailingInput() {
        var position=new PersonalCursor.Position(Instant.EPOCH,1);String valid=PersonalCursor.encode(1,"VOTES:ALL:ALL",position);
        for(String raw:List.of("",valid+"=",valid+"A","!","a".repeat(513))){
            assertThatThrownBy(()->PersonalCursor.decode(raw,1,"VOTES:ALL:ALL")).isInstanceOf(PersonalFailure.class);
        }
    }
}
