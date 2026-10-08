package com.discushion.home;

import com.discushion.contracts.identity.CurrentActorProvider;
import javax.sql.DataSource;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration(proxyBeanMethods=false) @Profile("!local")
class HomeConfiguration {
    @Bean HomeService homeService(CurrentActorProvider actors,DataSource source,PlatformTransactionManager manager,Environment env){
        var reads=new TransactionTemplate(manager);reads.setReadOnly(true);reads.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        return new HomeService(actors,new JdbcHomeStore(source,env.getProperty("SUPABASE_URL"),env.getProperty("SUPABASE_STORAGE_BUCKET")),reads);
    }
}
