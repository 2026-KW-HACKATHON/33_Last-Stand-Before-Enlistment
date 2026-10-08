package com.discushion.neighbor;

import com.discushion.identity.*;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.*;
import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.util.*;
import javax.sql.DataSource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import static org.assertj.core.api.Assertions.*;

/** Real HTTP/JDBC/filter with generated keys. No actual OTP/FE flow or production qualification writes. */
@EnabledIfEnvironmentVariable(named="DISCUSHION_TEST_JDBC_URL",matches="jdbc:postgresql://127\\.0\\.0\\.1:55432/discushion_migration_test(?:\\?.*)?")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
    "spring.profiles.active=neighbor11-http-test","spring.config.import=","PRIVY_APP_ID=synthetic-neighbor11-app"})
@Import(NeighborHttpIntegrationTests.Wiring.class)
class NeighborHttpIntegrationTests {
    private static final ECKey KEY=key();private static final Instant NOW=Instant.parse("2026-10-08T00:00:00Z");
    @TestConfiguration(proxyBeanMethods=false) static class Wiring {
        @Bean @Primary Clock neighborFixtureClock() {return Clock.fixed(NOW,ZoneOffset.UTC);}
        @Bean DataSource neighborFixtureSource() {return new DriverManagerDataSource(System.getenv("DISCUSHION_TEST_JDBC_URL"),"postgres",System.getenv("DISCUSHION_TEST_DB_PASSWORD"));}
        @Bean VerificationKeySource neighborFixtureKeys() throws Exception {
            return new PemVerificationKeySource("-----BEGIN PUBLIC KEY-----\n"+Base64.getEncoder().encodeToString(KEY.toECPublicKey().getEncoded())+"\n-----END PUBLIC KEY-----");
        }
    }
    @LocalServerPort int port;@Autowired DataSource source;
    private String marker,subject;private long user,other,region;
    private JdbcTemplate jdbc() {return new JdbcTemplate(source);}
    @BeforeEach void setup() {
        marker="synthetic-neighbor11-http-"+UUID.randomUUID();subject="did:privy:"+marker;
        region=jdbc().queryForObject("insert into discushion.regions(name) values(?) returning id",Long.class,marker);
        user=user(subject);other=user(subject+"-other");
    }
    private long user(String sub) {return jdbc().queryForObject("""
        insert into discushion.users(email,email_verified_at,created_at,updated_at,privy_user_id,registration_completed_at)
        values(?,?::timestamptz,?::timestamptz,?::timestamptz,?,?::timestamptz) returning id
        """,Long.class,sub.substring("did:privy:".length())+"@example.invalid",NOW.toString(),NOW.toString(),NOW.toString(),sub,NOW.toString());}
    @AfterEach void cleanup() {
        for(long id:new long[]{user,other}) {jdbc().update("delete from discushion.neighbor_verified_regions where user_id=?",id);jdbc().update("delete from discushion.users where id=?",id);}
        jdbc().update("delete from discushion.regions where id=?",region);
    }
    private String token(String sub,ECKey key,Instant until) throws Exception {
        var jwt=new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.ES256).type(JOSEObjectType.JWT).build(),new JWTClaimsSet.Builder()
            .issuer("privy.io").audience("synthetic-neighbor11-app").subject(sub).issueTime(Date.from(NOW.minusSeconds(60)))
            .expirationTime(Date.from(until)).claim("sid","synthetic-neighbor11-session").build());
        jwt.sign(new ECDSASigner(key));return jwt.serialize();
    }
    private String token() throws Exception {return token(subject,KEY,NOW.plusSeconds(120));}
    private HttpResponse<String> request(String method,String query,String access) throws Exception {
        var builder=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/v1/users/me/neighbor-verifications"+query)).timeout(Duration.ofSeconds(10));
        if(access!=null) builder.header("Authorization","Bearer "+access);
        return HttpClient.newHttpClient().send(builder.method(method,HttpRequest.BodyPublishers.noBody()).build(),HttpResponse.BodyHandlers.ofString());
    }
    @Test void emptyAndStoredListsAndTargetFlagsAre200WithoutCreatingQualifications() throws Exception {
        var empty=request("GET","",token());assertThat(empty.statusCode()).isEqualTo(200);
        assertThat(empty.body()).isEqualTo("{\"data\":{\"verifiedRegions\":[],\"maxVerifiedRegions\":3,\"targetRegion\":null}}");
        var absent=request("GET","?regionId="+region,token());assertThat(absent.statusCode()).isEqualTo(200);
        assertThat(absent.body()).contains("\"isNeighborVerified\":false");
        var seed=new NeighborDemoProvisioner(source,new DataSourceTransactionManager(source),Clock.fixed(NOW,ZoneOffset.UTC));seed.add(user,region);
        var verified=request("GET","?regionId="+region,token());assertThat(verified.statusCode()).isEqualTo(200);
        assertThat(verified.body()).contains(marker,"\"isNeighborVerified\":true","2026-10-08T09:00:00+09:00").doesNotContain(subject,"email","requests","evidenceFiles");
        assertThat(jdbc().queryForObject("select count(*) from discushion.neighbor_verified_regions where user_id=?",Integer.class,user)).isEqualTo(1);
    }
    @Test void invalidExpiredForgedAnonymousAndIncompleteSubjectsAreDenied() throws Exception {
        for(String access:Arrays.asList(null,"invalid",token(subject,KEY,NOW.minusSeconds(1)),token(subject,key(),NOW.plusSeconds(120))))
            assertThat(request("GET","",access).statusCode()).isEqualTo(401);
        assertThat(request("GET","",token(subject+"-missing",KEY,NOW.plusSeconds(120))).statusCode()).isEqualTo(403);
        jdbc().update("update discushion.users set registration_completed_at=null where id=?",user);
        var result=request("GET","",token());assertThat(result.statusCode()).isEqualTo(403);assertThat(result.body()).contains("USER_REGISTRATION_REQUIRED");
    }
    @Test void invalidUnknownOrRepeatedRegionsReturnEstablishedErrors() throws Exception {
        for(String query:List.of("?regionId=0","?regionId=1.0","?regionId=9007199254740992","?regionId=","?regionId="+region+"&regionId=1")) {
            var result=request("GET",query,token());assertThat(result.statusCode()).as(query).isEqualTo(400);assertThat(result.body()).contains("VALIDATION_ERROR","traceId");
        }
        var missing=request("GET","?regionId=9007199254740991",token());assertThat(missing.statusCode()).isEqualTo(404);assertThat(missing.body()).contains("REGION_NOT_FOUND");
    }
    @Test void clientUserIdNeverExposesOtherQualificationsAndNoPublicApprovalPostExists() throws Exception {
        new NeighborDemoProvisioner(source,new DataSourceTransactionManager(source),Clock.fixed(NOW,ZoneOffset.UTC)).add(other,region);
        var result=request("GET","?userId="+other+"&regionId="+region,token());assertThat(result.statusCode()).isEqualTo(200);
        assertThat(result.body()).contains("\"verifiedRegions\":[]","\"isNeighborVerified\":false");
        assertThat(request("POST","?regionId="+region,token()).statusCode()).isEqualTo(405);
        assertThat(jdbc().queryForObject("select count(*) from discushion.neighbor_verified_regions where user_id=?",Integer.class,user)).isZero();
    }
    private static ECKey key() {try{return new ECKeyGenerator(Curve.P_256).generate();}catch(Exception error){throw new ExceptionInInitializerError(error);}}
}
