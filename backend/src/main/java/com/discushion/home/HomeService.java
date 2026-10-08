package com.discushion.home;

import com.discushion.contracts.identity.CurrentActorProvider;
import com.discushion.identity.IdentityFailure;
import java.util.*;
import org.springframework.transaction.support.TransactionTemplate;

final class HomeService {
    private final CurrentActorProvider actors; private final JdbcHomeStore store; private final TransactionTemplate reads;
    HomeService(CurrentActorProvider actors,JdbcHomeStore store,TransactionTemplate reads){this.actors=actors;this.store=store;this.reads=reads;}
    Map<String,Object> read(HomeQuery query){return reads.execute(tx->{
        var actor=actors.current().orElseThrow(()->new IdentityFailure(IdentityFailure.Reason.INVALID_TOKEN));
        var member=actor.member().orElseThrow(()->new IdentityFailure(IdentityFailure.Reason.NOT_REGISTERED));
        var region=store.region(member.userId(),query.regionId());
        var now=store.now();
        var posts=store.cards(region.id(),member.userId(),now,false,6);
        var votes=store.cards(region.id(),member.userId(),now,true,3);
        store.fillVotes(posts,votes);
        return Map.of("data",Map.of("region",Map.of("id",region.id(),"name",region.name()),
            "posts",posts,"openVotes",votes,"boardCounts",store.counts(region.id())));
    });}
}
