package com.discushion.login;

import com.discushion.contracts.identity.CurrentActorProvider;
import com.discushion.identity.JdbcMemberStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods=false)
@Profile("!local")
class LoginConfiguration {
    @Bean LoginService loginService(CurrentActorProvider actors,JdbcMemberStore members) {
        return new LoginService(actors,members::findByVerifiedSubject);
    }
}
