package com.discushion.map;

import java.util.*;
import org.springframework.util.MultiValueMap;

record DongQuery(Long centerRegionId,List<Long> regionIds) {
    DongQuery { regionIds=List.copyOf(regionIds); }
    static DongQuery parse(MultiValueMap<String,String> values) {
        if(values.keySet().stream().anyMatch(k->!Set.of("centerRegionId","regionIds").contains(k))) throw MapFailure.invalid("query");
        Long center=values.containsKey("centerRegionId")?id(single(values,"centerRegionId"),"centerRegionId"):null;
        var ids=new LinkedHashSet<Long>();
        for(String value:single(values,"regionIds").split(",",-1))
            if(!ids.add(id(value,"regionIds"))) throw MapFailure.invalid("regionIds");
        return new DongQuery(center,new ArrayList<>(ids));
    }
    private static String single(MultiValueMap<String,String> values,String field) {
        var list=values.get(field);
        if(list==null || list.size()!=1 || list.get(0)==null) throw MapFailure.invalid(field);
        return list.get(0);
    }
    private static long id(String value,String field) {
        if(!value.matches("[1-9][0-9]{0,15}")) throw MapFailure.invalid(field);
        long id=Long.parseLong(value);
        if(id>9007199254740991L) throw MapFailure.invalid(field);
        return id;
    }
}
