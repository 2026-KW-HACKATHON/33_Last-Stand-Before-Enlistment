package com.discushion.officer;

import java.util.Set;
import org.springframework.util.MultiValueMap;

record OfficerAgendaQuery(OfficerAgendaScope scope, Long regionId, int size, String cursor) {
    private static final long MAX_ID = 9_007_199_254_740_991L;

    static OfficerAgendaQuery parse(MultiValueMap<String, String> params) {
        if (params.keySet().stream().anyMatch(key -> !Set.of("scope", "regionId", "size", "cursor").contains(key))) {
            throw OfficerAgendaFailure.invalid("query");
        }
        String rawScope = single(params, "scope");
        OfficerAgendaScope scope = OfficerAgendaScope.ALL;
        if (rawScope != null) {
            try {
                scope = OfficerAgendaScope.valueOf(rawScope);
            } catch (IllegalArgumentException invalid) {
                throw OfficerAgendaFailure.invalid("scope");
            }
        }
        String rawRegion = single(params, "regionId");
        Long regionId = null;
        if (rawRegion != null) {
            if (!rawRegion.matches("[0-9]{1,16}")) throw OfficerAgendaFailure.invalid("regionId");
            try {
                regionId = Long.parseLong(rawRegion);
            } catch (NumberFormatException invalid) {
                throw OfficerAgendaFailure.invalid("regionId");
            }
            if (regionId < 1 || regionId > MAX_ID) throw OfficerAgendaFailure.invalid("regionId");
        }
        String rawSize = single(params, "size");
        int size = 20;
        if (rawSize != null) {
            if (!rawSize.matches("[0-9]{1,3}")) throw OfficerAgendaFailure.invalid("size");
            size = Integer.parseInt(rawSize);
            if (size < 1 || size > 100) throw OfficerAgendaFailure.invalid("size");
        }
        return new OfficerAgendaQuery(scope, regionId, size, single(params, "cursor"));
    }

    private static String single(MultiValueMap<String, String> params, String field) {
        var values = params.get(field);
        if (values == null) return null;
        if (values.size() != 1 || values.get(0) == null || values.get(0).isBlank()) {
            throw OfficerAgendaFailure.invalid(field);
        }
        return values.get(0);
    }
}
