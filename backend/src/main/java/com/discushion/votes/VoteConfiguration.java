package com.discushion.votes;

import com.discushion.contracts.post.PostContextReader;
import com.discushion.identity.MemberAuthorization;
import java.time.Clock;
import javax.sql.DataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.*;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration(proxyBeanMethods=false) @Profile("!local")
class VoteConfiguration {
    @Bean VoteService voteService(MemberAuthorization members,ObjectProvider<PostContextReader> posts,
            DataSource source,PlatformTransactionManager manager,Clock clock) {
        return new VoteService(members,()->{
            var actual=posts.getIfAvailable();
            if(actual==null)throw new IllegalStateException("Post source adapter unavailable");
            return actual;
        },new JdbcVoteStore(source),new TransactionTemplate(manager),clock);
    }
}
