package com.discushion.neighbor;

import com.discushion.contracts.identity.CurrentActorProvider;
import com.discushion.identity.IdentityFailure;
import org.springframework.transaction.support.TransactionTemplate;

final class NeighborService {
    private final CurrentActorProvider actors;private final JdbcNeighborStore store;private final TransactionTemplate reads;
    NeighborService(CurrentActorProvider actors,JdbcNeighborStore store,TransactionTemplate reads) {
        this.actors=actors;this.store=store;this.reads=reads;
    }
    NeighborView current(String regionId) {
        var actor=actors.current().orElseThrow(()->new IdentityFailure(IdentityFailure.Reason.INVALID_TOKEN));
        Long target=targetId(regionId);
        return reads.execute(status->{
            long user=store.currentCompletedUser(actor.privySubject());
            if(target!=null && !store.regionExists(target)) throw new NeighborFailure("REGION_NOT_FOUND",404);
            var regions=store.regions(user);
            if(regions.size()>NeighborDemoProvisioner.MAX_REGIONS) throw new IllegalStateException("Completed region source exceeds its limit");
            var selected=target==null?null:new NeighborView.TargetRegion(target,regions.stream().anyMatch(region->region.id()==target));
            return new NeighborView(regions,NeighborDemoProvisioner.MAX_REGIONS,selected);
        });
    }
    static Long targetId(String value) {
        if(value==null) return null;
        if(!value.matches("[1-9][0-9]{0,15}")) throw new NeighborFailure("VALIDATION_ERROR",400);
        try {long id=Long.parseLong(value);if(id>9007199254740991L) throw new NumberFormatException();return id;}
        catch(NumberFormatException failure) {throw new NeighborFailure("VALIDATION_ERROR",400);}
    }
}
