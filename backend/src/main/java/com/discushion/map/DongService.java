package com.discushion.map;
import com.discushion.contracts.identity.CurrentActorProvider;
import com.discushion.identity.IdentityFailure;
import java.util.*;
import org.springframework.transaction.support.TransactionTemplate;
final class DongService {
    private final CurrentActorProvider actors;private final JdbcDongStore store;private final TransactionTemplate reads;
    DongService(CurrentActorProvider actors,JdbcDongStore store,TransactionTemplate reads){this.actors=actors;this.store=store;this.reads=reads;}
    Map<String,Object> read(DongQuery query){return reads.execute(tx->{
        var actor=actors.current().orElseThrow(()->new IdentityFailure(IdentityFailure.Reason.INVALID_TOKEN));
        var member=actor.member().orElseThrow(()->new IdentityFailure(IdentityFailure.Reason.NOT_REGISTERED));
        long center=store.center(member.userId(),query.centerRegionId());
        var required=new LinkedHashSet<>(query.regionIds());required.add(center);
        var regions=store.regions(required);
        if(regions.size()!=required.size())throw new MapFailure(404,"REGION_NOT_FOUND",null);
        var cards=store.representatives(query.regionIds());
        var dongs=new ArrayList<Map<String,Object>>();
        for(long id:query.regionIds()){
            var dong=new LinkedHashMap<String,Object>();dong.put("region",regions.get(id));dong.put("representativePost",cards.get(id));dongs.add(dong);
        }
        return Map.of("data",Map.of("centerRegion",regions.get(center),"dongs",dongs));
    });}
}
