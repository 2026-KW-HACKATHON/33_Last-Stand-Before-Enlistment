package com.discushion.region;

import com.discushion.identity.PemVerificationKeySource;
import com.discushion.identity.VerificationKeySource;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;

/** Actual production Region endpoint + local JDBC. Generated key/rows are test-only, not Privy/FE integration. */
@EnabledIfEnvironmentVariable(named="DISCUSHION_TEST_JDBC_URL",matches="jdbc:postgresql://127\\.0\\.0\\.1:55432/discushion_migration_test(?:\\?.*)?")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, properties={
    "spring.profiles.active=issue9-http-test", "spring.config.import=", "PRIVY_APP_ID=synthetic-issue9-app"})
@Import(RegionHttpIntegrationTests.Wiring.class)
class RegionHttpIntegrationTests {
    private static final ECKey KEY = generatedKey();
    private final JsonMapper json = JsonMapper.builder().build();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(5)).build();
    @LocalServerPort int port;
    @Autowired DataSource source;
    private String marker;
    private final List<Long> ids = new ArrayList<>();
    private Long userId;

    @TestConfiguration(proxyBeanMethods=false)
    static class Wiring {
        @Bean DataSource regionFixtureSource() {
            return new DriverManagerDataSource(System.getenv("DISCUSHION_TEST_JDBC_URL"),"postgres",System.getenv("DISCUSHION_TEST_DB_PASSWORD"));
        }
        @Bean VerificationKeySource regionFixturePublicKey() throws Exception {
            String pem = "-----BEGIN PUBLIC KEY-----\n"+Base64.getEncoder().encodeToString(KEY.toECPublicKey().getEncoded())+"\n-----END PUBLIC KEY-----";
            return new PemVerificationKeySource(pem);
        }
    }

    @BeforeEach void prepare() {
        marker = "synthetic-i9-http-"+UUID.randomUUID();userId=null;ids.clear();
        var jdbc = new JdbcTemplate(source);
        for(int i=0;i<23;i++) {
            ids.add(jdbc.queryForObject("insert into discushion.regions(name,external_code,map_feature_key) values (?,?,?) returning id",
                Long.class,marker+String.format("-가%02d동",i),marker+"-private-code-"+i,i==0?marker+"-map-feature":null));
        }
    }

    @AfterEach void cleanup() {
        var jdbc = new JdbcTemplate(source);
        if(userId!=null) jdbc.update("delete from discushion.users where id=? and privy_user_id=?",userId,"did:privy:"+marker);
        for(long id:ids) jdbc.update("delete from discushion.regions where id=? and name like ?",id,marker+"%");
    }

    private HttpResponse<String> get(String query, String token) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/v1/regions"+query))
            .timeout(java.time.Duration.ofSeconds(5));
        if(token!=null) builder.header("Authorization","Bearer "+token);
        return http.send(builder.GET().build(),HttpResponse.BodyHandlers.ofString());
    }
    private String encode(String value) { return URLEncoder.encode(value,StandardCharsets.UTF_8); }
    private JsonNode body(HttpResponse<String> response) { return json.readTree(response.body()); }
    private String token() throws Exception {
        var now=Instant.now();
        var claims = new JWTClaimsSet.Builder().issuer("privy.io").audience("synthetic-issue9-app").subject("did:privy:"+marker)
            .issueTime(Date.from(now.minusSeconds(1))).expirationTime(Date.from(now.plusSeconds(300))).claim("sid","synthetic-session").build();
        var jwt=new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.ES256).type(JOSEObjectType.JWT).build(),claims);
        jwt.sign(new ECDSASigner(KEY));return jwt.serialize();
    }

    @Test void anonymousDefaultPagingUsesTheProductionRouteAndTwentyRows() throws Exception {
        var first = get("?q="+encode(marker),null);
        assertThat(first.statusCode()).isEqualTo(200);
        var parsed = body(first);
        assertThat(parsed.get("data").size()).isEqualTo(20);
        assertThat(parsed.get("meta").get("hasNext").asBoolean()).isTrue();
        var second = get("?q="+encode(marker)+"&cursor="+parsed.get("meta").get("nextCursor").asString(),null);
        assertThat(second.statusCode()).isEqualTo(200);
        var last = body(second);
        assertThat(last.get("data").size()).isEqualTo(3);
        assertThat(last.get("meta").get("hasNext").asBoolean()).isFalse();
        assertThat(last.get("meta").get("nextCursor").isNull()).isTrue();
        var actual = new ArrayList<Long>();
        parsed.get("data").forEach(row -> actual.add(row.get("id").asLong()));
        last.get("data").forEach(row -> actual.add(row.get("id").asLong()));
        assertThat(actual).containsExactlyElementsOf(ids).doesNotHaveDuplicates();
    }

    @Test void responseKeepsNumberIdsMapNullAndPrivateFieldsHidden() throws Exception {
        var response = get("?q="+encode(marker)+"&size=100",null);
        assertThat(response.statusCode()).isEqualTo(200);
        var rows = body(response).get("data");
        assertThat(rows.get(0).get("id").isIntegralNumber()).isTrue();
        assertThat(rows.get(0).get("id").asLong()).isEqualTo(ids.get(0));
        assertThat(rows.get(0).get("mapFeatureKey").asString()).isEqualTo(marker+"-map-feature");
        assertThat(rows.get(1).get("mapFeatureKey").isNull()).isTrue();
        assertThat(response.body()).doesNotContain("private-code","externalCode","external_code","userId","latitude","longitude");
    }

    @Test void invalidInputsHaveExistingEnvelopeAndFieldDetails() throws Exception {
        for(String invalid : List.of("size=0","size=101","size=abc","size=","q=a&q=b","size=1&size=1","cursor=","cursor=invalid","cursor=x&cursor=y","q="+encode("동".repeat(101)))) {
            var response=get("?"+invalid,null);
            assertThat(response.statusCode()).as(invalid).isEqualTo(400);
            var error=body(response);
            assertThat(error.get("code").asString()).isEqualTo("VALIDATION_ERROR");
            assertThat(error.get("details").get(0).get("field").asString()).isEqualTo(invalid.startsWith("q=")?"q":invalid.startsWith("size=")?"size":"cursor");
            assertThat(error.get("traceId").asString()).isNotBlank();
            assertThat(response.body()).doesNotContain("stackTrace","SQLException");
        }
    }

    @Test void changingSearchWithAnOldCursorRequiresRestart() throws Exception {
        var first = body(get("?q="+encode(marker)+"&size=1",null));
        String cursor=first.get("meta").get("nextCursor").asString();
        var result=get("?q=other&cursor="+cursor,null);
        assertThat(result.statusCode()).isEqualTo(400);
        assertThat(result.body()).contains("VALIDATION_ERROR","cursor");
    }

    @Test void unregisteredAndIncompleteCanReadWithoutGainingNeighbourStatus() throws Exception {
        assertThat(get("?q="+encode(marker),token()).statusCode()).isEqualTo(200);
        var jdbc=new JdbcTemplate(source);
        userId=jdbc.queryForObject("""
            insert into discushion.users(email,email_verified_at,created_at,updated_at,privy_user_id)
            values (?,now(),now(),now(),?) returning id
            """,Long.class,marker+"@example.invalid","did:privy:"+marker);
        assertThat(get("?q="+encode(marker),token()).statusCode()).isEqualTo(200);
        assertThat(jdbc.queryForObject("select count(*) from discushion.neighbor_verified_regions where user_id=?",Integer.class,userId)).isZero();
        assertThat(jdbc.queryForObject("select registration_completed_at is null from discushion.users where id=?",Boolean.class,userId)).isTrue();
    }

    @Test void invalidBearerIsNotSilentlyDowngradedToAnonymous() throws Exception {
        var result=get("?q="+encode(marker),"synthetic-invalid-token");
        assertThat(result.statusCode()).isEqualTo(401);
        assertThat(result.headers().firstValue("WWW-Authenticate")).contains("Bearer");
        assertThat(result.body()).contains("UNAUTHORIZED","traceId").doesNotContain("synthetic-invalid-token");
        assertThat(get("?q="+encode(marker),null).statusCode()).isEqualTo(200);
    }

    @Test void emptySearchResultKeepsEmptyArrayAndTerminalMeta() throws Exception {
        var result=get("?q="+encode(marker+"-not-registered"),null);
        assertThat(result.statusCode()).isEqualTo(200);
        assertThat(body(result).get("data").size()).isZero();
        assertThat(body(result).get("meta").get("hasNext").asBoolean()).isFalse();
        assertThat(body(result).get("meta").get("nextCursor").isNull()).isTrue();
    }

    private static ECKey generatedKey() {
        try { return new ECKeyGenerator(Curve.P_256).generate(); }
        catch(Exception error) { throw new ExceptionInInitializerError(error); }
    }
}
