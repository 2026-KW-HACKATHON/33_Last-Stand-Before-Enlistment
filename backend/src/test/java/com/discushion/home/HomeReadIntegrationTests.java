package com.discushion.home;
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

@EnabledIfEnvironmentVariable(named="DISCUSHION_TEST_JDBC_URL",matches="jdbc:postgresql://127\\.0\\.0\\.1:55432/discushion_migration_test(?:\\?.*)?")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HomeReadIntegrationTests {
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
    }
    @AfterEach void cleanup(){
        for(String table:List.of("ai_agenda_summaries","post_reactions","comments","activity_post_details","post_photos"))
            admin.update("delete from discushion."+table+" where post_id in(select id from discushion.posts where author_user_id=?)",user);
        admin.update("delete from discushion.vote_selections where poll_id in(select id from discushion.polls where post_id in(select id from discushion.posts where author_user_id=?))",user);
        admin.update("delete from discushion.poll_options where poll_id in(select id from discushion.polls where post_id in(select id from discushion.posts where author_user_id=?))",user);
        admin.update("delete from discushion.polls where post_id in(select id from discushion.posts where author_user_id=?)",user);
        admin.update("delete from discushion.posts where author_user_id=?",user);
        admin.update("delete from discushion.media_files where owner_user_id=?",user);
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

    HomeService service(CurrentActorProvider actors){
        tx.setReadOnly(true);tx.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        return new HomeService(actors,new JdbcHomeStore(runtime,"https://example.supabase.co","post-photos"),tx);
    }
    @SuppressWarnings("unchecked") Map<String,Object> data(Map<String,Object> result){return (Map<String,Object>)result.get("data");}
    @Test void defaultAndTemporaryRegionsNeverChangeProfile(){
        post("LOCAL_AGENDA",region,Instant.now());post("LOCAL_AGENDA",otherRegion,Instant.now());
        assertThat(data(service(actor()).read(new HomeQuery(null))).get("region")).isEqualTo(Map.of("id",region,"name",marker));
        assertThat(data(service(actor()).read(new HomeQuery(otherRegion))).get("region")).isEqualTo(Map.of("id",otherRegion,"name",marker+"-other"));
        assertThat(jdbc.queryForObject("select activity_region_id from discushion.profiles where user_id=?",Long.class,user)).isEqualTo(region);
    }
    @Test void boundedNewestOriginalCardsAndFullCounts(){
        var created=Instant.now().minusSeconds(60);var ids=new ArrayList<Long>();
        for(int i=0;i<8;i++)ids.add(post("LOCAL_AGENDA",region,created));
        var result=data(service(actor()).read(new HomeQuery(null)));
        assertThat((List<?>)result.get("posts")).hasSize(6);
        @SuppressWarnings("unchecked") var cards=(List<Map<String,Object>>)result.get("posts");
        assertThat(cards.stream().map(card->card.get("id"))).containsExactlyElementsOf(java.util.stream.IntStream.range(0,6).mapToObj(i->ids.get(7-i)).toList());
        assertThat(result.get("boardCounts")).isEqualTo(Map.of("LOCAL_AGENDA",8L,"LOCAL_ACTIVITY",0L,"VOTE",0L));
    }
    @Test void openVotesExcludeClosedAndReuseOriginalAggregates(){
        var now=Instant.now();for(int i=0;i<4;i++)poll(post("VOTE",region,now.minusSeconds(60)),now.plusSeconds(86400));
        poll(post("VOTE",region,now.minusSeconds(60)),now.minusSeconds(60));
        var result=data(service(actor()).read(new HomeQuery(null)));
        assertThat((List<?>)result.get("openVotes")).hasSize(3);
        @SuppressWarnings("unchecked") var cards=(List<Map<String,Object>>)result.get("openVotes");
        for(var card:cards){@SuppressWarnings("unchecked") var vote=(Map<String,Object>)card.get("vote");assertThat(vote.get("status")).isEqualTo("OPEN");assertThat(vote.get("participantCount")).isEqualTo(0L);assertThat((List<?>)vote.get("options")).hasSize(2);}
        assertThat(result.get("boardCounts")).isEqualTo(Map.of("LOCAL_AGENDA",0L,"LOCAL_ACTIVITY",0L,"VOTE",5L));
    }
    @Test void readActivityAndReactionsWithOnlyRequiredPermissions(){
        long activity=post("LOCAL_ACTIVITY",region,Instant.now());
        admin.update("insert into discushion.activity_post_details(post_id,source,schedule,place,activity_status) values(?,'합성 출처','합성 일정','합성 장소','SCHEDULED')",activity);
        for(String kind:List.of("EMPATHY","NEEDED","CURIOUS"))admin.update("insert into discushion.post_reactions(post_id,user_id,reaction_type,created_at) values(?,?,?,now())",activity,user,kind);
        @SuppressWarnings("unchecked") var cards=(List<Map<String,Object>>)data(service(actor()).read(new HomeQuery(null))).get("posts");
        assertThat(cards.get(0).get("activityStatus")).isEqualTo("SCHEDULED");
        assertThat(cards.get(0).get("reactionCounts")).isEqualTo(Map.of("EMPATHY",1L,"NEEDED",1L,"CURIOUS",1L,"total",3L));
        assertThatThrownBy(()->jdbc.update("update discushion.activity_post_details set place='denied' where post_id=?",activity)).isInstanceOf(org.springframework.dao.DataAccessException.class);
    }
    @Test void deletedAndOtherRegionPostsStayHidden(){
        long hidden=post("LOCAL_AGENDA",region,Instant.now());
        admin.update("update discushion.posts set status='DELETED',deleted_at=now() where id=?",hidden);
        post("LOCAL_AGENDA",otherRegion,Instant.now());
        var result=data(service(actor()).read(new HomeQuery(null)));assertThat((List<?>)result.get("posts")).isEmpty();
        assertThat(result.get("boardCounts")).isEqualTo(Map.of("LOCAL_AGENDA",0L,"LOCAL_ACTIVITY",0L,"VOTE",0L));
    }
    @Test void guestIncompleteAndUnknownRegionFailClosed(){
        assertThatThrownBy(()->service(Optional::empty).read(new HomeQuery(null))).isInstanceOf(IdentityFailure.class);
        admin.update("update discushion.users set registration_completed_at=null where id=?",user);
        assertThatThrownBy(()->service(actor()).read(new HomeQuery(region))).isInstanceOf(IdentityFailure.class);
        admin.update("update discushion.users set registration_completed_at=now() where id=?",user);
        assertThatThrownBy(()->service(actor()).read(new HomeQuery(9007199254740991L))).isInstanceOf(HomeFailure.class);
    }
    @Test void actualControllerValidatesQueryAndReturnsCommonAuthErrors()throws Exception{
        var mvc=org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(new HomeController(service(actor())))
            .setControllerAdvice(new HomeExceptionHandler(),new com.discushion.identity.IdentityExceptionHandler()).build();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/home"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Cache-Control","no-store"));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/home").param("regionId","1","2"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest());
        var guest=org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(new HomeController(service(Optional::empty)))
            .setControllerAdvice(new HomeExceptionHandler(),new com.discushion.identity.IdentityExceptionHandler()).build();
        guest.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/home"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isUnauthorized());
    }
    long photo(long post,int order,String key){
        long file=admin.queryForObject("insert into discushion.media_files(owner_user_id,storage_key,original_name,mime_type,size_bytes,purpose,created_at,lifecycle_status,uploaded_at,linked_at) values(?,?,'synthetic.png','image/png',67,'POST_PHOTO',now(),'LINKED',now(),now()) returning id",Long.class,user,marker+"/"+key);
        admin.update("insert into discushion.post_photos(post_id,file_id,sort_order) values(?,?,?)",post,file,order);return file;
    }
    @Test void thumbnailUsesAttachmentOrderAndEncodesPublicUrl(){
        long id=post("LOCAL_AGENDA",region,Instant.parse("2026-10-08T00:00:00Z"));
        photo(id,1,"second.png");photo(id,0,"first photo.png");
        @SuppressWarnings("unchecked") var cards=(List<Map<String,Object>>)data(service(actor()).read(new HomeQuery(null))).get("posts");
        assertThat(cards.get(0).get("thumbnailUrl")).isEqualTo("https://example.supabase.co/storage/v1/object/public/post-photos/"+marker+"/first%20photo.png");
    }

}
