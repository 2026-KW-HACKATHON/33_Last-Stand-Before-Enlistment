package com.discushion.profile;

import com.discushion.contracts.identity.CurrentActorProvider;
import com.discushion.identity.IdentityFailure;
import com.discushion.identity.MemberAuthorization;
import java.time.Clock;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.support.TransactionTemplate;

final class ProfileService {
    private final CurrentActorProvider actors; private final MemberAuthorization authorization;
    private final JdbcProfileStore store; private final TransactionTemplate readTx,writeTx; private final Clock clock;
    ProfileService(CurrentActorProvider actors,MemberAuthorization authorization,JdbcProfileStore store,
                   TransactionTemplate readTx,TransactionTemplate writeTx,Clock clock) {
        this.actors=actors;this.authorization=authorization;this.store=store;this.readTx=readTx;this.writeTx=writeTx;this.clock=clock;
    }
    ProfileView current() {
        var actor=actors.current().orElseThrow(()->new IdentityFailure(IdentityFailure.Reason.INVALID_TOKEN));
        return readTx.execute(status->store.read(store.completedUser(actor.privySubject()),clock.instant()));
    }
    ProfileView patch(ProfilePatch patch) {
        try {
            return writeTx.execute(status->{
                var member=authorization.lockCurrentCompletedMember();var now=clock.instant();
                store.update(member.userId(),patch,now);return store.read(member.userId(),now);
            });
        } catch(DataIntegrityViolationException error) {
            for(Throwable cause=error;cause!=null;cause=cause.getCause()) if(cause instanceof java.sql.SQLException sql) {
                var message=sql.getMessage();
                if("23505".equals(sql.getSQLState()) && message!=null && message.contains("profiles_nickname_key"))
                    throw new ProfileFailure("NICKNAME_ALREADY_IN_USE",409,"nickname");
                if("23503".equals(sql.getSQLState()) && message!=null && message.contains("profiles_activity_region_id_fkey"))
                    throw new ProfileFailure("REGION_NOT_FOUND",404,"activityRegionId");
            }
            throw error;
        }
    }
}
