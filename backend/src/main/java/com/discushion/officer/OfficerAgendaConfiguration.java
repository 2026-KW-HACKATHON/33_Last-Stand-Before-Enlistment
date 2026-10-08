package com.discushion.officer;

import com.discushion.contracts.identity.CurrentActorProvider;
import com.discushion.contracts.identity.MemberQualificationReader;
import com.discushion.contracts.participation.ParticipationSnapshotReader;
import com.discushion.contracts.post.PostSummaryReader;
import com.discushion.identity.MemberAuthorization;
import java.time.Clock;
import javax.sql.DataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration(proxyBeanMethods = false)
@Profile("!local")
class OfficerAgendaConfiguration {
    @Bean OfficerAgendaService officerAgendaService(CurrentActorProvider actors,
            MemberQualificationReader qualifications, MemberAuthorization authorization,
            ObjectProvider<PostSummaryReader> summaries, ObjectProvider<ParticipationSnapshotReader> participation,
            DataSource source, PlatformTransactionManager manager, Clock clock) {
        var reads = new TransactionTemplate(manager);
        reads.setReadOnly(true);
        reads.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        return new OfficerAgendaService(actors, qualifications, authorization,
                () -> summaries.getIfAvailable(), () -> participation.getIfAvailable(),
                new JdbcOfficerAgendaStore(source), reads, clock);
    }
}
