package com.discushion.home;
import org.junit.jupiter.api.Test;
import org.springframework.util.LinkedMultiValueMap;
import static org.assertj.core.api.Assertions.*;
class HomeQueryTests {
    @Test void omittedAndPositiveRegion(){var values=new LinkedMultiValueMap<String,String>();assertThat(HomeQuery.parse(values).regionId()).isNull();values.add("regionId","15");assertThat(HomeQuery.parse(values).regionId()).isEqualTo(15);}
    @Test void malformedDuplicateAndUnknownFields(){for(String value:java.util.List.of("","-1","0","1.0","9007199254740992"," 15")){var params=new LinkedMultiValueMap<String,String>();params.add("regionId",value);assertThatThrownBy(()->HomeQuery.parse(params)).isInstanceOf(HomeFailure.class);}var params=new LinkedMultiValueMap<String,String>();params.add("regionId","15");params.add("regionId","16");assertThatThrownBy(()->HomeQuery.parse(params)).isInstanceOf(HomeFailure.class);params.clear();params.add("userId","1");assertThatThrownBy(()->HomeQuery.parse(params)).isInstanceOf(HomeFailure.class);}
}
