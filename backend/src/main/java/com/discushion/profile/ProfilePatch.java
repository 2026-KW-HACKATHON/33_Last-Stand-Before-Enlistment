package com.discushion.profile;

import java.util.List;
import java.util.Map;
import java.util.Set;

record ProfilePatch(Map<String,Object> fields) {
    ProfilePatch {fields=java.util.Collections.unmodifiableMap(new java.util.HashMap<>(fields));}
    static ProfilePatch parse(Map<String,Object> fields) {
        if(fields==null || fields.isEmpty() || !Set.of("nickname","bio","residentAttributes","activityRegionId").containsAll(fields.keySet()))
            throw ProfileFailure.invalid("body");
        if(fields.containsKey("nickname")) {
            if(!(fields.get("nickname") instanceof String name) || name.isBlank() || name.codePoints().allMatch(ProfilePatch::blank)
                    || name.codePointCount(0,name.length())>10) throw ProfileFailure.invalid("nickname");
        }
        if(fields.containsKey("bio") && fields.get("bio")!=null && (!(fields.get("bio") instanceof String text)
                || text.codePointCount(0,text.length())>50)) throw ProfileFailure.invalid("bio");
        if(fields.containsKey("residentAttributes")) {
            if(!(fields.get("residentAttributes") instanceof List<?> values)
                    || values.stream().anyMatch(value->!(value instanceof String))
                    || Set.copyOf(values).size()!=values.size()
                    || !Set.of("RESIDENT","STUDENT","WORKER","MERCHANT").containsAll(values))
                throw ProfileFailure.invalid("residentAttributes");
        }
        if(fields.containsKey("activityRegionId")) {
            Object id=fields.get("activityRegionId");
            if(!(id instanceof Integer || id instanceof Long) || ((Number)id).longValue()<1 || ((Number)id).longValue()>9007199254740991L)
                throw ProfileFailure.invalid("activityRegionId");
        }
        return new ProfilePatch(fields);
    }
    private static boolean blank(int c) {
        return (c>=9 && c<=13) || c==32 || c==0x85 || c==0xA0 || c==0x1680 || (c>=0x2000 && c<=0x200A)
            || c==0x2028 || c==0x2029 || c==0x202F || c==0x205F || c==0x3000 || c==0xFEFF;
    }
}
