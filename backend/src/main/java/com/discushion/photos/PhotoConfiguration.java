package com.discushion.photos;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import javax.sql.DataSource;
import com.discushion.identity.MemberAuthorization;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration(proxyBeanMethods=false)
@ConditionalOnProperty(name="PHOTO_UPLOADS_ENABLED",havingValue="true")
public class PhotoConfiguration {
    @Bean JdbcPhotoStore jdbcPhotoStore(DataSource source) {return new JdbcPhotoStore(source);}
    @Bean @ConditionalOnMissingBean(PhotoStorage.class)
    @ConditionalOnProperty(name="PHOTO_STORAGE_WIRE_VERIFIED",havingValue="true")
    PhotoStorage photoStorage(Environment env,Clock clock) {
        if(!env.getProperty("PHOTO_STORAGE_WIRE_VERIFIED",Boolean.class,false))
            throw new IllegalStateException("Verify relay Storage write/read/delete and deployment limits before enabling photos");
        return new SupabasePhotoStorage(URI.create(env.getRequiredProperty("SUPABASE_URL")),
            env.getRequiredProperty("SUPABASE_SECRET_KEY"),env.getRequiredProperty("SUPABASE_STORAGE_BUCKET"),
            clock);
    }
    @Bean @ConditionalOnMissingBean(PhotoService.class)
    PhotoService photoService(JdbcPhotoStore store,MemberAuthorization authorization,PhotoStorage storage,
            PlatformTransactionManager manager,Clock clock) {
        return new PhotoService(store,authorization,storage,new TransactionTemplate(manager),new PhotoDatabaseClock(store),true);
    }
    @Bean @ConditionalOnMissingBean(PhotoCleanup.class)
    PhotoCleanup photoCleanup(JdbcPhotoStore store,PhotoStorage storage,PlatformTransactionManager manager,Clock clock) {
        return new PhotoCleanup(store,storage,new TransactionTemplate(manager),new PhotoDatabaseClock(store),Duration.ofMinutes(2));
    }
    @Bean PhotoAttachments photoAttachments(JdbcPhotoStore store,MemberAuthorization authorization,Clock clock) {
        return new PhotoAttachments(store,authorization,new PhotoDatabaseClock(store));
    }
    @Bean org.springframework.boot.web.server.WebServerFactoryCustomizer<org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory>
    photoUploadTimeouts() {
        return factory->factory.addConnectorCustomizers(connector->{
            if(!connector.setProperty("disableUploadTimeout","false")
                || !connector.setProperty("connectionUploadTimeout","30000")
                || !connector.setProperty("maxSwallowSize","0"))
                throw new IllegalStateException("Photo upload receiving limits were not applied");
        });
    }
    @Configuration(proxyBeanMethods=false)
    @EnableScheduling
    @ConditionalOnProperty(name="PHOTO_CLEANUP_ENABLED",havingValue="true")
    static class Scheduling {
        @Bean CleanupTask cleanupTask(PhotoCleanup cleanup) {return new CleanupTask(cleanup);}
    }
    static class CleanupTask {
        private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger(CleanupTask.class);
        private final PhotoCleanup cleanup;
        CleanupTask(PhotoCleanup cleanup) {this.cleanup=cleanup;}
        @Scheduled(fixedDelayString="${PHOTO_CLEANUP_INTERVAL_MS:60000}")
        public void run() {
            try {LOG.info("Photo cleanup processed {} reservations",cleanup.runBatch(20));}
            catch(RuntimeException failure) {LOG.error("Photo cleanup failed: INTERNAL_ERROR");}
        }
    }
}
