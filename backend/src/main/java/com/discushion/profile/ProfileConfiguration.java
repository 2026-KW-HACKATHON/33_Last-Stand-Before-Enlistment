package com.discushion.profile;

import com.discushion.contracts.identity.CurrentActorProvider;
import com.discushion.identity.MemberAuthorization;
import java.time.Clock;
import javax.sql.DataSource;
import org.springframework.context.annotation.*;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration(proxyBeanMethods=false) @Profile("!local")
class ProfileConfiguration {
    @Bean ProfileService profileService(DataSource source,PlatformTransactionManager manager,CurrentActorProvider actors,
                                        MemberAuthorization authorization,Clock clock) {
        var read=new TransactionTemplate(manager);read.setReadOnly(true);
        read.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        return new ProfileService(actors,authorization,new JdbcProfileStore(source),read,new TransactionTemplate(manager),clock);
    }
}
