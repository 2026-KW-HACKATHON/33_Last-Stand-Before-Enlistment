package com.discushion.bookmark;

import com.discushion.contracts.participation.PostDeletionParticipant;
import com.discushion.contracts.post.PostContextReader;
import com.discushion.contracts.post.PostSummaryReader;
import com.discushion.identity.MemberAuthorization;
import java.time.Clock;
import javax.sql.DataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.*;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration(proxyBeanMethods=false) @Profile("!local")
class BookmarkConfiguration {
    @Bean BookmarkService bookmarkService(MemberAuthorization members,ObjectProvider<PostContextReader> posts,
            ObjectProvider<PostSummaryReader> summaries,DataSource source,PlatformTransactionManager manager,Clock clock) {
        return new BookmarkService(members,()->{
            var actual=posts.getIfAvailable();if(actual==null)throw new IllegalStateException("Post source adapter unavailable");return actual;
        },()->{
            var actual=summaries.getIfAvailable();if(actual==null)throw new IllegalStateException("Post summary source adapter unavailable");return actual;
        },new JdbcBookmarkStore(source),new TransactionTemplate(manager),clock);
    }
    @Bean PostDeletionParticipant postDeletionParticipant(DataSource source){return new JdbcPostDeletionParticipant(source);}
}
