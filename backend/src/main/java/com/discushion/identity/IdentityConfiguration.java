package com.discushion.identity;

import java.time.Clock;
import java.time.Duration;
import javax.sql.DataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

@Configuration(proxyBeanMethods = false)
public class IdentityConfiguration {
    @Bean
    @ConditionalOnMissingBean(Clock.class)
    Clock identityClock() { return Clock.systemUTC(); }

    @Bean
    RequestActorContext currentActorProvider() { return new RequestActorContext(); }

    @Bean
    @Profile("!local")
    JdbcMemberStore jdbcMemberStore(DataSource dataSource, Clock clock) { return new JdbcMemberStore(dataSource, clock); }

    @Bean
    @ConditionalOnMissingBean(AccessTokenVerifier.class)
    AccessTokenVerifier accessTokenVerifier(Environment environment, ObjectProvider<VerificationKeySource> keys, Clock clock) {
        return new PrivyAccessTokenVerifier(environment.getProperty("PRIVY_APP_ID"), keyId -> {
            var source = keys.getIfAvailable();
            if (source == null) throw new IdentityFailure(IdentityFailure.Reason.PROVIDER_UNAVAILABLE);
            return source.find(keyId);
        }, clock, Duration.ZERO);
    }

    @Bean
    BearerIdentityFilter bearerIdentityFilter(AccessTokenVerifier tokens, ObjectProvider<JdbcMemberStore> members) {
        return new BearerIdentityFilter(tokens, subject -> {
            var store = members.getIfAvailable();
            if (store == null) throw new IllegalStateException("Member DataSource is not configured");
            return store.findByVerifiedSubject(subject);
        });
    }

    @Bean
    @Profile("!local")
    MemberAuthorization memberAuthorization(RequestActorContext actors, JdbcMemberStore members, Clock clock) {
        return new MemberAuthorization(actors, members, clock);
    }
}
