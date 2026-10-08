package com.discushion.neighbor;

import com.discushion.identity.JdbcMemberStore;
import java.sql.Timestamp;
import java.time.Clock;
import javax.sql.DataSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/** #75 internal admin/seed contract. No Spring bean/controller; never use the server account to grant qualifications. */
public final class NeighborDemoProvisioner {
    public static final int MAX_REGIONS=3;
    private final JdbcMemberStore members;private final JdbcNeighborStore store;
    private final TransactionTemplate writes;private final Clock clock;
    public NeighborDemoProvisioner(DataSource adminSource,PlatformTransactionManager adminTransactions,Clock clock) {
        this.members=new JdbcMemberStore(adminSource,clock);this.store=new JdbcNeighborStore(adminSource);
        this.writes=new TransactionTemplate(adminTransactions);this.clock=clock;
    }
    /** Same region is idempotent and preserves verified_at. Parent lock serializes all cooperating seed writers. */
    public void add(long userId,long regionId) {
        if(userId<1 || userId>9007199254740991L || regionId<1 || regionId>9007199254740991L)
            throw new IllegalArgumentException("Invalid seed identity");
        if(TransactionSynchronizationManager.isActualTransactionActive())
            throw new IllegalStateException("Seed provisioning requires its own transaction");
        writes.executeWithoutResult(status->{
            var member=members.lockAndRead(userId).orElseThrow(()->new IllegalArgumentException("Unknown seed member"));
            if(member.registrationCompletedAt().isEmpty()) throw new IllegalArgumentException("Seed member must complete signup");
            if(!store.regionExists(regionId)) throw new IllegalArgumentException("Unknown seed region");
            if(member.verifiedRegionIds().contains(regionId)) return;
            if(member.verifiedRegionIds().size()>=MAX_REGIONS) throw new IllegalStateException("Completed region limit is 3");
            store.jdbc.update("insert into discushion.neighbor_verified_regions(user_id,region_id,source_request_id,verified_at) values(?,?,null,?)",
                userId,regionId,Timestamp.from(clock.instant()));
        });
    }
}
