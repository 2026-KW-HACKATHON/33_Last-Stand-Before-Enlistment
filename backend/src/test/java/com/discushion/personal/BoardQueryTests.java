package com.discushion.personal;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.util.LinkedMultiValueMap;
import static org.assertj.core.api.Assertions.*;

class BoardQueryTests {
    @Test void defaultsAndAllExplicitFilters(){
        var params=new LinkedMultiValueMap<String,String>();var empty=BoardQuery.parse(params);
        assertThat(empty.regionId()).isNull();assertThat(empty.size()).isEqualTo(20);
        params.add("regionId","15");params.add("type","LOCAL_ACTIVITY");params.add("topic","LIVING_INFORMATION");params.add("size","100");
        var query=BoardQuery.parse(params);assertThat(query.regionId()).isEqualTo(15L);assertThat(query.size()).isEqualTo(100);assertThat(query.topic()).isEqualTo("LIVING_INFORMATION");
    }
    @Test void unsupportedBlankDuplicateInvalidEnumsAndIdsAreRejected(){
        for(var pair:List.of(List.of("q","search"),List.of("userId","2"),List.of("regionId","0"),List.of("regionId","9007199254740992"),List.of("regionId","1.0"),List.of("type","ALL"),List.of("topic","NOPE"),List.of("size","101"),List.of("size","0"),List.of("cursor",""))){
            var params=new LinkedMultiValueMap<String,String>();params.add(pair.get(0),pair.get(1));assertThatThrownBy(()->BoardQuery.parse(params)).isInstanceOf(PersonalFailure.class);
        }
        var params=new LinkedMultiValueMap<String,String>();params.add("regionId","1");params.add("regionId","1");assertThatThrownBy(()->BoardQuery.parse(params)).isInstanceOf(PersonalFailure.class);
    }
}
