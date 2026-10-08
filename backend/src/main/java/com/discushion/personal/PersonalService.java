package com.discushion.personal;

import com.discushion.contracts.identity.*;
import com.discushion.contracts.participation.*;
import com.discushion.contracts.post.*;
import com.discushion.identity.*;
import java.math.*;
import java.time.*;
import java.util.*;
import java.util.function.*;
import org.springframework.transaction.support.TransactionTemplate;

final class PersonalService {
    private static final ZoneOffset OFFSET=ZoneOffset.ofHours(9);
    private final CurrentActorProvider actors; private final JdbcMemberStore members;
    private final Supplier<PostSummaryReader> summaries; private final Supplier<ParticipationSnapshotReader> participation;
    private final JdbcPersonalStore store; private final Function<String,String> photoUrl;
    private final TransactionTemplate reads; private final Clock clock;
    PersonalService(CurrentActorProvider actors,JdbcMemberStore members,Supplier<PostSummaryReader> summaries,
            Supplier<ParticipationSnapshotReader> participation,JdbcPersonalStore store,Function<String,String> photoUrl,
            TransactionTemplate reads,Clock clock) {
        this.actors=actors;this.members=members;this.summaries=summaries;this.participation=participation;
        this.store=store;this.photoUrl=photoUrl;this.reads=reads;this.clock=clock;
    }
    Map<String,Object> list(PersonalQuery query) { return reads.execute(tx->{
        var actor=actors.current().orElseThrow(()->new IdentityFailure(IdentityFailure.Reason.INVALID_TOKEN));
        var local=members.findByVerifiedSubject(actor.privySubject()).orElseThrow(()->new IdentityFailure(IdentityFailure.Reason.NOT_REGISTERED));
        var member=members.find(local.userId()).orElseThrow(()->new IdentityFailure(IdentityFailure.Reason.MEMBER_NOT_FOUND));
        if(member.registrationCompletedAt().isEmpty())throw new IdentityFailure(IdentityFailure.Reason.INCOMPLETE);
        long viewer=member.userId();Instant now=clock.instant();
        var after=query.cursor()==null?null:PersonalCursor.decode(query.cursor(),viewer,query.filter());
        var rows=new ArrayList<>(store.page(viewer,query,after,now));boolean hasNext=rows.size()>query.size();
        if(hasNext)rows.remove(rows.size()-1);
        var ids=new HashSet<Long>();rows.forEach(row->ids.add(row.postId()));
        var found=summaries.get().findAll(Set.copyOf(ids));
        if(found==null)throw new IllegalStateException("Post summaries unavailable");
        var published=new HashSet<Long>();
        for(var row:rows){var post=found.get(row.postId());if(post==null||post.postId()!=row.postId())throw new IllegalStateException("Personal source mismatch");
            if(post.status()==PostStatus.PUBLISHED)published.add(post.postId());}
        var snapshots=participation.get().findAll(Set.copyOf(published),OptionalLong.of(viewer));
        if(snapshots==null)throw new IllegalStateException("Participation source unavailable");
        var bases=store.bases(published,viewer,now);var options=store.options(published);
        var items=new ArrayList<Map<String,Object>>();
        for(var row:rows){var post=found.get(row.postId());
            if(post.status()!=PostStatus.PUBLISHED){
                if(query.kind()!=PersonalQuery.Kind.VOTES||query.status()!=PersonalQuery.Status.ALL||post.type()!=PostType.VOTE)throw new IllegalStateException("Unavailable record outside vote history");
                var unavailable=new LinkedHashMap<String,Object>();unavailable.put("postId",row.postId());unavailable.put("availability","UNAVAILABLE");
                unavailable.put("participatedAt",row.participatedAt().atOffset(OFFSET));unavailable.put("post",null);unavailable.put("vote",null);items.add(unavailable);continue;
            }
            var base=bases.get(post.postId());var snapshot=snapshots.get(post.postId());
            if(base==null||snapshot==null||snapshot.postId()!=post.postId())throw new IllegalStateException("Published personal source unavailable");
            var card=card(post,base,snapshot,options.getOrDefault(post.postId(),List.of()),member,now);
            if(query.kind()==PersonalQuery.Kind.PARTICIPATIONS){
                var mine=snapshot.viewer().orElseThrow();card.put("myParticipation",Map.of("reactions",mine.reactions().stream().sorted().map(Enum::name).toList(),
                        "hasCommentOrReply",base.commented(),"hasCommentOrReplyEvaluation",base.evaluated(),"hasVote",mine.selectedOptionId().isPresent()));
                card.put("participatedAt",row.at().atOffset(OFFSET));
            }
            if(query.kind()==PersonalQuery.Kind.VOTES){
                var item=new LinkedHashMap<String,Object>();item.put("postId",post.postId());item.put("availability","AVAILABLE");
                item.put("participatedAt",row.participatedAt().atOffset(OFFSET));item.put("post",card);item.put("vote",card.remove("vote"));items.add(item);
            }else items.add(card);
        }
        var meta=new LinkedHashMap<String,Object>();meta.put("hasNext",hasNext);
        meta.put("nextCursor",hasNext?PersonalCursor.encode(viewer,query.filter(),new PersonalCursor.Position(rows.get(rows.size()-1).at(),rows.get(rows.size()-1).postId())):null);
        return Map.<String,Object>of("data",items,"meta",meta);
    }); }
    private Map<String,Object> card(PostSummary post,JdbcPersonalStore.Base base,ParticipationSnapshot snapshot,
            List<JdbcPersonalStore.Option> options,MemberQualification member,Instant now){
        var display=post.display().orElseThrow();var out=new LinkedHashMap<String,Object>();
        out.put("id",post.postId());out.put("type",post.type());out.put("topic",display.topic());out.put("title",display.title());out.put("excerpt",base.excerpt());
        out.put("region",Map.of("id",post.regionId(),"name",base.regionName()));
        var author=new LinkedHashMap<String,Object>();author.put("displayName",base.nickname());author.put("profileImageUrl",null);author.put("institutionVerified",base.institutionVerified());out.put("author",author);
        String url=base.thumbnailKey()==null?null:photoUrl.apply(base.thumbnailKey());
        if(base.thumbnailKey()!=null&&(url==null||url.isBlank()))throw new IllegalStateException("Photo URL unavailable");out.put("thumbnailUrl",url);
        var counts=snapshot.reactions();out.put("reactionCounts",Map.of("EMPATHY",counts.empathy(),"NEEDED",counts.needed(),"CURIOUS",counts.curious(),"total",Math.addExact(Math.addExact(counts.empathy(),counts.needed()),counts.curious())));
        out.put("commentCount",Math.addExact(snapshot.parentCommentCount(),snapshot.replyCount()));
        out.put("createdAt",post.createdAt().atOffset(OFFSET));out.put("updatedAt",base.updatedAt().atOffset(OFFSET));
        boolean editable=post.authorUserId()==member.userId()&&member.verifiedRegionIds().contains(post.regionId());
        if(post.type()==PostType.LOCAL_ACTIVITY){if(base.activityStatus()==null)throw new IllegalStateException("Activity source unavailable");out.put("activityStatus",base.activityStatus());}
        if(post.type()==PostType.VOTE){
            var result=snapshot.poll().orElseThrow();if(base.pollId()==null||result.pollId()!=base.pollId()||base.endsAt()==null)throw new IllegalStateException("Poll source mismatch");
            if(!options.stream().map(JdbcPersonalStore.Option::id).toList().equals(result.options().stream().map(ParticipationSnapshot.OptionVotes::optionId).toList()))throw new IllegalStateException("Poll options mismatch");
            var vote=new LinkedHashMap<String,Object>();boolean open=now.isBefore(base.endsAt());editable&=open;
            vote.put("question",base.question());vote.put("status",open?"OPEN":"CLOSED");vote.put("endsAt",base.endsAt().atOffset(OFFSET));vote.put("participantCount",result.participantCount());
            var choice=snapshot.viewer().orElseThrow().selectedOptionId();vote.put("myOptionId",choice.isPresent()?choice.getAsLong():null);
            var choices=new ArrayList<Map<String,Object>>();
            for(int i=0;i<options.size();i++){var option=options.get(i);long count=result.options().get(i).count();
                BigDecimal percentage=result.participantCount()==0?BigDecimal.ZERO.setScale(2):BigDecimal.valueOf(count).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(result.participantCount()),2,RoundingMode.HALF_UP);
                choices.add(Map.of("id",option.id(),"content",option.content(),"voteCount",count,"votePercentage",percentage));}
            vote.put("options",choices);out.put("vote",vote);
        }
        out.put("capabilities",Map.of("canEdit",editable,"canDelete",editable));return out;
    }
}
