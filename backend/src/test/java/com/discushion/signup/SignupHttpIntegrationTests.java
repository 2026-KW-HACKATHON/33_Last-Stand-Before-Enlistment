package com.discushion.signup;

import com.discushion.contracts.identity.CurrentActorProvider;
import com.discushion.identity.*;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.*;
import java.net.URI;
import java.net.http.*;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Real signup HTTP/filter/signature/JDBC; test-only provider facts, not actual Privy or FE. */
@EnabledIfEnvironmentVariable(named="DISCUSHION_TEST_JDBC_URL",matches="jdbc:postgresql://127\\.0\\.0\\.1:55432/discushion_migration_test(?:\\?.*)?")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
    "spring.profiles.active=signup7-http-test","spring.config.import=","PRIVY_APP_ID=synthetic-signup7-app"})
@Import(SignupHttpIntegrationTests.Wiring.class)
class SignupHttpIntegrationTests {
    private static final com.nimbusds.jose.jwk.ECKey KEY=key();
    private static final AtomicReference<String> EMAIL_MODE=new AtomicReference<>("NORMAL");
    private static final AtomicInteger EMAIL_CALLS=new AtomicInteger();
    @LocalServerPort int port;
    @Autowired DataSource source;
    private String marker,subject,nickname;
    private long region;
    private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    @TestConfiguration(proxyBeanMethods=false)
    static class Wiring {
        @Bean DataSource signupFixtureDataSource() {
            return new DriverManagerDataSource(System.getenv("DISCUSHION_TEST_JDBC_URL"),"postgres",System.getenv("DISCUSHION_TEST_DB_PASSWORD"));
        }
        @Bean VerificationKeySource signupFixtureKey() throws Exception {
            return new PemVerificationKeySource("-----BEGIN PUBLIC KEY-----\n"+
                Base64.getEncoder().encodeToString(KEY.toECPublicKey().getEncoded())+"\n-----END PUBLIC KEY-----");
        }
        @Bean @Primary AuthenticatedEmailService signupFixtureEmails(CurrentActorProvider actors) {
            var fixture=mock(AuthenticatedEmailService.class);
            when(fixture.currentVerifiedEmail()).thenAnswer(call->{
                assertThat(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
                EMAIL_CALLS.incrementAndGet();
                if("UNAVAILABLE".equals(EMAIL_MODE.get())) throw new IdentityFailure(IdentityFailure.Reason.PROVIDER_UNAVAILABLE);
                if("EMPTY".equals(EMAIL_MODE.get())) return Optional.empty();
                var subject=actors.current().orElseThrow().privySubject();
                return Optional.of(new VerifiedEmail(subject,subject.substring("did:privy:".length())+"@example.invalid",Instant.now().minusSeconds(60)));
            });
            return fixture;
        }
    }
    @BeforeEach void setup() {
        marker="synthetic-signup7-http-"+UUID.randomUUID();subject="did:privy:"+marker;
        nickname="n"+UUID.randomUUID().toString().substring(0,8);EMAIL_MODE.set("NORMAL");EMAIL_CALLS.set(0);
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
    }
    private JdbcTemplate jdbc(){return new JdbcTemplate(source);}
    private String body(){return """
        {"agreements":{"termsOfService":true,"privacyCollection":true,"marketing":false},
         "profile":{"nickname":"%s","bio":"소개","residentAttributes":["RESIDENT"],"activityRegionId":%d}}
        """.formatted(nickname,region);}
    private String token() throws Exception {return token(Instant.now().plusSeconds(120));}
    private String token(Instant expires) throws Exception {
        var claims=new JWTClaimsSet.Builder().issuer("privy.io").audience("synthetic-signup7-app").subject(subject)
            .issueTime(Date.from(Instant.now().minusSeconds(60))).expirationTime(Date.from(expires)).claim("sid","synthetic-session").build();
        var jwt=new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.ES256).type(JOSEObjectType.JWT).build(),claims);
        jwt.sign(new ECDSASigner(KEY));return jwt.serialize();
    }
    private HttpResponse<String> post(String body,String token) throws Exception {
        var request=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/v1/auth/sign-up")).timeout(Duration.ofSeconds(10))
            .header("Content-Type","application/json");
        if(token!=null) request.header("Authorization","Bearer "+token);
        return http.send(request.POST(HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
    }
    @Test void firstCompletionIs201AndRetryIs200WithoutNewMemberOrMutation() throws Exception {
        var first=post(body(),token());assertThat(first.statusCode()).isEqualTo(201);
        assertThat(first.body()).contains("\"data\"","\"registrationStatus\":\"COMPLETED\"","\"registrationCompletedAt\"","+09:00")
            .doesNotContain("accessToken","refreshToken",subject,marker+"@example.invalid");
        var repeat=post(body().replace(nickname,"변경요청"),token());assertThat(repeat.statusCode()).isEqualTo(200);
        assertThat(repeat.body()).isEqualTo(first.body());assertThat(EMAIL_CALLS.get()).isEqualTo(1);
        assertThat(jdbc().queryForObject("select nickname from discushion.profiles p join discushion.users u on u.id=p.user_id where u.privy_user_id=?",String.class,subject)).isEqualTo(nickname);
    }
    @Test void tokenIsRequiredAndInvalidOrExpiredTokensCannotWrite() throws Exception {
        for(String token:Arrays.asList(null,"synthetic-invalid",token(Instant.now().minusSeconds(10)))) {
            var response=post(body(),token);assertThat(response.statusCode()).isEqualTo(401);
            assertThat(response.body()).contains("UNAUTHORIZED","traceId").doesNotContain("synthetic-invalid");
        }
        assertThat(EMAIL_CALLS.get()).isZero();
        assertThat(jdbc().queryForObject("select count(*) from discushion.users where privy_user_id=?",Integer.class,subject)).isZero();
    }
    @Test void missingAgreementInvalidRegionAndUnknownIdentityFieldsUseExistingEnvelope() throws Exception {
        var agreement=post(body().replace("\"termsOfService\":true","\"termsOfService\":false"),token());
        assertThat(agreement.statusCode()).isEqualTo(400);assertThat(agreement.body()).contains("REQUIRED_AGREEMENT_MISSING","details","traceId");
        var unknown=post(body().replace("\"activityRegionId\":"+region,"\"activityRegionId\":9007199254740991"),token());
        assertThat(unknown.statusCode()).isEqualTo(404);assertThat(unknown.body()).contains("REGION_NOT_FOUND");
        var injected=post(body().replaceFirst("\\{","{\"email\":\"synthetic-secret-client-email\","),token());
        assertThat(injected.statusCode()).isEqualTo(400);assertThat(injected.body()).contains("VALIDATION_ERROR").doesNotContain("synthetic-secret-client-email");
    }
    @Test void providerFailureAndMissingEmailBlockWritesButDocumentVersionsAreNotRequired() throws Exception {
        EMAIL_MODE.set("UNAVAILABLE");var unavailable=post(body(),token());
        assertThat(unavailable.statusCode()).isEqualTo(503);assertThat(unavailable.body()).contains("AUTH_PROVIDER_UNAVAILABLE").doesNotContain(subject);
        EMAIL_MODE.set("EMPTY");var noEmail=post(body(),token());
        assertThat(noEmail.statusCode()).isEqualTo(400);assertThat(noEmail.body()).contains("VALIDATION_ERROR","email");
        assertThat(jdbc().queryForObject("select count(*) from discushion.users where privy_user_id=?",Integer.class,subject)).isZero();
        EMAIL_MODE.set("NORMAL");
        var completed=post(body(),token());assertThat(completed.statusCode()).isEqualTo(201);
        assertThat(jdbc().queryForList("""
            select distinct a.policy_version from discushion.user_agreements a join discushion.users u on u.id=a.user_id
            where u.privy_user_id=?
            """,String.class,subject)).containsExactly("MVP_UI_ONLY");
    }
    @Test void existingIncompleteMemberCompletesAndFollowingAnonymousRequestCannotReuseActor() throws Exception {
        long id=jdbc().queryForObject("""
            insert into discushion.users(email,email_verified_at,created_at,updated_at,privy_user_id) values(?,?,?,?,?) returning id
            """,Long.class,marker+"@example.invalid",Timestamp.from(Instant.now().minusSeconds(60)),
            Timestamp.from(Instant.now().minusSeconds(60)),Timestamp.from(Instant.now().minusSeconds(60)),subject);
        var response=post(body(),token());assertThat(response.statusCode()).isEqualTo(201);assertThat(response.body()).contains("\"id\":"+id);
        assertThat(post(body(),null).statusCode()).isEqualTo(401);
        assertThat(jdbc().queryForObject("select count(*) from discushion.neighbor_verified_regions where user_id=?",Integer.class,id)).isZero();
    }
    @Test void malformedJsonAndNicknameConflictNeverExposeDbErrors() throws Exception {
        assertThat(post("{bad",token()).statusCode()).isEqualTo(400);
        long other=jdbc().queryForObject("""
            insert into discushion.users(email,email_verified_at,created_at,updated_at) values(?,?,?,?) returning id
            """,Long.class,marker+"-other@example.invalid",Timestamp.from(Instant.now()),Timestamp.from(Instant.now()),Timestamp.from(Instant.now()));
        jdbc().update("insert into discushion.profiles(user_id,nickname,activity_region_id,updated_at) values(?,?,?,?)",other,nickname,region,Timestamp.from(Instant.now()));
        var conflict=post(body(),token());assertThat(conflict.statusCode()).isEqualTo(409);
        assertThat(conflict.body()).contains("NICKNAME_ALREADY_IN_USE").doesNotContain("duplicate key","profiles_nickname_key","INSERT",subject);
    }
    @Test void unicodeBlankNicknameReturns400BeforeProviderOrMemberWrites() throws Exception {
        for(String blank:new String[]{"\u00A0","\u202F","\uFEFF","\u0085","\t\u00A0\uFEFF "}) {
            // JSON escapes are used for the mixed case containing a tab.
            String encoded=tools.jackson.databind.json.JsonMapper.builder().build().writeValueAsString(blank);
            var response=post(body().replace("\""+nickname+"\"",encoded),token());
            assertThat(response.statusCode()).isEqualTo(400);
            assertThat(response.body()).contains("VALIDATION_ERROR","profile.nickname","details","traceId")
                .doesNotContain("INTERNAL_ERROR","profiles_nickname_nonblank","INSERT",subject);
        }
        assertThat(EMAIL_CALLS.get()).isZero();
        assertThat(jdbc().queryForObject("select count(*) from discushion.users where privy_user_id=?",Integer.class,subject)).isZero();
    }
    @Test void validNicknameWithSpecialSpaceIsStoredUnchanged() throws Exception {
        String expected="\u00A0"+nickname;
        var response=post(body().replace(nickname,expected),token());
        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(jdbc().queryForObject("select p.nickname from discushion.profiles p join discushion.users u on u.id=p.user_id where u.privy_user_id=?",String.class,subject)).isEqualTo(expected);
    }
    private static com.nimbusds.jose.jwk.ECKey key() {
        try{return new ECKeyGenerator(Curve.P_256).generate();}catch(Exception e){throw new ExceptionInInitializerError(e);}
    }
}
