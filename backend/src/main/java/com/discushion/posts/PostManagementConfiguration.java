package com.discushion.posts;

import com.discushion.contracts.participation.PostDeletionParticipant;
import com.discushion.contracts.post.PostContext;
import com.discushion.contracts.post.PostContextReader;
import com.discushion.identity.MemberAuthorization;
import com.discushion.photos.JdbcPhotoStore;
import com.discushion.photos.PhotoAttachments;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration(proxyBeanMethods = false)
@Profile("!local")
class PostManagementConfiguration {
    @Bean PostJdbcStore postJdbcStore(DataSource source) { return new PostJdbcStore(source); }

    @Bean
    @ConditionalOnMissingBean(PostContextReader.class)
    PostContextReader postContextReader(PostJdbcStore store) {
        return new PostContextReader() {
            @Override public Optional<PostContext> find(long postId) { return store.find(postId); }
            @Override public Optional<PostContext> findForUpdate(long postId) { return store.findForUpdate(postId); }
        };
    }

    @Bean PostManagementService postManagementService(MemberAuthorization members, PostJdbcStore posts,
            PostDeletionParticipant deletion, DataSource source, ObjectProvider<PostDetailLookup> details,
            PlatformTransactionManager manager) {
        var photoStore = new JdbcPhotoStore(source);
        var attachments = new PhotoAttachments(photoStore, members, new PostDatabaseClock(posts));
        return new PostManagementService(members, posts, attachments, deletion,
                details::getIfAvailable, new TransactionTemplate(manager));
    }
}
