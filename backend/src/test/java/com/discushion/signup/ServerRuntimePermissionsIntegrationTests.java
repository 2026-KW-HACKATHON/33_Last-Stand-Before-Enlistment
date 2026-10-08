package com.discushion.signup;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.discushion.contracts.identity.VerifiedActor;
import com.discushion.identity.JdbcMemberStore;
import com.discushion.identity.MemberAuthorization;
import com.discushion.identity.VerifiedEmail;
import com.discushion.photos.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Real password LOGIN on isolated localhost only; never a Supabase role or production data. */
@EnabledIfEnvironmentVariable(named="DISCUSHION_TEST_JDBC_URL",matches="jdbc:postgresql://127\\.0\\.0\\.1:55432/discushion_migration_test(?:\\?.*)?")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ServerRuntimePermissionsIntegrationTests {
    private static final String ROLE="discushion_server";
    private final String url=System.getenv("DISCUSHION_TEST_JDBC_URL");
    private final DriverManagerDataSource admin=new DriverManagerDataSource(url,"postgres",System.getenv("DISCUSHION_TEST_DB_PASSWORD"));
    private final JdbcTemplate fixture=new JdbcTemplate(admin);
    private HikariDataSource runtime;
    private boolean activated;
    private JdbcTemplate jdbc;
    private TransactionTemplate tx;
    private final Clock clock=Clock.fixed(Instant.parse("2026-10-08T00:00:00Z"),java.time.ZoneOffset.UTC);
    private String marker;
    private long region;

    @BeforeAll void activateOnlyTheIsolatedTestLogin() {
        assertThat(fixture.queryForObject("select inet_server_addr()='127.0.0.1'::inet and current_database()='discushion_migration_test'",Boolean.class)).isTrue();
        assertThat(fixture.queryForObject("select not rolcanlogin from pg_roles where rolname=?",Boolean.class,ROLE)).isTrue();
        // Generated synthetic value has only hexadecimal characters. Never print/store credentials.
        String password=UUID.randomUUID().toString().replace("-","");
        fixture.execute("alter role discushion_server login password '"+password+"'");
        activated=true;
        var config=new HikariConfig();
        config.setJdbcUrl(url);config.setUsername(ROLE);config.setPassword(password);
        config.setMaximumPoolSize(2);config.setConnectionTimeout(5000);
        runtime=new HikariDataSource(config);
        jdbc=new JdbcTemplate(runtime);
        tx=new TransactionTemplate(new DataSourceTransactionManager(runtime));
    }
    @AfterAll void deactivateTestLogin() {
        if(runtime!=null) runtime.close();
        if(activated) fixture.execute("alter role discushion_server nologin password null");
    }
    @BeforeEach void seedOnlyAsAdmin() {
        marker="synthetic-runtime30-"+UUID.randomUUID();
        region=fixture.queryForObject("insert into discushion.regions(name) values(?) returning id",Long.class,marker);
    }
    @AfterEach void removeOnlyThisTestsFixtures() {
        var ids=fixture.queryForList("select id from discushion.users where privy_user_id=?",Long.class,"did:privy:"+marker);
        for(long id:ids) {
            fixture.update("delete from discushion.post_photos where file_id in(select id from discushion.media_files where owner_user_id=?)",id);
            fixture.update("delete from discushion.polls where post_id in(select id from discushion.posts where author_user_id=?)",id);
            fixture.update("delete from discushion.posts where author_user_id=?",id);
            fixture.update("delete from discushion.media_files where owner_user_id=?",id);
            fixture.update("delete from discushion.neighbor_verified_regions where user_id=?",id);
            fixture.update("delete from discushion.profile_attributes where user_id=?",id);
            fixture.update("delete from discushion.user_agreements where user_id=?",id);
            fixture.update("delete from discushion.profiles where user_id=?",id);
            fixture.update("delete from discushion.users where id=?",id);
        }
        fixture.update("delete from discushion.regions where id=? and name=?",region,marker);
    }
    private SignupService service() {
        String subject="did:privy:"+marker;
        return new SignupService(()->Optional.of(new VerifiedActor(subject,Optional.empty())),
            ()->Optional.of(new VerifiedEmail(subject,marker+"@example.invalid",clock.instant())),
            new JdbcSignupStore(runtime),tx,clock);
    }
    private SignupInput input() {
        return new SignupInput(true,true,false,"r"+UUID.randomUUID().toString().substring(0,8),null,List.of("RESIDENT"),region);
    }
    @Test void actualLoginCannotOwnObjectsOrInheritElevatedRoles() {
        assertThat(jdbc.queryForObject("select current_user",String.class)).isEqualTo(ROLE);
        assertThat(jdbc.queryForObject("""
            select rolcanlogin and not (rolsuper or rolcreatedb or rolcreaterole or rolinherit or rolreplication or rolbypassrls)
            and not exists(select 1 from pg_auth_members where member=r.oid)
            and not exists(select 1 from pg_namespace where nspowner=r.oid)
            and not exists(select 1 from pg_class where relowner=r.oid)
            from pg_roles r where rolname=current_user
            """,Boolean.class)).isTrue();
        assertThat(jdbc.queryForObject("select has_schema_privilege(current_user,'discushion','USAGE') and not has_schema_privilege(current_user,'discushion','CREATE') and not has_schema_privilege(current_user,'public','CREATE')",Boolean.class)).isTrue();
    }
    @Test void all27TablesHaveExactlyThePhaseOnePrivilegesAndNoSequenceOrGrantOptions() {
        Map<String,Set<String>> allowed=new HashMap<>();
        for(String table:List.of("regions","institutions","neighbor_verified_regions","institution_credentials")) allowed.put(table,Set.of("SELECT"));
        for(String table:List.of("users","profiles","user_agreements","media_files")) allowed.put(table,Set.of("SELECT","INSERT","UPDATE"));
        for(String table:List.of("posts","polls")) allowed.put(table,Set.of("SELECT","UPDATE"));
        allowed.put("profile_attributes",Set.of("SELECT","INSERT","DELETE"));
        allowed.put("post_photos",Set.of("SELECT","INSERT","UPDATE","DELETE"));
        allowed.put("comments",Set.of("SELECT","INSERT"));
        allowed.put("comment_evaluations",Set.of("SELECT"));
        var tables=jdbc.queryForList("select tablename from pg_tables where schemaname='discushion' order by tablename",String.class);
        assertThat(tables).hasSize(27);
        for(String table:tables) for(String operation:List.of("SELECT","INSERT","UPDATE","DELETE","TRUNCATE","REFERENCES","TRIGGER")) {
            boolean permitted=allowed.getOrDefault(table,Set.of()).contains(operation);
            assertThat(jdbc.queryForObject("select has_table_privilege(current_user,?,?)",Boolean.class,"discushion."+table,operation))
                .as(table+" "+operation).isEqualTo(permitted);
            assertThat(jdbc.queryForObject("select has_table_privilege(current_user,?,?)",Boolean.class,"discushion."+table,operation+" WITH GRANT OPTION")).isFalse();
        }
        assertThat(jdbc.queryForObject("select not exists(select 1 from pg_class where relnamespace='discushion'::regnamespace and relkind='S' and has_sequence_privilege(current_user,oid,'USAGE,SELECT,UPDATE'))",Boolean.class)).isTrue();
    }
    @Test void actualSignupAndRetryWorkUnderRlsWithoutGivingQualificationWriteAccess() {
        var service=service();var first=service.complete(input());
        assertThat(first.completedNow()).isTrue();
        assertThat(service.complete(input()).result()).isEqualTo(first.result());
        long id=first.result().member().id();
        var qualification=tx.execute(status->new JdbcMemberStore(runtime,clock).lockAndRead(id).orElseThrow());
        assertThat(qualification.registrationCompletedAt()).isPresent();
        assertThat(qualification.verifiedRegionIds()).isEmpty();
        assertThat(qualification.institutionGrants()).isEmpty();
        assertThat(jdbc.queryForObject("select count(*) from discushion.user_agreements where user_id=?",Integer.class,id)).isEqualTo(3);
    }
    @Test void photoReservationLinkOrderingAndDurableDeletionWorkWithRuntimeAccount() {
        long id=service().complete(input()).result().member().id();
        fixture.update("insert into discushion.neighbor_verified_regions(user_id,region_id,verified_at) values(?,?,now())",id,region);
        long post=fixture.queryForObject("""
            insert into discushion.posts(author_user_id,region_id,type,topic,title,content,status,content_revision,created_at,updated_at)
            values(?,?,'VOTE','OTHER',?,'synthetic','PUBLISHED',1,now(),now()) returning id
            """,Long.class,id,region,marker);
        fixture.update("insert into discushion.polls(post_id,question,ends_at) values(?,'synthetic',now()+interval '1 day')",post);
        var member=new JdbcMemberStore(runtime,clock);
        var actor=new VerifiedActor("did:privy:"+marker,Optional.of(member.findByVerifiedSubject("did:privy:"+marker).orElseThrow()));
        var authorization=new MemberAuthorization(()->Optional.of(actor),member,clock);
        var storage=mock(PhotoStorage.class);
        Instant expires=clock.instant().plusSeconds(7200);
        when(storage.authorizationUpperBound(any())).thenReturn(expires);
        when(storage.createUpload(anyString(),anyString())).thenReturn(new PhotoStorage.Upload("https://example.invalid/synthetic","PUT","RAW",Map.of(),expires));
        var store=new JdbcPhotoStore(runtime);
        var photos=new PhotoService(store,authorization,storage,tx,clock);
        long file=photos.reserve("synthetic.png","image/png",67).fileId();
        // Provider verification is outside this DB test; stage a server-verified file fixture.
        jdbc.update("update discushion.media_files set lifecycle_status='UNLINKED',uploaded_at=created_at where id=?",file);
        var attachments=new PhotoAttachments(store,authorization,clock);
        tx.executeWithoutResult(status->attachments.replace(post,List.of(new PhotoAttachments.Reference(null,file))));
        long photo=jdbc.queryForObject("select id from discushion.post_photos where file_id=?",Long.class,file);
        tx.executeWithoutResult(status->attachments.replace(post,List.of(new PhotoAttachments.Reference(photo,null))));
        List<Long> removed=tx.execute(status->attachments.replace(post,List.of()));
        assertThat(removed).containsExactly(file);
        assertThat(photos.get(file).status()).isEqualTo("DELETE_PENDING");
        assertThat(jdbc.queryForObject("select count(*) from discushion.post_photos where file_id=?",Integer.class,file)).isZero();
        verify(storage,never()).remove(anyString());
    }
    @Test void forbiddenDdlTruncateDeletesSeedLegacyAndAdminEscalationReallyFail() throws Exception {
        for(String sql:List.of(
            "create table discushion.forbidden_runtime30(id bigint)",
            "create table public.forbidden_runtime30(id bigint)",
            "alter table discushion.users add column forbidden_runtime30 text",
            "truncate discushion.users cascade",
            "delete from discushion.users where false",
            "delete from discushion.media_files where false",
            "insert into discushion.regions(name) values('forbidden-runtime30')",
            "insert into discushion.institutions(name,created_at) values('forbidden-runtime30',now())",
            "update discushion.institutions set name='forbidden-runtime30' where false",
            "delete from discushion.institutions where false",
            "update discushion.neighbor_verified_regions set verified_at=now() where false",
            "update discushion.institution_credentials set valid_until=now() where false",
            "select * from discushion.email_verifications",
            "select * from discushion.neighbor_verification_evidences",
            "insert into discushion.posts default values",
            "select * from discushion.activity_events",
            "alter role discushion_server bypassrls",
            "set role postgres",
            "select nextval('discushion.users_id_seq')",
            "select setval('discushion.users_id_seq',1)")) {
            try(Connection connection=runtime.getConnection()) {
                connection.setAutoCommit(false);
                try {
                    assertThatThrownBy(()->connection.createStatement().execute(sql)).as(sql)
                        .isInstanceOfSatisfying(SQLException.class,error->assertThat(error.getSQLState()).isEqualTo("42501"));
                } finally {connection.rollback();}
            }
        }
    }
    @Test void disablingRowSecurityCannotBypassThePolicies() throws Exception {
        try(Connection connection=runtime.getConnection()) {
            connection.setAutoCommit(false);
            try {
                connection.createStatement().execute("set local row_security=off");
                assertThatThrownBy(()->connection.createStatement().executeQuery("select * from discushion.users"))
                    .isInstanceOfSatisfying(SQLException.class,error->assertThat(error.getSQLState()).isEqualTo("42501"));
            } finally {connection.rollback();}
        }
    }
    @Test void runtimeCannotDelegateItsPrivilegesToPublic() throws Exception {
        try(Connection connection=runtime.getConnection()) {
            connection.setAutoCommit(false);
            try {
                // PostgreSQL may warn rather than throw for a GRANT without grant option.
                connection.createStatement().execute("grant select on discushion.users to public");
                try(var result=connection.createStatement().executeQuery("select has_table_privilege('public','discushion.users','SELECT')")) {
                    assertThat(result.next()).isTrue();assertThat(result.getBoolean(1)).isFalse();
                }
            } finally {connection.rollback();}
        }
    }
    @Test void serverPoliciesAreExplicitPerOperationAndNeverPublicOrAll() {
        assertThat(jdbc.queryForObject("select count(*) from pg_policies where schemaname='discushion' and policyname like 'server_%'",Integer.class)).isEqualTo(30);
        assertThat(jdbc.queryForObject("""
            select bool_and(roles=array['discushion_server']::name[] and cmd<>'ALL' and permissive='PERMISSIVE'
              and (qual is not distinct from case when cmd<>'INSERT' then 'true' end)
              and (with_check is not distinct from case when cmd in ('INSERT','UPDATE') then 'true' end))
            from pg_policies where schemaname='discushion' and policyname like 'server_%'
            """,Boolean.class)).isTrue();
    }
}
