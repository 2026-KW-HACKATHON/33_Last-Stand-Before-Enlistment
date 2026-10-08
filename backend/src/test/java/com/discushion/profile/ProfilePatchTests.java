package com.discushion.profile;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class ProfilePatchTests {
    @Test void nullBioAndEmptyAttributesAreExplicitClearOperations() {
        var body=new HashMap<String,Object>();body.put("bio",null);body.put("residentAttributes",List.of());
        var patch=ProfilePatch.parse(body);assertThat(patch.fields()).containsKey("bio");
        assertThat(patch.fields()).doesNotContainKeys("nickname","activityRegionId");
    }
    @Test void nicknameUsesSignupUnicodeAndCodepointLimitsWithoutTrimming() {
        for(String name:List.of(""," ","\u00a0\u202f\ufeff","가".repeat(11)))
            assertThatThrownBy(()->ProfilePatch.parse(Map.of("nickname",name))).isInstanceOf(ProfileFailure.class);
        assertThat(ProfilePatch.parse(Map.of("nickname","😀".repeat(10))).fields().get("nickname")).isEqualTo("😀".repeat(10));
        assertThat(ProfilePatch.parse(Map.of("nickname","가\u00a0나")).fields().get("nickname")).isEqualTo("가\u00a0나");
    }
    @Test void limitsBioAndRejectsInvalidRegionTypes() {
        assertThatThrownBy(()->ProfilePatch.parse(Map.of("bio","가".repeat(51)))).isInstanceOf(ProfileFailure.class);
        for(Object id:List.of(0,-1,9007199254740992L,1.0,"1",true))
            assertThatThrownBy(()->ProfilePatch.parse(Map.of("activityRegionId",id))).isInstanceOf(ProfileFailure.class);
    }
    @Test void attributesAreDistinctKnownValues() {
        for(List<?> attributes:List.of(List.of("RESIDENT","RESIDENT"),List.of("INSTITUTION"),Arrays.asList((Object)null),List.of(1)))
            assertThatThrownBy(()->ProfilePatch.parse(Map.of("residentAttributes",attributes))).isInstanceOf(ProfileFailure.class);
    }
    @Test void rejectsEmptyUnknownAndPrivilegeOrPhotoFields() {
        assertThatThrownBy(()->ProfilePatch.parse(Map.of())).isInstanceOf(ProfileFailure.class);
        for(String field:List.of("email","userId","privySubject","institutionVerified","neighborVerifiedRegions","profileImage","removeProfileImage"))
            assertThatThrownBy(()->ProfilePatch.parse(Map.of(field,"synthetic"))).isInstanceOf(ProfileFailure.class);
    }
}
