package com.discushion.map;
import org.junit.jupiter.api.Test;
import org.springframework.util.LinkedMultiValueMap;
import static org.assertj.core.api.Assertions.*;
class DongQueryTests {
    @Test void parsesRequiredCatalogIdsPreservingOrder(){var params=new LinkedMultiValueMap<String,String>();params.add("regionIds","18,15");assertThat(DongQuery.parse(params).regionIds()).containsExactly(18L,15L);assertThat(DongQuery.parse(params).centerRegionId()).isNull();}
    @Test void rejectsMissingMalformedDuplicateAndUnknown(){assertThatThrownBy(()->DongQuery.parse(new LinkedMultiValueMap<>())).isInstanceOf(MapFailure.class);for(String value:java.util.List.of("","15,15","0","9007199254740992","15,","15, 18")){var params=new LinkedMultiValueMap<String,String>();params.add("regionIds",value);assertThatThrownBy(()->DongQuery.parse(params)).isInstanceOf(MapFailure.class);}var params=new LinkedMultiValueMap<String,String>();params.add("regionIds","15");params.add("userId","1");assertThatThrownBy(()->DongQuery.parse(params)).isInstanceOf(MapFailure.class);}
}
