package com.discushion.personal;

import com.discushion.contracts.identity.CurrentActorProvider;
import com.discushion.contracts.participation.ParticipationSnapshotReader;
import com.discushion.contracts.post.PostSummaryReader;
import com.discushion.identity.JdbcMemberStore;
import com.discushion.photos.PhotoStorage;
import java.time.Clock;
import javax.sql.DataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.*;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration(proxyBeanMethods = false) @Profile("!local")
class PersonalConfiguration {
    @Bean PersonalService personalService(CurrentActorProvider actors, JdbcMemberStore members,
            ObjectProvider<PostSummaryReader> summaries, ObjectProvider<ParticipationSnapshotReader> participation,
            ObjectProvider<PhotoStorage> photos, DataSource source, PlatformTransactionManager manager, Clock clock) {
        var reads = new TransactionTemplate(manager);
        reads.setReadOnly(true); reads.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        return new PersonalService(actors, members, summaries::getObject, participation::getObject,
                new JdbcPersonalStore(source), key -> photos.getObject().publicUrl(key), reads, clock);
    }
}
