package com.discushion.map;
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
class DongReadIntegrationTests {
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

    DongService service(CurrentActorProvider actors){tx.setReadOnly(true);tx.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);return new DongService(actors,new JdbcDongStore(runtime,"https://example.supabase.co","post-photos"),tx);}
    @SuppressWarnings("unchecked") Map<String,Object> data(Map<String,Object> result){return (Map<String,Object>)result.get("data");}
    @SuppressWarnings("unchecked") List<Map<String,Object>> dongs(Map<String,Object> result){return (List<Map<String,Object>>)data(result).get("dongs");}
    @Test void emptyDongsAndInputOrderArePreserved(){
        var result=service(actor()).read(new DongQuery(null,List.of(otherRegion,region)));
        assertThat(data(result).get("centerRegion")).isEqualTo(Map.of("id",region,"name",marker));
        var rows=dongs(result);assertThat(rows).hasSize(2);assertThat(rows.get(0).get("region")).isEqualTo(Map.of("id",otherRegion,"name",marker+"-other"));assertThat(rows.get(0)).containsEntry("representativePost",null);
    }
    @Test void reactionTotalWinsAndRequeryReflectsChanges(){
        long agenda=post("LOCAL_AGENDA",region,Instant.now().minusSeconds(60)),vote=post("VOTE",region,Instant.now());poll(vote,Instant.now().plusSeconds(3600));
        admin.update("insert into discushion.post_reactions(post_id,user_id,reaction_type,created_at) values(?,?,'EMPATHY',now())",agenda,user);
        assertThat(((Map<?,?>)dongs(service(actor()).read(new DongQuery(null,List.of(region)))).get(0).get("representativePost")).get("id")).isEqualTo(agenda);
        for(String kind:List.of("EMPATHY","NEEDED"))admin.update("insert into discushion.post_reactions(post_id,user_id,reaction_type,created_at) values(?,?,?,now())",vote,user,kind);
        var card=(Map<?,?>)dongs(service(actor()).read(new DongQuery(null,List.of(region)))).get(0).get("representativePost");assertThat(card.get("id")).isEqualTo(vote);assertThat(card.get("reactionCount")).isEqualTo(2L);
    }
    @Test void sameTimeAndReactionTieUsesOriginalId(){
        var time=Instant.parse("2026-10-08T00:00:00Z");post("LOCAL_AGENDA",region,time);long latest=post("LOCAL_AGENDA",region,time);
        var card=(Map<?,?>)dongs(service(actor()).read(new DongQuery(null,List.of(region)))).get(0).get("representativePost");assertThat(card.get("id")).isEqualTo(latest);
    }
    @Test void activitiesDeletedAndOtherRegionsNeverCompete(){
        long activity=post("LOCAL_ACTIVITY",region,Instant.now()),deleted=post("LOCAL_AGENDA",region,Instant.now());post("LOCAL_AGENDA",otherRegion,Instant.now());
        admin.update("update discushion.posts set status='DELETED',deleted_at=now() where id=?",deleted);
        for(long id:List.of(activity,deleted))for(String kind:List.of("EMPATHY","NEEDED","CURIOUS"))admin.update("insert into discushion.post_reactions(post_id,user_id,reaction_type,created_at) values(?,?,?,now())",id,user,kind);
        assertThat(dongs(service(actor()).read(new DongQuery(null,List.of(region)))).get(0)).containsEntry("representativePost",null);
    }
    @Test void temporaryCenterDoesNotChangeProfile(){
        var result=service(actor()).read(new DongQuery(otherRegion,List.of(region)));
        assertThat(data(result).get("centerRegion")).isEqualTo(Map.of("id",otherRegion,"name",marker+"-other"));
        assertThat(jdbc.queryForObject("select activity_region_id from discushion.profiles where user_id=?",Long.class,user)).isEqualTo(region);
    }
    @Test void guestIncompleteAndUnknownCatalogIdsAreRejected(){
        assertThatThrownBy(()->service(Optional::empty).read(new DongQuery(null,List.of(region)))).isInstanceOf(IdentityFailure.class);
        assertThatThrownBy(()->service(actor()).read(new DongQuery(null,List.of(9007199254740991L)))).isInstanceOf(MapFailure.class);
        admin.update("update discushion.users set registration_completed_at=null where id=?",user);
        assertThatThrownBy(()->service(actor()).read(new DongQuery(null,List.of(region)))).isInstanceOf(IdentityFailure.class);
    }
    long photo(long post,int order,String key){
        long file=admin.queryForObject("insert into discushion.media_files(owner_user_id,storage_key,original_name,mime_type,size_bytes,purpose,created_at,lifecycle_status,uploaded_at,linked_at) values(?,?,'synthetic.png','image/png',67,'POST_PHOTO',now(),'LINKED',now(),now()) returning id",Long.class,user,marker+"/"+key);
        admin.update("insert into discushion.post_photos(post_id,file_id,sort_order) values(?,?,?)",post,file,order);return file;
    }
    @Test void thumbnailUsesAttachmentOrderAndEncodesPublicUrl(){
        long id=post("LOCAL_AGENDA",region,Instant.parse("2026-10-08T00:00:00Z"));
        photo(id,1,"second.png");photo(id,0,"first photo.png");
        var card=(Map<?,?>)dongs(service(actor()).read(new DongQuery(null,List.of(region)))).get(0).get("representativePost");
        assertThat(card.get("thumbnailUrl")).isEqualTo("https://example.supabase.co/storage/v1/object/public/post-photos/"+marker+"/first%20photo.png");
    }

}
