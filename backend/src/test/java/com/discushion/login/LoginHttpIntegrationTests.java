package com.discushion.login;

import com.discushion.contracts.identity.CurrentActorProvider;
import com.discushion.contracts.identity.LocalMember;
import com.discushion.identity.*;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.*;
import java.net.URI;
import java.net.http.*;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.sql.DataSource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.*;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Actual HTTP/signature/filter/JDBC. Generated keys and provider facts are test-only, not a real OTP/FE flow. */
@EnabledIfEnvironmentVariable(named="DISCUSHION_TEST_JDBC_URL",matches="jdbc:postgresql://127\\.0\\.0\\.1:55432/discushion_migration_test(?:\\?.*)?")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
    "spring.profiles.active=login8-http-test","spring.config.import=","PRIVY_APP_ID=synthetic-login8-app"})
@Import(LoginHttpIntegrationTests.Wiring.class)
class LoginHttpIntegrationTests {
    private static final ECKey KEY=key();
    private static final AtomicBoolean KEY_UNAVAILABLE=new AtomicBoolean();
    private static final AtomicReference<String> READ_MODE=new AtomicReference<>("NORMAL");
    private static final AtomicInteger READS=new AtomicInteger(), EMAIL_CALLS=new AtomicInteger();
    @LocalServerPort int port;
    @Autowired DataSource source;
    private String marker,subject;
    private long region;
    private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build();

    @TestConfiguration(proxyBeanMethods=false)
    static class Wiring {
        @Bean DataSource loginFixtureDataSource() {
            return new DriverManagerDataSource(System.getenv("DISCUSHION_TEST_JDBC_URL"),"postgres",System.getenv("DISCUSHION_TEST_DB_PASSWORD"));
        }
        @Bean VerificationKeySource loginFixtureKeys() throws Exception {
            var pem=new PemVerificationKeySource("-----BEGIN PUBLIC KEY-----\n"+
                Base64.getEncoder().encodeToString(KEY.toECPublicKey().getEncoded())+"\n-----END PUBLIC KEY-----");
            return kid->{if(KEY_UNAVAILABLE.get()) throw new IdentityFailure(IdentityFailure.Reason.PROVIDER_UNAVAILABLE);return pem.find(kid);};
        }
        @Bean @Primary JdbcMemberStore loginFixtureMemberStore(DataSource source,Clock clock) {
            return new JdbcMemberStore(source,clock) {
                @Override public Optional<LocalMember> findByVerifiedSubject(String subject) {
                    int read=READS.incrementAndGet();
                    if("OUTAGE".equals(READ_MODE.get()) && read>=2)
                        throw new DataAccessResourceFailureException("synthetic-membership-db-detail");
                    var member=super.findByVerifiedSubject(subject);
                    if("COMPLETE_AFTER_SNAPSHOT".equals(READ_MODE.get()) && read==1 && member.isPresent())
                        new JdbcTemplate(source).update("update discushion.users set registration_completed_at=? where id=?",
                            Timestamp.from(clock.instant()),member.get().userId());
                    return member;
                }
            };
        }
        @Bean @Primary AuthenticatedEmailService loginFixtureSignupEmails(CurrentActorProvider actors) {
            var fixture=mock(AuthenticatedEmailService.class);
            when(fixture.currentVerifiedEmail()).thenAnswer(call->{
                EMAIL_CALLS.incrementAndGet();
                var sub=actors.current().orElseThrow().privySubject();
                return Optional.of(new VerifiedEmail(sub,sub.substring("did:privy:".length())+"@example.invalid",Instant.now().minusSeconds(60)));
            });
            return fixture;
        }
    }

