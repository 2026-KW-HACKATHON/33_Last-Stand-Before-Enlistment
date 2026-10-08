package com.discushion.home;

import org.springframework.util.MultiValueMap;

record HomeQuery(Long regionId) {
    static HomeQuery parse(MultiValueMap<String,String> params) {
        if(params.keySet().stream().anyMatch(k -> !k.equals("regionId"))) throw HomeFailure.invalid("query");
        var values=params.get("regionId");
        if(values==null) return new HomeQuery(null);
        if(values.size()!=1) throw HomeFailure.invalid("regionId");
        String value=values.get(0);
        if(value==null || !value.matches("[0-9]{1,16}")) throw HomeFailure.invalid("regionId");
        long id=Long.parseLong(value);
        if(id<1 || id>9_007_199_254_740_991L) throw HomeFailure.invalid("regionId");
        return new HomeQuery(id);
    }
}
