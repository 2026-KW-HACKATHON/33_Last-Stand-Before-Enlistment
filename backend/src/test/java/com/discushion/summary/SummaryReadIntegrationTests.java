package com.discushion.summary;
import com.discushion.contracts.identity.*;
import com.discushion.identity.IdentityFailure;
import com.zaxxer.hikari.*;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.LinkedMultiValueMap;
import static org.assertj.core.api.Assertions.*;

import com.discushion.contracts.share.SharedPostAccess;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
@EnabledIfEnvironmentVariable(named="DISCUSHION_TEST_JDBC_URL",matches="jdbc:postgresql://127\\.0\\.0\\.1:55432/discushion_migration_test(?:\\?.*)?")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SummaryReadIntegrationTests {
    static final String URL=System.getenv("DISCUSHION_TEST_JDBC_URL");
    final JdbcTemplate admin=new JdbcTemplate(new DriverManagerDataSource(URL,"postgres",System.getenv("DISCUSHION_TEST_DB_PASSWORD")));
    HikariDataSource runtime;JdbcTemplate jdbc;TransactionTemplate tx;
    boolean activated;long user,region,otherRegion;String marker;
    @BeforeAll void runtimeLogin(){
        assertThat(admin.queryForObject("select inet_server_addr()='127.0.0.1'::inet and current_database()='discushion_migration_test'",Boolean.class)).isTrue();
        assertThat(admin.queryForObject("select not rolcanlogin from pg_roles where rolname='discushion_server'",Boolean.class)).isTrue();
        String password=UUID.randomUUID().toString().replace("-","");
        admin.execute("alter role discushion_server login password '"+password+"'");activated=true;
        var config=new HikariConfig();config.setJdbcUrl(URL);config.setUsername("discushion_server");config.setPassword(password);config.setMaximumPoolSize(4);
        runtime=new HikariDataSource(config);jdbc=new JdbcTemplate(runtime);tx=new TransactionTemplate(new DataSourceTransactionManager(runtime));
    }
    @AfterAll void close(){try{if(runtime!=null)runtime.close();}finally{if(activated)admin.execute("alter role discushion_server nologin password null");}}
    @BeforeEach void fixture(){
        marker="read-"+UUID.randomUUID();region=admin.queryForObject("insert into discushion.regions(name) values(?) returning id",Long.class,marker);
        otherRegion=admin.queryForObject("insert into discushion.regions(name) values(?) returning id",Long.class,marker+"-other");
        user=admin.queryForObject("insert into discushion.users(email,password_hash,email_verified_at,created_at,updated_at,privy_user_id,registration_completed_at) values(?,'synthetic',now(),now(),now(),?,now()) returning id",Long.class,marker+"@example.invalid","did:privy:"+marker);
        admin.update("insert into discushion.profiles(user_id,nickname,activity_region_id,updated_at) values(?,?,?,now())",user,UUID.randomUUID().toString().substring(0,8),region);
        sourceFixture();
    }
    @AfterEach void cleanup(){
        for(String table:List.of("ai_agenda_summaries","post_reactions","comments","activity_post_details","post_photos"))
            admin.update("delete from discushion."+table+" where post_id in(select id from discushion.posts where author_user_id=?)",user);
        admin.update("delete from discushion.vote_selections where poll_id in(select id from discushion.polls where post_id in(select id from discushion.posts where author_user_id=?))",user);
        admin.update("delete from discushion.poll_options where poll_id in(select id from discushion.polls where post_id in(select id from discushion.posts where author_user_id=?))",user);
        admin.update("delete from discushion.polls where post_id in(select id from discushion.posts where author_user_id=?)",user);
        admin.update("delete from discushion.posts where author_user_id=?",user);
        admin.update("delete from discushion.profiles where user_id=?",user);admin.update("delete from discushion.users where id=?",user);
        admin.update("delete from discushion.regions where id in(?,?)",region,otherRegion);
    }
    long post(String type,long target,Instant created){
        return admin.queryForObject("insert into discushion.posts(author_user_id,region_id,type,topic,title,content,status,content_revision,created_at,updated_at) values(?,?,?,'OTHER',?,'첫 원문입니다. 두 번째 정보입니다. 마지막 정보입니다.','PUBLISHED',1,?,?) returning id",Long.class,user,target,type,marker,Timestamp.from(created),Timestamp.from(created));
    }
    long poll(long post,Instant ends){
        long poll=admin.queryForObject("insert into discushion.polls(post_id,question,ends_at) values(?,'질문',?) returning id",Long.class,post,Timestamp.from(ends));
        for(int i=0;i<2;i++)admin.update("insert into discushion.poll_options(poll_id,content,sort_order) values(?,?,?)",poll,"선택 "+i,i);
        return poll;
    }
    CurrentActorProvider actor(){return ()->Optional.of(new VerifiedActor("did:privy:"+marker,Optional.of(new LocalMember(user,Optional.of(Instant.now())))));}

    final AtomicInteger calls=new AtomicInteger();
    long agenda;
    void sourceFixture(){calls.set(0);agenda=post("LOCAL_AGENDA",region,Instant.parse("2026-10-08T00:00:00Z"));}
    SummaryGenerator generator(java.util.function.Supplier<SummaryGenerator.Result> action){return new SummaryGenerator(){
        public boolean available(){return true;}
        public Result generate(String title,String content){assertThat(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()).isFalse();calls.incrementAndGet();return action.get();}
    };}
    SharedPostAccess denied(){return new SharedPostAccess(){public Optional<GuestContext> guestForRead(long id,Action action){return Optional.empty();}public Optional<GuestContext> guestForWrite(long id,Action action){throw new AssertionError("Read only guest");}};}
    SummaryService service(CurrentActorProvider actor,SharedPostAccess access,SummaryGenerator generator){
        tx.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        return new SummaryService(actor,access,new JdbcSummaryStore(runtime),generator,tx);
    }
    @SuppressWarnings("unchecked") Map<String,Object> data(Map<String,Object> value){return (Map<String,Object>)value.get("data");}
    SummaryGenerator.Result success(){return new SummaryGenerator.Result("SUCCEEDED","첫 문장입니다. 두 번째 문장입니다. 마지막 문장입니다.");}
    @Test void cacheByRevisionAndSourceAreConsistent(){
        var service=service(actor(),denied(),generator(this::success));var first=data(service.read(agenda));var next=data(service.read(agenda));
        assertThat(first.get("status")).isEqualTo("SUCCEEDED");assertThat(next).isEqualTo(first);assertThat(calls).hasValue(1);
        assertThat(((Map<?,?>)first.get("source")).get("originalPath")).isEqualTo("/posts/"+agenda);
        admin.update("update discushion.posts set content_revision=2,content='수정 원문',updated_at=now() where id=?",agenda);
        assertThat(data(service.read(agenda)).get("status")).isEqualTo("SUCCEEDED");assertThat(calls).hasValue(2);
        assertThat(jdbc.queryForObject("select source_revision from discushion.ai_agenda_summaries where post_id=?",Long.class,agenda)).isEqualTo(2L);
    }
    @Test void failuresAndShortSourceNeverAutomaticallyRetry(){
        var service=service(actor(),denied(),generator(()->SummaryGenerator.Result.failed()));
        assertThat(data(service.read(agenda))).containsEntry("status","FAILED").containsEntry("fallbackToSource",true).containsEntry("summary",null);
        service.read(agenda);assertThat(calls).hasValue(1);
        admin.update("update discushion.posts set content_revision=2 where id=?",agenda);
        var shortService=service(actor(),denied(),generator(()->new SummaryGenerator.Result("SOURCE_TOO_SHORT",null)));
        assertThat(data(shortService.read(agenda)).get("status")).isEqualTo("SOURCE_TOO_SHORT");shortService.read(agenda);assertThat(calls).hasValue(2);
    }
    @Test void unconfiguredProviderDoesNotPoisonCache(){
        var disabled=new SummaryGenerator(){public boolean available(){return false;}public Result generate(String title,String content){throw new AssertionError("Not configured");}};
        assertThat(data(service(actor(),denied(),disabled).read(agenda)).get("status")).isEqualTo("FAILED");
        assertThat(jdbc.queryForObject("select count(*) from discushion.ai_agenda_summaries where post_id=?",Long.class,agenda)).isZero();
        assertThat(data(service(actor(),denied(),generator(this::success)).read(agenda)).get("status")).isEqualTo("SUCCEEDED");
    }
    @Test void concurrentRequestReturnsPendingWithoutDuplicateProviderCall()throws Exception{
        var started=new CountDownLatch(1);var finish=new CountDownLatch(1);var pool=Executors.newSingleThreadExecutor();
        var service=service(actor(),denied(),generator(()->{started.countDown();try{if(!finish.await(10,TimeUnit.SECONDS))throw new AssertionError("Timed out");}catch(InterruptedException e){Thread.currentThread().interrupt();throw new AssertionError(e);}return success();}));
        try{
            var first=pool.submit(()->service.read(agenda));assertThat(started.await(5,TimeUnit.SECONDS)).isTrue();
            assertThat(data(service.read(agenda)).get("status")).isEqualTo("PENDING");assertThat(calls).hasValue(1);
            finish.countDown();assertThat(data(first.get(10,TimeUnit.SECONDS)).get("status")).isEqualTo("SUCCEEDED");
        }finally{finish.countDown();pool.shutdownNow();pool.awaitTermination(10,TimeUnit.SECONDS);}
    }
    @Test void lateOldResultCannotOverwriteNewRevision(){
        var service=service(actor(),denied(),generator(()->{admin.update("update discushion.posts set content_revision=2,content='새 원문',updated_at=now() where id=?",agenda);return success();}));
        var result=data(service.read(agenda));assertThat(result.get("status")).isEqualTo("PENDING");assertThat(result.get("summary")).isNull();
        assertThat(((Map<?,?>)result.get("source")).get("content")).isEqualTo("새 원문");
        assertThat(data(service(actor(),denied(),generator(this::success)).read(agenda)).get("status")).isEqualTo("SUCCEEDED");
    }
    @Test void deletionAndNonAgendaNeverExposeSource(){
        var service=service(actor(),denied(),generator(()->{admin.update("update discushion.posts set status='DELETED',deleted_at=now() where id=?",agenda);return success();}));
        assertThatThrownBy(()->service.read(agenda)).isInstanceOf(SummaryFailure.class).satisfies(e->assertThat(((SummaryFailure)e).status).isEqualTo(404));
        long vote=post("VOTE",region,Instant.now());poll(vote,Instant.now().plusSeconds(3600));
        assertThatThrownBy(()->service.read(vote)).isInstanceOf(SummaryFailure.class).satisfies(e->assertThat(((SummaryFailure)e).status).isEqualTo(422));
    }
    @Test void guestRequiresTrustedMatchingScopeAndMemberNeverFallsBack(){
        var gen=generator(this::success);
        assertThatThrownBy(()->service(Optional::empty,denied(),gen).read(agenda)).isInstanceOf(IdentityFailure.class);
        var allowed=new SharedPostAccess(){public Optional<GuestContext> guestForRead(long id,Action action){assertThat(action).isEqualTo(Action.SUMMARY);assertThat(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()).isTrue();return Optional.of(new GuestContext(agenda,Instant.now().plusSeconds(30)));}public Optional<GuestContext> guestForWrite(long id,Action action){throw new AssertionError();}};
        assertThat(data(service(Optional::empty,allowed,gen).read(agenda)).get("status")).isEqualTo("SUCCEEDED");
        long another=post("LOCAL_AGENDA",region,Instant.now());
        assertThatThrownBy(()->service(Optional::empty,allowed,gen).read(another)).isInstanceOf(IdentityFailure.class);
        admin.update("update discushion.users set registration_completed_at=null where id=?",user);
        assertThatThrownBy(()->service(actor(),allowed,gen).read(agenda)).isInstanceOf(IdentityFailure.class);
    }
    @Test void expiredPendingFailsWithoutRetryAndRuntimeCannotDelete(){
        var store=new JdbcSummaryStore(runtime);
        tx.executeWithoutResult(status->{store.source(agenda);store.claim(store.source(agenda),store.now().minusSeconds(120));});
        assertThat(data(service(actor(),denied(),generator(this::success)).read(agenda)).get("status")).isEqualTo("FAILED");assertThat(calls).hasValue(0);
        assertThatThrownBy(()->jdbc.update("delete from discushion.ai_agenda_summaries where post_id=?",agenda)).isInstanceOf(org.springframework.dao.DataAccessException.class);
    }
    @Test void actualControllerReturnsStateNoStoreAndRejectsInvalidIds()throws Exception{
        var service=service(actor(),denied(),generator(this::success));
        var mvc=org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(new SummaryController(service))
            .setControllerAdvice(new SummaryExceptionHandler(),new com.discushion.identity.IdentityExceptionHandler()).build();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/posts/"+agenda+"/summary"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Cache-Control","no-store"));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/posts/9007199254740992/summary"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest());
        long other=post("LOCAL_ACTIVITY",region,Instant.parse("2026-10-08T00:00:00Z"));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/posts/"+other+"/summary"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isUnprocessableEntity());
    }

}
