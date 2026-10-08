package com.discushion.share;

import com.discushion.contracts.identity.CurrentActorProvider;
import com.discushion.contracts.post.*;
import com.discushion.contracts.share.SharedPostAccess;
import com.discushion.identity.JdbcMemberStore;
import jakarta.servlet.http.HttpServletRequest;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration(proxyBeanMethods = false) @Profile("!local")
class ShareConfiguration {
    @Bean PostShareTokens postShareTokens(Clock clock, Environment env) {
        return new PostShareTokens(clock, env.getProperty("SHARE_TOKEN_SIGNING_KEY"), new SecureRandom());
    }
    @Bean ShareLinkService shareLinkService(CurrentActorProvider actors, JdbcMemberStore members, ObjectProvider<PostContextReader> posts,
            PostShareTokens tokens, PlatformTransactionManager manager, Environment env) {
        var reads = new TransactionTemplate(manager); reads.setReadOnly(true); reads.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        return new ShareLinkService(actors, members, deferred(posts), tokens, reads, env.getProperty("PUBLIC_WEB_BASE_URL"));
    }
    @Bean SharedPostAccess sharedPostAccess(ObjectProvider<HttpServletRequest> requests, CurrentActorProvider actors, ObjectProvider<PostContextReader> posts,
            PostShareTokens tokens, DataSource source) {
        return new HttpSharedPostAccess(requests::getObject, actors, deferred(posts), tokens, source);
    }
    private static PostContextReader deferred(ObjectProvider<PostContextReader> provider) {
        return new PostContextReader() {
            private PostContextReader actual() {
                var value = provider.getIfAvailable();
                if (value == null) throw new IllegalStateException("Post source adapter unavailable");
                return value;
            }
            @Override public Optional<PostContext> find(long id) { return actual().find(id); }
            @Override public Optional<PostContext> findForUpdate(long id) { return actual().findForUpdate(id); }
        };
    }
}
