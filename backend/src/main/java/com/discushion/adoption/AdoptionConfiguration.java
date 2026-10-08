package com.discushion.adoption;

import com.discushion.contracts.post.PostContextReader;
import com.discushion.identity.MemberAuthorization;
import java.time.Clock;
import javax.sql.DataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration(proxyBeanMethods = false)
@Profile("!local")
class AdoptionConfiguration {
    @Bean AdoptionService adoptionService(MemberAuthorization members, ObjectProvider<PostContextReader> posts,
            DataSource source, PlatformTransactionManager manager, Clock clock) {
        return new AdoptionService(members, () -> {
            var actual = posts.getIfAvailable();
            if (actual == null) throw new IllegalStateException("Post source adapter unavailable");
            return actual;
        }, new JdbcAdoptionStore(source), new TransactionTemplate(manager), clock);
    }
}
