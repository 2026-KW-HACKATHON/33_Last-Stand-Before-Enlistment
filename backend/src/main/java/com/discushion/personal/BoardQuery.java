package com.discushion.personal;

import com.discushion.contracts.post.PostType;
import java.util.Set;
import org.springframework.util.MultiValueMap;

record BoardQuery(Long regionId,PostType type,String topic,int size,String cursor) {
    static final Set<String> TOPICS=Set.of("TRANSPORTATION","HOUSING","SAFETY","WELFARE","LIVING_INFORMATION","ENVIRONMENT","OTHER");
    static BoardQuery parse(MultiValueMap<String,String> params) {
        if(!Set.of("regionId","type","topic","size","cursor").containsAll(params.keySet()))throw new PersonalFailure("query");
        Long region=null;String raw=single(params,"regionId");
        if(raw!=null){if(!raw.matches("[0-9]{1,16}"))throw new PersonalFailure("regionId");region=Long.parseLong(raw);if(region<1||region>9007199254740991L)throw new PersonalFailure("regionId");}
        PostType type=null;raw=single(params,"type");
        if(raw!=null)try{type=PostType.valueOf(raw);}catch(IllegalArgumentException e){throw new PersonalFailure("type");}
        String topic=single(params,"topic");if(topic!=null&&!TOPICS.contains(topic))throw new PersonalFailure("topic");
        int size=20;raw=single(params,"size");
        if(raw!=null){if(!raw.matches("[0-9]{1,3}"))throw new PersonalFailure("size");size=Integer.parseInt(raw);if(size<1||size>100)throw new PersonalFailure("size");}
        return new BoardQuery(region,type,topic,size,single(params,"cursor"));
    }
    String filter(long region){return "BOARD:"+region+":"+(type==null?"ALL":type.name())+":"+(topic==null?"ALL":topic);}
    private static String single(MultiValueMap<String,String> params,String field){var values=params.get(field);if(values==null)return null;
        if(values.size()!=1||values.get(0)==null||values.get(0).isBlank())throw new PersonalFailure(field);return values.get(0);}
}
