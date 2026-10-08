package com.discushion.signup;

import com.discushion.contracts.identity.CurrentActorProvider;
import com.discushion.identity.AuthenticatedEmailService;
import java.time.Clock;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration(proxyBeanMethods=false)
@Profile("!local")
class SignupConfiguration {
    @Bean SignupService signupService(DataSource source,PlatformTransactionManager manager,CurrentActorProvider actors,
                                     AuthenticatedEmailService emails,Clock clock) {
        return new SignupService(actors,emails::currentVerifiedEmail,
            new JdbcSignupStore(source),new TransactionTemplate(manager),clock);
    }
}
