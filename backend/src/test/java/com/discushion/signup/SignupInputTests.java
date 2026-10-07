package com.discushion.signup;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class SignupInputTests {
    private Map<String,Object> body(Map<String,Object> agreements,Map<String,Object> profile) {
        return Map.of("agreements",agreements,"profile",profile);
    }
    @Test void optionalMarketingBioAndAttributesHaveExplicitEmptyDefaults() {
        var input=SignupInput.parse(body(Map.of("termsOfService",true,"privacyCollection",true),Map.of("nickname","주민","activityRegionId",1)));
        input.validate();
        assertThat(input.marketing()).isFalse();assertThat(input.bio()).isNull();assertThat(input.attributes()).isEmpty();
    }
    @Test void requiredAgreementsRemainRequired() {
        assertThatThrownBy(()->new SignupInput(false,true,false,"주민",null,List.of(),1).validate())
            .isInstanceOfSatisfying(SignupFailure.class,e->assertThat(e.reason).isEqualTo(SignupFailure.Reason.REQUIRED_AGREEMENT_MISSING));
    }
    @Test void nicknameUsesDbCompatibleUnicodeCodePointLengthWithoutNormalization() {
        new SignupInput(true,true,false,"😀".repeat(10),null,List.of(),1).validate();
        assertThatThrownBy(()->new SignupInput(true,true,false,"😀".repeat(11),null,List.of(),1).validate()).isInstanceOf(SignupFailure.class);
        assertThatThrownBy(()->new SignupInput(true,true,false,"  ",null,List.of(),1).validate()).isInstanceOf(SignupFailure.class);
    }
    @Test void introductionAndIdentifiersKeepEstablishedLimits() {
        new SignupInput(true,true,false,"주민","가".repeat(50),List.of(),9007199254740991L).validate();
        assertThatThrownBy(()->new SignupInput(true,true,false,"주민","가".repeat(51),List.of(),1).validate()).isInstanceOf(SignupFailure.class);
        for(long id:new long[]{0,-1,9007199254740992L}) assertThatThrownBy(()->new SignupInput(true,true,false,"주민",null,List.of(),id).validate()).isInstanceOf(SignupFailure.class);
    }
    @Test void attributesAreNonAuthorizationEnumsAndDuplicateValuesAreRejected() {
        new SignupInput(true,true,false,"주민",null,List.of("RESIDENT","STUDENT","WORKER","MERCHANT"),1).validate();
        for(var attrs:List.of(List.of("ADMIN"),List.of("RESIDENT","RESIDENT")))
            assertThatThrownBy(()->new SignupInput(true,true,false,"주민",null,attrs,1).validate()).isInstanceOf(SignupFailure.class);
    }
    @Test void clientIdentityPasswordPhotoAndReturnToCannotEnterSignupStorage() {
        for(String field:List.of("email","userId","subject","password","emailVerificationToken","returnTo")) {
            var body=new java.util.HashMap<String,Object>(body(Map.of("termsOfService",true,"privacyCollection",true),Map.of("nickname","주민","activityRegionId",1)));
            body.put(field,"client-value");assertThatThrownBy(()->SignupInput.parse(body)).isInstanceOf(SignupFailure.class);
        }
        assertThatThrownBy(()->SignupInput.parse(body(Map.of("termsOfService",true,"privacyCollection",true),
            Map.of("nickname","주민","activityRegionId",1,"profileImageFileId",5)))).isInstanceOf(SignupFailure.class);
    }
    @Test void nonBooleanAgreementsFractionalRegionAndInvalidArrayShapesAreRejected() {
        assertThatThrownBy(()->SignupInput.parse(body(Map.of("termsOfService","true","privacyCollection",true),Map.of("nickname","주민","activityRegionId",1)))).isInstanceOf(SignupFailure.class);
        assertThatThrownBy(()->SignupInput.parse(body(Map.of("termsOfService",true,"privacyCollection",true),Map.of("nickname","주민","activityRegionId",1.0)))).isInstanceOf(SignupFailure.class);
        assertThatThrownBy(()->SignupInput.parse(body(Map.of("termsOfService",true,"privacyCollection",true),Map.of("nickname","주민","activityRegionId",1,"residentAttributes",List.of(1))))).isInstanceOf(SignupFailure.class);
    }
    @Test void uiOnlyDecisionUsesAnExplicitMarkerInsteadOfInventingDocumentVersions() {
        assertThat(JdbcSignupStore.AGREEMENT_RECORD_MARKER).isEqualTo("MVP_UI_ONLY");
    }
}
