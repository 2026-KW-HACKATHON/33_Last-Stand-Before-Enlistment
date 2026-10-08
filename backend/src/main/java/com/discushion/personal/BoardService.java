package com.discushion.personal;

import com.discushion.contracts.identity.CurrentActorProvider;
import com.discushion.contracts.participation.ParticipationSnapshotReader;
import com.discushion.contracts.post.*;
import com.discushion.identity.*;
import java.time.Clock;
import java.util.*;
import java.util.function.Supplier;
import org.springframework.transaction.support.TransactionTemplate;

final class BoardService {
    private final CurrentActorProvider actors;private final JdbcMemberStore members;private final JdbcBoardStore store;
    private final JdbcPersonalStore cards;private final PersonalService composer;
    private final Supplier<PostSummaryReader> summaries;private final Supplier<ParticipationSnapshotReader> participation;
    private final TransactionTemplate reads;private final Clock clock;
    BoardService(CurrentActorProvider actors,JdbcMemberStore members,JdbcBoardStore store,JdbcPersonalStore cards,PersonalService composer,
            Supplier<PostSummaryReader> summaries,Supplier<ParticipationSnapshotReader> participation,TransactionTemplate reads,Clock clock){
        this.actors=actors;this.members=members;this.store=store;this.cards=cards;this.composer=composer;this.summaries=summaries;
        this.participation=participation;this.reads=reads;this.clock=clock;
    }
    Map<String,Object> list(BoardQuery query){return reads.execute(tx->{
        var actor=actors.current().orElseThrow(()->new IdentityFailure(IdentityFailure.Reason.INVALID_TOKEN));
        var local=members.findByVerifiedSubject(actor.privySubject()).orElseThrow(()->new IdentityFailure(IdentityFailure.Reason.NOT_REGISTERED));
        var member=members.find(local.userId()).orElseThrow(()->new IdentityFailure(IdentityFailure.Reason.MEMBER_NOT_FOUND));
        if(member.registrationCompletedAt().isEmpty())throw new IdentityFailure(IdentityFailure.Reason.INCOMPLETE);
        long viewer=member.userId(),region=store.region(viewer,query.regionId());String filter=query.filter(region);
        var after=query.cursor()==null?null:PersonalCursor.decode(query.cursor(),viewer,filter);
        var rows=new ArrayList<>(store.page(region,query,after));boolean hasNext=rows.size()>query.size();if(hasNext)rows.remove(rows.size()-1);
        var ids=new HashSet<Long>();rows.forEach(row->ids.add(row.postId()));var now=clock.instant();
        var found=summaries.get().findAll(Set.copyOf(ids));var snapshots=participation.get().findAll(Set.copyOf(ids),OptionalLong.of(viewer));
        if(found==null||snapshots==null)throw new IllegalStateException("Board source unavailable");
        var bases=cards.bases(ids,viewer,now);var options=cards.options(ids);var data=new ArrayList<Map<String,Object>>();
        for(var row:rows){var post=found.get(row.postId());var snapshot=snapshots.get(row.postId());var base=bases.get(row.postId());
            if(post==null||post.postId()!=row.postId()||post.status()!=PostStatus.PUBLISHED||post.regionId()!=region||snapshot==null||snapshot.postId()!=row.postId()||base==null)
                throw new IllegalStateException("Board source mismatch");
            data.add(composer.card(post,base,snapshot,options.getOrDefault(row.postId(),List.of()),member,now));}
        var meta=new LinkedHashMap<String,Object>();meta.put("hasNext",hasNext);meta.put("nextCursor",hasNext?PersonalCursor.encode(viewer,filter,rows.get(rows.size()-1)):null);
        return Map.<String,Object>of("data",data,"meta",meta);
    });}
}
