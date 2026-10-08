package com.discushion.bookmark;

import com.discushion.contracts.post.PostType;
import java.util.Set;
import org.springframework.util.MultiValueMap;

record BookmarkQuery(PostType type, String topic, int size, BookmarkCursor.Position after) {
    private static final Set<String> TOPICS = Set.of("TRANSPORTATION","HOUSING","SAFETY","WELFARE","LIVING_INFORMATION","ENVIRONMENT","OTHER");
    static BookmarkQuery parse(MultiValueMap<String,String> params) {
        if(params.keySet().stream().anyMatch(key->!Set.of("type","topic","size","cursor").contains(key)))
            throw BookmarkFailure.invalid("query");
        String typeValue=single(params,"type"), topic=single(params,"topic"), rawSize=single(params,"size"), rawCursor=single(params,"cursor");
        PostType type=null;
        if(typeValue!=null) try { type=PostType.valueOf(typeValue); } catch(IllegalArgumentException invalid) { throw BookmarkFailure.invalid("type"); }
        if(topic!=null&&!TOPICS.contains(topic)) throw BookmarkFailure.invalid("topic");
        int size=20;
        if(rawSize!=null) {
            if(!rawSize.matches("[0-9]{1,3}")) throw BookmarkFailure.invalid("size");
            size=Integer.parseInt(rawSize);
            if(size<1||size>100) throw BookmarkFailure.invalid("size");
        }
        var filter=new BookmarkCursor.Filter(type==null?null:type.name(),topic);
        return new BookmarkQuery(type,topic,size,rawCursor==null?null:BookmarkCursor.decode(rawCursor,filter));
    }
    private static String single(MultiValueMap<String,String> params,String field) {
        var values=params.get(field);
        if(values==null)return null;
        if(values.size()!=1||values.get(0)==null||values.get(0).isBlank())throw BookmarkFailure.invalid(field);
        return values.get(0);
    }
}
