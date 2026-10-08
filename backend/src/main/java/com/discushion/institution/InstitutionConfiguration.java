package com.discushion.institution;

import com.discushion.contracts.identity.CurrentActorProvider;
import java.time.Clock;
import javax.sql.DataSource;
import org.springframework.context.annotation.*;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration(proxyBeanMethods = false)
@Profile("!local")
class InstitutionConfiguration {
    @Bean InstitutionLookup institutionLookup(CurrentActorProvider actors, DataSource source,
            PlatformTransactionManager manager, Clock clock) {
        return new InstitutionLookup(actors, source, manager, clock);
    }
}
