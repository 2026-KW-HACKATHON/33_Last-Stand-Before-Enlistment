package com.discushion.signup;

import java.util.List;
import java.util.Map;
import java.util.Set;
import static com.discushion.signup.SignupFailure.invalid;

record SignupInput(boolean terms, boolean privacy, boolean marketing, String nickname, String bio,
                   List<String> attributes, long regionId) {
    SignupInput { attributes=List.copyOf(attributes); }
    void validate() {
        if (!terms || !privacy) throw new SignupFailure(SignupFailure.Reason.REQUIRED_AGREEMENT_MISSING, "agreements");
        if (nickname==null || nickname.isBlank() || nickname.codePoints().allMatch(SignupInput::dbBlank)
                || nickname.codePointCount(0,nickname.length())>10)
            throw invalid("profile.nickname");
        if (bio!=null && bio.codePointCount(0,bio.length())>50) throw invalid("profile.bio");
        if (regionId<1 || regionId>9007199254740991L) throw invalid("profile.activityRegionId");
        if (Set.copyOf(attributes).size()!=attributes.size()
                || !Set.of("RESIDENT","STUDENT","WORKER","MERCHANT").containsAll(attributes))
            throw invalid("profile.residentAttributes");
    }
    /** Matches profiles_nickname_nonblank's Unicode White_Space + BOM, without trimming valid names. */
    private static boolean dbBlank(int codePoint) {
        return (codePoint>=0x0009 && codePoint<=0x000D) || codePoint==0x0020 || codePoint==0x0085
            || codePoint==0x00A0 || codePoint==0x1680 || (codePoint>=0x2000 && codePoint<=0x200A)
            || codePoint==0x2028 || codePoint==0x2029 || codePoint==0x202F || codePoint==0x205F
            || codePoint==0x3000 || codePoint==0xFEFF;
    }
    static SignupInput parse(Map<String,Object> body) {
        if (body==null || !body.keySet().equals(Set.of("agreements","profile"))) throw invalid("body");
        var agreements=object(body.get("agreements"),"agreements");
        var profile=object(body.get("profile"),"profile");
        if (!Set.of("termsOfService","privacyCollection","marketing").containsAll(agreements.keySet())
                || !agreements.keySet().containsAll(Set.of("termsOfService","privacyCollection"))) throw invalid("agreements");
        if (!Set.of("nickname","bio","residentAttributes","activityRegionId").containsAll(profile.keySet())
                || !profile.keySet().containsAll(Set.of("nickname","activityRegionId"))) throw invalid("profile");
        boolean terms=bool(agreements.get("termsOfService"),"agreements.termsOfService");
        boolean privacy=bool(agreements.get("privacyCollection"),"agreements.privacyCollection");
        boolean marketing=agreements.containsKey("marketing")?bool(agreements.get("marketing"),"agreements.marketing"):false;
        if (!(profile.get("nickname") instanceof String nickname)) throw invalid("profile.nickname");
        Object bio=profile.get("bio");
        if (bio!=null && !(bio instanceof String)) throw invalid("profile.bio");
        Object region=profile.get("activityRegionId");
        if (!(region instanceof Integer || region instanceof Long)) throw invalid("profile.activityRegionId");
        Object attributes=profile.getOrDefault("residentAttributes",List.of());
        if (!(attributes instanceof List<?> items) || items.stream().anyMatch(item->!(item instanceof String)))
            throw invalid("profile.residentAttributes");
        return new SignupInput(terms,privacy,marketing,nickname,(String)bio,
            items.stream().map(String.class::cast).toList(),((Number)region).longValue());
    }
    @SuppressWarnings("unchecked")
    private static Map<String,Object> object(Object value,String field) {
        if (!(value instanceof Map<?,?> map) || map.keySet().stream().anyMatch(key->!(key instanceof String))) throw invalid(field);
        return (Map<String,Object>)map;
    }
    private static boolean bool(Object value,String field) { if (!(value instanceof Boolean flag)) throw invalid(field); return flag; }
}
