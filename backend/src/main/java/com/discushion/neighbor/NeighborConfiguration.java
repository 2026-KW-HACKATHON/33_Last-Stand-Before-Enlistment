package com.discushion.neighbor;

import com.discushion.contracts.identity.CurrentActorProvider;
import javax.sql.DataSource;
import org.springframework.context.annotation.*;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration(proxyBeanMethods=false) @Profile("!local")
class NeighborConfiguration {
    @Bean NeighborService neighborService(CurrentActorProvider actors,DataSource source,PlatformTransactionManager manager) {
        var reads=new TransactionTemplate(manager);reads.setReadOnly(true);
        reads.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        return new NeighborService(actors,new JdbcNeighborStore(source),reads);
    }
}
