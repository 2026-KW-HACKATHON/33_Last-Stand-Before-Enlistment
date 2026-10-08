package com.discushion.posts;

import com.discushion.identity.MemberAuthorization;
import com.discushion.photos.PhotoAttachments;
import javax.sql.DataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration(proxyBeanMethods = false)
@Profile("!local & !share21-http-test & !comment22-http-test & !bookmark26-http-test & !reaction23-http-test & !vote25-http-test & !evaluation24-http-test")
class PostCreationConfiguration {
    @Bean PostJdbcRepository postJdbcRepository(DataSource source) { return new PostJdbcRepository(source); }

    @Bean PostCreationService postCreationService(MemberAuthorization members, PostJdbcRepository posts,
            ObjectProvider<PhotoAttachments> photos, PlatformTransactionManager manager) {
        return new PostCreationService(members, posts, photos::getIfAvailable, new TransactionTemplate(manager));
    }
}
