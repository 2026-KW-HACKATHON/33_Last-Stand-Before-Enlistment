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
            throw new IllegalStateException("Verify real Storage PUT/RAW/bucket/capability bounds before enabling photos");
        return new SupabasePhotoStorage(URI.create(env.getRequiredProperty("SUPABASE_URL")),
            env.getRequiredProperty("SUPABASE_SECRET_KEY"),env.getRequiredProperty("SUPABASE_STORAGE_BUCKET"),
            Duration.ofSeconds(Long.parseLong(env.getRequiredProperty("PHOTO_VERIFIED_ISSUANCE_ALLOWANCE_SECONDS"))),clock);
    }
    @Bean PhotoService photoService(JdbcPhotoStore store,MemberAuthorization authorization,PhotoStorage storage,
            PlatformTransactionManager manager,Clock clock) {
        return new PhotoService(store,authorization,storage,new TransactionTemplate(manager),clock);
    }
    @Bean PhotoCleanup photoCleanup(JdbcPhotoStore store,PhotoStorage storage,PlatformTransactionManager manager,Clock clock) {
        return new PhotoCleanup(store,storage,new TransactionTemplate(manager),clock,Duration.ofMinutes(2));
    }
    @Bean PhotoAttachments photoAttachments(JdbcPhotoStore store,MemberAuthorization authorization,Clock clock) {
        return new PhotoAttachments(store,authorization,clock);
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
