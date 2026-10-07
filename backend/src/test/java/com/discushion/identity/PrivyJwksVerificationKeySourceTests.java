package com.discushion.identity;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class PrivyJwksVerificationKeySourceTests {
    private static final Instant NOW=Instant.parse("2026-10-08T00:00:00Z");
    private static final String APP="synthetic-issue6-app";
    private ECKey key(String id) throws Exception {return new ECKeyGenerator(Curve.P_256).keyID(id).generate();}
    private String jwks(ECKey... keys) {return new JWKSet(List.of(keys)).toString();}
    private PrivyJwksVerificationKeySource source(PrivyTestServer server,Clock clock) {
        return new PrivyJwksVerificationKeySource(APP,server.base(),PrivyHttpJson.client(),clock,
            Duration.ofMinutes(60),Duration.ofMinutes(10),Duration.ofSeconds(3));
    }
    private void unavailable(org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
        assertThatThrownBy(action).isInstanceOf(IdentityFailure.class).satisfies(error ->
            assertThat(((IdentityFailure)error).reason()).isEqualTo(IdentityFailure.Reason.PROVIDER_UNAVAILABLE));
    }

    @Test void getsOnlyConfiguredAppPublicKeysAndCachesThem() throws Exception {
        try(var server=new PrivyTestServer()) {
            var key=key("trusted");server.json(jwks(key.toPublicJWK()));
            var source=source(server,Clock.fixed(NOW,ZoneOffset.UTC));
            assertThat(source.find("trusted")).isEqualTo(key.toECPublicKey());
            assertThat(source.find(null)).isEqualTo(key.toECPublicKey());
            assertThat(server.requests).hasSize(1);
            assertThat(server.requests.get(0)).isEqualTo(new PrivyTestServer.Request("/v1/apps/"+APP+"/jwks.json","GET",APP,null));
        }
    }

    @Test void keyRotationRefreshesAfterCooldownAndNeverReturnsAnUnknownKey() throws Exception {
        try(var server=new PrivyTestServer()) {
            var old=key("old");var next=key("next");var clock=new MutableClock();
            server.json(jwks(old.toPublicJWK()));var source=source(server,clock);source.find("old");
            server.json(jwks(next.toPublicJWK()));
            unavailable(() -> source.find("next"));assertThat(server.requests).hasSize(1);
            clock.advance(Duration.ofMinutes(10));
            assertThat(source.find("next")).isEqualTo(next.toECPublicKey());
            assertThat(server.requests).hasSize(2);
        }
    }

    @Test void expiredKeysFailClosedOnProviderFailure() throws Exception {
        try(var server=new PrivyTestServer()) {
            var clock=new MutableClock();server.json(jwks(key("old").toPublicJWK()));var source=source(server,clock);source.find("old");
            clock.advance(Duration.ofMinutes(60));server.respond(503,"application/json","{\"sensitive\":\"synthetic-provider-detail\"}");
            unavailable(() -> source.find("old"));
        }
    }

    @Test void providerFailureIsThrottledAndCanRecoverWithoutStaleKeys() throws Exception {
        try(var server=new PrivyTestServer()) {
            var clock=new MutableClock();var key=key("good");var source=source(server,clock);
            server.respond(500,"application/json","{}");unavailable(() -> source.find("good"));
            server.json(jwks(key.toPublicJWK()));unavailable(() -> source.find("good"));assertThat(server.requests).hasSize(1);
            clock.advance(Duration.ofSeconds(5));assertThat(source.find("good")).isEqualTo(key.toECPublicKey());
            assertThat(server.requests).hasSize(2);
        }
    }

    @Test void ambiguousKeyWithoutKidAndDuplicateKidsAreRejected() throws Exception {
        try(var server=new PrivyTestServer()) {
            server.json(jwks(key("a").toPublicJWK(),key("b").toPublicJWK()));
            unavailable(() -> source(server,Clock.fixed(NOW,ZoneOffset.UTC)).find(null));
            server.json(jwks(key("same").toPublicJWK(),key("same").toPublicJWK()));
            unavailable(() -> source(server,Clock.fixed(NOW,ZoneOffset.UTC)).find("same"));
        }
    }

    @Test void privateKeysWrongCurvesAndMalformedProviderDataAreRejected() throws Exception {
        try(var server=new PrivyTestServer()) {
            var privateKey=key("private");
            server.json("{\"keys\":["+privateKey.toJSONString()+"]}");
            unavailable(() -> source(server,Clock.fixed(NOW,ZoneOffset.UTC)).find("private"));
            var wrong=new ECKeyGenerator(Curve.P_384).keyID("wrong").generate().toPublicJWK();server.json(jwks(wrong));
            unavailable(() -> source(server,Clock.fixed(NOW,ZoneOffset.UTC)).find("wrong"));
            for(String body:List.of("{}","{\"keys\":[]}","not-json","{\"keys\":[],\"keys\":[]}","{\"keys\":[]} {}")) {
                server.json(body);unavailable(() -> source(server,Clock.fixed(NOW,ZoneOffset.UTC)).find("bad"));
            }
        }
    }

    @Test void missingOrInjectedAppIdNeverMakesAnHttpRequest() throws Exception {
        try(var server=new PrivyTestServer()) {
            for(String id:new String[]{null,"","<privy-app-id>","../other","app?secret=value"}) {
                var source=new PrivyJwksVerificationKeySource(id,server.base(),PrivyHttpJson.client(),Clock.fixed(NOW,ZoneOffset.UTC),
                    Duration.ofMinutes(60),Duration.ofMinutes(10),Duration.ofSeconds(2));
                unavailable(() -> source.find("any"));
            }
            assertThat(server.requests).isEmpty();
        }
    }

    @Test void realJwtVerificationUsesOnlyTrustedKeysAndIgnoresTokenJku() throws Exception {
        try(var server=new PrivyTestServer();var attacker=new PrivyTestServer()) {
            var key=key("trusted");server.json(jwks(key.toPublicJWK()));
            var clock=Clock.fixed(NOW,ZoneOffset.UTC);var verifier=new PrivyAccessTokenVerifier(APP,source(server,clock),clock,Duration.ZERO);
            var claims=new JWTClaimsSet.Builder().issuer("privy.io").audience(APP).subject("did:privy:synthetic-user")
                .issueTime(Date.from(NOW.minusSeconds(1))).expirationTime(Date.from(NOW.plusSeconds(100))).claim("sid","synthetic-session").build();
            var token=new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.ES256).type(JOSEObjectType.JWT).keyID("trusted").jwkURL(attacker.base()).build(),claims);
            token.sign(new ECDSASigner(key));assertThat(verifier.verify(token.serialize())).isEqualTo("did:privy:synthetic-user");
            var forged = new SignedJWT(token.getHeader(), claims);
            forged.sign(new ECDSASigner(key("forged")));
            assertThatThrownBy(() -> verifier.verify(forged.serialize())).isInstanceOf(IdentityFailure.class)
                .satisfies(error -> assertThat(((IdentityFailure)error).reason()).isEqualTo(IdentityFailure.Reason.INVALID_TOKEN));
            assertThat(attacker.requests).isEmpty();
        }
    }

    @Test void redirectsAndWrongContentTypeNeverSupplyKeys() throws Exception {
        try(var server=new PrivyTestServer();var target=new PrivyTestServer()) {
            server.redirect(target.base());unavailable(() -> source(server,Clock.fixed(NOW,ZoneOffset.UTC)).find("any"));
            assertThat(target.requests).isEmpty();
            server.respond(200,"text/html",jwks(key("good").toPublicJWK()));
            unavailable(() -> source(server,Clock.fixed(NOW,ZoneOffset.UTC)).find("good"));
        }
    }

    @Test void insecureCustomDestinationOrRedirectingClientIsRejected() {
        assertThatThrownBy(() -> PrivyHttpJson.base(URI.create("http://untrusted.example/"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PrivyHttpJson(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).followRedirects(HttpClient.Redirect.ALWAYS).build(),Duration.ofSeconds(1)))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PrivyHttpJson(HttpClient.newHttpClient(),Duration.ofSeconds(1))).isInstanceOf(IllegalArgumentException.class);
    }

    private static final class MutableClock extends Clock {
        private Instant value=NOW;
        void advance(Duration duration) {value=value.plus(duration);}
        @Override public ZoneId getZone() {return ZoneOffset.UTC;}
        @Override public Clock withZone(ZoneId zone) {return this;}
        @Override public Instant instant() {return value;}
    }
}
