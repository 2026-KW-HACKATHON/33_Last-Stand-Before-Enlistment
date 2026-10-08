package com.discushion.bookmark;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import static org.assertj.core.api.Assertions.*;

/** Actual local discushion_server login; this never targets a Supabase project. */
@EnabledIfEnvironmentVariable(named="DISCUSHION_TEST_JDBC_URL",matches="jdbc:postgresql://127\\.0\\.0\\.1:55432/discushion_migration_test(?:\\?.*)?")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BookmarkRuntimePermissionsIntegrationTests {
    private static final String ROLE="discushion_server";
    private final String url=System.getenv("DISCUSHION_TEST_JDBC_URL");
    private final DriverManagerDataSource adminSource=new DriverManagerDataSource(url,"postgres",System.getenv("DISCUSHION_TEST_DB_PASSWORD"));
    private final JdbcTemplate admin=new JdbcTemplate(adminSource);
    private HikariDataSource runtime;
    private JdbcTemplate jdbc;
    private TransactionTemplate tx;
    private boolean activated;
    private String marker;
    private long user,region,post;

    @BeforeAll void activateOnlyTheIsolatedTestLogin() {
        assertThat(admin.queryForObject("select inet_server_addr()='127.0.0.1'::inet and current_database()='discushion_migration_test'",Boolean.class)).isTrue();
        assertThat(admin.queryForObject("select not rolcanlogin from pg_roles where rolname=?",Boolean.class,ROLE)).isTrue();
        String password=UUID.randomUUID().toString().replace("-","");
        admin.execute("alter role discushion_server login password '"+password+"'");activated=true;
        var config=new HikariConfig();config.setJdbcUrl(url);config.setUsername(ROLE);config.setPassword(password);config.setMaximumPoolSize(2);
        runtime=new HikariDataSource(config);jdbc=new JdbcTemplate(runtime);tx=new TransactionTemplate(new DataSourceTransactionManager(runtime));
    }
    @AfterAll void deactivateTestLogin() {
        if(runtime!=null)runtime.close();
        if(activated)admin.execute("alter role discushion_server nologin password null");
    }
    @BeforeEach void fixture() {
        marker="bookmark26-"+UUID.randomUUID();
        region=admin.queryForObject("insert into discushion.regions(name) values(?) returning id",Long.class,marker);
        user=admin.queryForObject("""
            insert into discushion.users(email,password_hash,email_verified_at,created_at,updated_at,privy_user_id,registration_completed_at)
            values(?,'synthetic',now(),now(),now(),?,now()) returning id
            """,Long.class,marker+"@example.invalid","did:privy:"+marker);
        post=admin.queryForObject("""
            insert into discushion.posts(author_user_id,region_id,type,topic,title,content,status,content_revision,created_at,updated_at)
            values(?,?,'LOCAL_AGENDA','OTHER',?,'synthetic','PUBLISHED',1,now(),now()) returning id
            """,Long.class,user,region,marker);
    }
    @AfterEach void cleanup() {
        admin.update("delete from discushion.bookmarks where post_id=?",post);
        admin.update("delete from discushion.posts where id=?",post);
        admin.update("delete from discushion.users where id=?",user);
        admin.update("delete from discushion.regions where id=?",region);
    }

    @Test void runtimeCanReadInsertDeleteButCannotUpdateOrAccessActivityEvents() {
        var store=new JdbcBookmarkStore(runtime);var now=Instant.parse("2026-10-08T00:00:00Z");
        store.add(post,user,now);store.add(post,user,now.plusSeconds(1));
        assertThat(jdbc.queryForObject("select created_at from discushion.bookmarks where post_id=? and user_id=?",java.sql.Timestamp.class,post,user).toInstant()).isEqualTo(now);
        assertThatThrownBy(()->jdbc.update("update discushion.bookmarks set created_at=now() where post_id=?",post))
            .isInstanceOf(org.springframework.dao.DataAccessException.class);
        assertThatThrownBy(()->jdbc.queryForObject("select count(*) from discushion.activity_events",Integer.class))
            .isInstanceOf(org.springframework.dao.DataAccessException.class);
        store.remove(post,user);assertThat(jdbc.queryForObject("select count(*) from discushion.bookmarks where post_id=?",Integer.class,post)).isZero();
    }

    @Test void deletionParticipantRequiresAndJoinsTheCallerTransaction() {
        var store=new JdbcBookmarkStore(runtime);store.add(post,user,Instant.now());
        var participant=new JdbcPostDeletionParticipant(runtime);
        assertThatThrownBy(()->participant.removeBookmarks(post)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(()->tx.executeWithoutResult(status->{participant.removeBookmarks(post);throw new IllegalStateException("rollback fixture");}))
            .isInstanceOf(IllegalStateException.class);
        assertThat(jdbc.queryForObject("select count(*) from discushion.bookmarks where post_id=?",Integer.class,post)).isEqualTo(1);
        tx.executeWithoutResult(status->participant.removeBookmarks(post));
        assertThat(jdbc.queryForObject("select count(*) from discushion.bookmarks where post_id=?",Integer.class,post)).isZero();
    }
}
