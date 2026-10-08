package com.discushion.summary;
import com.discushion.contracts.identity.CurrentActorProvider;
import com.discushion.contracts.share.SharedPostAccess;
import com.discushion.identity.IdentityFailure;
import java.time.*;
import java.util.*;
import org.springframework.transaction.support.TransactionTemplate;
final class SummaryService {
    private final CurrentActorProvider actors;private final SharedPostAccess shares;private final JdbcSummaryStore store;
    private final SummaryGenerator generator;private final TransactionTemplate writes;
    private static final Duration PENDING_LIMIT=Duration.ofSeconds(60);
    SummaryService(CurrentActorProvider actors,SharedPostAccess shares,JdbcSummaryStore store,SummaryGenerator generator,TransactionTemplate writes){
        this.actors=actors;this.shares=shares;this.store=store;this.generator=generator;this.writes=writes;
    }
    private record Work(JdbcSummaryStore.Source source,Instant requested,Map<String,Object> response){}
    Map<String,Object> read(long id){
        var work=writes.execute(tx->{
            var source=authorized(id);var now=store.now();var state=store.state(id);
            if(state!=null && state.revision()==source.revision()){
                if("PENDING".equals(state.status()) && !now.isBefore(state.requested().plus(PENDING_LIMIT))){
                    store.finish(source,state.requested(),SummaryGenerator.Result.failed(),now);state=store.state(id);
                }
                return new Work(source,null,response(source,state));
            }
            // Missing provider configuration must not permanently poison a revision's cache.
            if(!generator.available())return new Work(source,null,response(source,new JdbcSummaryStore.State(source.revision(),"FAILED",null,now,null)));
            if(!store.claim(source,now))return new Work(source,null,response(source,store.state(id)));
            // Read database-rounded timestamp to fence completion, including microsecond precision.
            return new Work(source,store.state(id).requested(),null);
        });
        if(work.requested==null)return work.response;
        SummaryGenerator.Result result;
        try{result=generator.generate(work.source.title(),work.source.content());if(result==null)result=SummaryGenerator.Result.failed();}
        catch(RuntimeException failure){result=SummaryGenerator.Result.failed();}
        var generated=result;
        return writes.execute(tx->{
            var current=authorized(id);var now=store.now();
            if(current.revision()!=work.source.revision())
                return response(current,store.state(id));
            if(now.isBefore(work.requested.plus(PENDING_LIMIT)))store.finish(current,work.requested,generated,now);
            else store.finish(current,work.requested,SummaryGenerator.Result.failed(),now);
            return response(current,store.state(id));
        });
    }
    private JdbcSummaryStore.Source authorized(long id){
        var actor=actors.current();
        if(actor.isPresent()){
            var member=actor.get().member().orElseThrow(()->new IdentityFailure(IdentityFailure.Reason.NOT_REGISTERED));store.member(member.userId());
        }else{
            var guest=shares.guestForRead(id,SharedPostAccess.Action.SUMMARY);
            if(guest.isEmpty() || guest.get().postId()!=id)throw new IdentityFailure(IdentityFailure.Reason.INVALID_TOKEN);
        }
        return store.source(id);
    }
    private static Map<String,Object> response(JdbcSummaryStore.Source source,JdbcSummaryStore.State state){
        boolean current=state!=null && state.revision()==source.revision();String status=current?state.status():"PENDING";
        var data=new LinkedHashMap<String,Object>();data.put("postId",source.id());data.put("status",status);
        data.put("summary",current?state.summary():null);data.put("generatedAt",current && state.generated()!=null?state.generated().atOffset(ZoneOffset.ofHours(9)):null);
        data.put("source",Map.of("postId",source.id(),"title",source.title(),"content",source.content(),"authorDisplayName",source.author(),
            "updatedAt",source.updated().atOffset(ZoneOffset.ofHours(9)),"originalPath","/posts/"+source.id()));
        data.put("fallbackToSource",!"SUCCEEDED".equals(status));return Map.of("data",data);
    }
}
