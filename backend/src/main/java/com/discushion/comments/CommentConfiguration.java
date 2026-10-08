package com.discushion.comments;

import com.discushion.contracts.identity.CurrentActorProvider;
import com.discushion.contracts.post.PostContextReader;
import com.discushion.contracts.share.SharedPostAccess;
import com.discushion.identity.*;
import java.time.Clock;
import javax.sql.DataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.*;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration(proxyBeanMethods = false) @Profile("!local")
class CommentConfiguration {
    @Bean CommentService commentService(CurrentActorProvider actors, JdbcMemberStore members, MemberAuthorization guard,
        SharedPostAccess sharing, ObjectProvider<PostContextReader> posts, DataSource source, PlatformTransactionManager manager, Clock clock) {
        var reads = new TransactionTemplate(manager); reads.setReadOnly(true); reads.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        return new CommentService(actors, members, guard, sharing, () -> {
            var actual = posts.getIfAvailable();
            if (actual == null) throw new IllegalStateException("Post source adapter unavailable");
            return actual;
        }, new JdbcCommentStore(source), reads, new TransactionTemplate(manager), clock);
    }
}