    @BeforeEach void setup() {
        marker="synthetic-login8-http-"+UUID.randomUUID();subject="did:privy:"+marker;
        KEY_UNAVAILABLE.set(false);READ_MODE.set("NORMAL");READS.set(0);EMAIL_CALLS.set(0);
        region=jdbc().queryForObject("insert into discushion.regions(name) values(?) returning id",Long.class,marker);
    }
    @AfterEach void cleanup() {
        for(long id:jdbc().queryForList("select id from discushion.users where email like ?",Long.class,marker+"%")) {
            jdbc().update("delete from discushion.profile_attributes where user_id=?",id);
            jdbc().update("delete from discushion.user_agreements where user_id=?",id);
            jdbc().update("delete from discushion.profiles where user_id=?",id);
            jdbc().update("delete from discushion.users where id=?",id);
        }
        jdbc().update("delete from discushion.regions where id=? and name=?",region,marker);
        KEY_UNAVAILABLE.set(false);READ_MODE.set("NORMAL");
    }
    private JdbcTemplate jdbc() {return new JdbcTemplate(source);}
    private long member() {
        var now=Timestamp.from(Instant.now().minusSeconds(60));
        return jdbc().queryForObject("insert into discushion.users(email,email_verified_at,created_at,updated_at,privy_user_id) values(?,?,?,?,?) returning id",
            Long.class,marker+"@example.invalid",now,now,now,subject);
    }
    private String token() throws Exception {return token(KEY,"synthetic-login8-app",Instant.now().plusSeconds(120));}
    private String token(ECKey key,String app,Instant expiration) throws Exception {
        var claims=new JWTClaimsSet.Builder().issuer("privy.io").audience(app).subject(subject)
            .issueTime(Date.from(Instant.now().minusSeconds(60))).expirationTime(Date.from(expiration)).claim("sid","synthetic-session").build();
        var jwt=new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.ES256).type(JOSEObjectType.JWT).build(),claims);
        jwt.sign(new ECDSASigner(key));return jwt.serialize();
    }
    private HttpResponse<String> post(String path,String body,String token) throws Exception {
        var request=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).timeout(Duration.ofSeconds(10))
            .header("Content-Type","application/json");
        if(token!=null) request.header("Authorization","Bearer "+token);
        return http.send(request.POST(body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body)).build(),
            HttpResponse.BodyHandlers.ofString());
    }
    private HttpResponse<String> login(String body,String token) throws Exception {return post("/api/v1/auth/login",body,token);}
    private int users() {return jdbc().queryForObject("select count(*) from discushion.users where email like ?",Integer.class,marker+"%");}

    @Test void allThreeMembershipStatesAre200AndExposeOnlyEstablishedFields() throws Exception {
        var absent=login("{}",token());assertThat(absent.statusCode()).isEqualTo(200);
        assertThat(absent.body()).isEqualTo("{\"data\":{\"registrationStatus\":\"NOT_REGISTERED\",\"member\":null}}");
        assertThat(users()).isZero();
        long id=member();var incomplete=login("{}",token());
        assertThat(incomplete.statusCode()).isEqualTo(200);
        assertThat(incomplete.body()).contains("\"registrationStatus\":\"INCOMPLETE\"","\"id\":"+id,"\"registrationCompletedAt\":null");
        jdbc().update("update discushion.users set registration_completed_at=? where id=?",Timestamp.from(Instant.now()),id);
        String accessToken=token();var completed=login("{}",accessToken);
        assertThat(completed.statusCode()).isEqualTo(200);
        assertThat(completed.body()).contains("\"registrationStatus\":\"COMPLETED\"","+09:00")
            .doesNotContain(subject,marker+"@example.invalid",accessToken,"accessToken","refreshToken","role","capabilities","regionId");
        assertThat(completed.headers().allValues("Set-Cookie")).isEmpty();assertThat(EMAIL_CALLS.get()).isZero();
    }
    @Test void explicitSignupChangesStateButRepeatedLoginDoesNotWriteOrInvokeProviderEmail() throws Exception {
        assertThat(login("{}",token()).body()).contains("NOT_REGISTERED");assertThat(EMAIL_CALLS.get()).isZero();
        String name="n"+UUID.randomUUID().toString().substring(0,8);
        String signup="""
            {"agreements":{"termsOfService":true,"privacyCollection":true},
             "profile":{"nickname":"%s","activityRegionId":%d}}
            """.formatted(name,region);
        assertThat(post("/api/v1/auth/sign-up",signup,token()).statusCode()).isEqualTo(201);
        long id=jdbc().queryForObject("select id from discushion.users where privy_user_id=?",Long.class,subject);
        var before=jdbc().queryForMap("select updated_at,registration_completed_at from discushion.users where id=?",id);
        for(int index=0;index<3;index++) assertThat(login("{}",token()).body()).contains("COMPLETED");
        assertThat(users()).isEqualTo(1);assertThat(EMAIL_CALLS.get()).isEqualTo(1);
        assertThat(jdbc().queryForMap("select updated_at,registration_completed_at from discushion.users where id=?",id)).isEqualTo(before);
        assertThat(jdbc().queryForObject("select count(*) from discushion.user_agreements where user_id=?",Integer.class,id)).isEqualTo(3);
        assertThat(jdbc().queryForObject("select count(*) from discushion.neighbor_verified_regions where user_id=?",Integer.class,id)).isZero();
        assertThat(jdbc().queryForObject("select count(*) from discushion.institution_credentials where user_id=?",Integer.class,id)).isZero();
    }
    @Test void missingInvalidExpiredForgedAndWrongAppTokensAre401WithoutWrites() throws Exception {
        for(String access:Arrays.asList(null,"synthetic-invalid",token(KEY,"synthetic-login8-app",Instant.now().minusSeconds(10)),
                token(key(),"synthetic-login8-app",Instant.now().plusSeconds(120)),token(KEY,"synthetic-other-app",Instant.now().plusSeconds(120)))) {
            var result=login("{}",access);assertThat(result.statusCode()).isEqualTo(401);
            assertThat(result.body()).contains("UNAUTHORIZED","traceId").doesNotContain(subject,"synthetic-invalid");
            assertThat(result.headers().firstValue("WWW-Authenticate")).contains("Bearer");
        }
        assertThat(users()).isZero();assertThat(EMAIL_CALLS.get()).isZero();
    }
    @Test void malformedMissingNullArrayOrNonemptyJsonIs400WithoutEchoingClientFields() throws Exception {
        for(String body:Arrays.asList(null,"","null","[]","{bad","{\"email\":\"synthetic-client-secret\"}",
                "{\"userId\":999}","{\"token\":\"synthetic-client-secret\"}","{\"returnTo\":\"https://example.invalid\"}")) {
            var response=login(body,token());assertThat(response.statusCode()).as("body=%s",body).isEqualTo(400);
            assertThat(response.body()).contains("VALIDATION_ERROR","details","traceId").doesNotContain("synthetic-client-secret","example.invalid","stackTrace");
        }
        assertThat(users()).isZero();
    }
    @Test void clientQueryMemberAndReturnToCannotSelectAnotherMemberOrRedirect() throws Exception {
        long id=member();
        var response=post("/api/v1/auth/login?userId=999&returnTo=https%3A%2F%2Fexample.invalid","{}",token());
        assertThat(response.statusCode()).isEqualTo(200);assertThat(response.body()).contains("\"id\":"+id).doesNotContain("999","example.invalid");
        assertThat(response.headers().firstValue("Location")).isEmpty();
    }
    @Test void currentMembershipIsRereadAfterFilterSnapshot() throws Exception {
        long id=member();READ_MODE.set("COMPLETE_AFTER_SNAPSHOT");
        var response=login("{}",token());assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("COMPLETED","\"id\":"+id);assertThat(READS.get()).isEqualTo(2);
    }
    @Test void membershipReadFailureIs500AndDoesNotBecomeNotRegistered() throws Exception {
        READ_MODE.set("OUTAGE");var response=login("{}",token());
        assertThat(response.statusCode()).isEqualTo(500);
        assertThat(response.body()).contains("INTERNAL_ERROR","traceId").doesNotContain("NOT_REGISTERED","synthetic-membership-db-detail",subject);
        assertThat(users()).isZero();
    }
    @Test void verificationKeyFailureIs503AndCanRecoverWithoutLeakingActor() throws Exception {
        KEY_UNAVAILABLE.set(true);var response=login("{}",token());
        assertThat(response.statusCode()).isEqualTo(503);assertThat(response.body()).contains("AUTH_PROVIDER_UNAVAILABLE").doesNotContain(subject);
        KEY_UNAVAILABLE.set(false);assertThat(login("{}",token()).statusCode()).isEqualTo(200);
        assertThat(login("{}",null).statusCode()).isEqualTo(401);
        assertThat(users()).isZero();assertThat(EMAIL_CALLS.get()).isZero();
    }
    private static ECKey key() {
        try{return new ECKeyGenerator(Curve.P_256).generate();}catch(Exception failure){throw new ExceptionInInitializerError(failure);}
    }
}
