package com.discushion.posts;

import com.discushion.contracts.identity.CurrentActorProvider;
import com.discushion.contracts.participation.*;
import com.discushion.contracts.share.SharedPostAccess;
import com.discushion.identity.JdbcMemberStore;
import com.discushion.participation.JdbcParticipationSnapshotReader;
import com.discushion.photos.PhotoStorage;
import java.time.Clock;
import javax.sql.DataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.*;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration(proxyBeanMethods = false)
@Profile("!local & !share21-http-test & !comment22-http-test & !bookmark26-http-test & !reaction23-http-test & !vote25-http-test & !evaluation24-http-test & !officer28-http-test")
class PostDetailConfiguration {
    @Bean JdbcParticipationSnapshotReader participationSnapshotReader(DataSource source, Clock clock) { return new JdbcParticipationSnapshotReader(source, clock); }
    @Bean JdbcPostSummaryReader postSummaryReader(DataSource source) { return new JdbcPostSummaryReader(source); }
    @Bean PostDetailService postDetailService(CurrentActorProvider actors, JdbcMemberStore members, SharedPostAccess sharing,
            ParticipationSnapshotReader participation, PostCommentPageReader comments, DataSource source,
            ObjectProvider<PhotoStorage> photos, PlatformTransactionManager manager, Clock clock) {
        var reads = new TransactionTemplate(manager);
        reads.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        return new PostDetailService(actors, members, sharing, participation, comments, new JdbcPostDetailStore(source), key -> {
            var storage = photos.getIfAvailable();
            if (storage == null) throw new IllegalStateException("Photo URL adapter unavailable");
            return storage.publicUrl(key);
        }, reads, clock);
    }
}
