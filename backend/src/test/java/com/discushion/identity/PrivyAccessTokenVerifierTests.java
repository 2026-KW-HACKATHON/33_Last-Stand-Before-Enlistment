package com.discushion.identity;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class PrivyAccessTokenVerifierTests {
    private static final Instant NOW = Instant.parse("2026-10-07T00:00:00Z");
    private final com.nimbusds.jose.jwk.ECKey key;
    private final PrivyAccessTokenVerifier verifier;

    PrivyAccessTokenVerifierTests() throws Exception {
        key = new ECKeyGenerator(Curve.P_256).generate();
        verifier = new PrivyAccessTokenVerifier("synthetic-app", ignored -> {
            try { return key.toECPublicKey(); } catch (JOSEException error) {throw new IllegalStateException(error);}
        }, Clock.fixed(NOW, ZoneOffset.UTC), Duration.ZERO);
    }

    private JWTClaimsSet.Builder claims() {
        return new JWTClaimsSet.Builder().issuer("privy.io").audience("synthetic-app")
            .subject("did:privy:synthetic-user").issueTime(Date.from(NOW.minusSeconds(10)))
            .expirationTime(Date.from(NOW.plusSeconds(60))).claim("sid", "synthetic-session");
    }

    private String sign(JWTClaimsSet claims) throws Exception {
        var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.ES256).type(JOSEObjectType.JWT).build(), claims);
        jwt.sign(new ECDSASigner(key));
        return jwt.serialize();
    }

    private void invalid(String token) {
        assertThatThrownBy(() -> verifier.verify(token)).isInstanceOfSatisfying(IdentityFailure.class,
            error -> assertThat(error.reason()).isEqualTo(IdentityFailure.Reason.INVALID_TOKEN));
    }

    @Test void verifiesRealEs256SignatureAndSubject() throws Exception {
        assertThat(verifier.verify(sign(claims().build()))).isEqualTo("did:privy:synthetic-user");
    }

    @Test void rejectsAnotherIssuerAndApp() throws Exception {
        invalid(sign(claims().issuer("attacker").build()));
        invalid(sign(claims().audience("another-app").build()));
    }

    @Test void rejectsExpiryAtNowMissingExpiryAndFutureIssueTime() throws Exception {
        invalid(sign(claims().expirationTime(Date.from(NOW)).build()));
        invalid(sign(claims().expirationTime(null).build()));
        invalid(sign(claims().issueTime(Date.from(NOW.plusSeconds(1))).build()));
        invalid(sign(claims().notBeforeTime(Date.from(NOW.plusSeconds(1))).build()));
    }

    @Test void rejectsIdentityPayloadMissingSessionAndMissingSubject() throws Exception {
        invalid(sign(claims().claim("linked_accounts", List.of()).build()));
        invalid(sign(claims().claim("sid", null).build()));
        invalid(sign(claims().subject(null).build()));
        invalid(sign(claims().subject("did:privy:").build()));
    }

    @Test void rejectsWrongSigningKeyMalformedAndHmacTokens() throws Exception {
        var forged = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.ES256).type(JOSEObjectType.JWT).build(), claims().build());
        forged.sign(new ECDSASigner(new ECKeyGenerator(Curve.P_256).generate()));
        invalid(forged.serialize());
        invalid("not-a-jwt");
        var hmac = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.HS256).type(JOSEObjectType.JWT).build(), claims().build());
        hmac.sign(new MACSigner(new byte[32]));
        invalid(hmac.serialize());
    }

    @Test void distinguishesMissingOrBrokenKeyProviderFromInvalidToken() throws Exception {
        var unavailable = new PrivyAccessTokenVerifier("synthetic-app", ignored -> {
            throw new IllegalStateException("sensitive provider detail");
        }, Clock.fixed(NOW, ZoneOffset.UTC), Duration.ZERO);
        var token = sign(claims().build());
        assertThatThrownBy(() -> unavailable.verify(token)).isInstanceOfSatisfying(IdentityFailure.class,
            error -> assertThat(error.reason()).isEqualTo(IdentityFailure.Reason.PROVIDER_UNAVAILABLE))
            .hasMessage("PROVIDER_UNAVAILABLE").hasNoCause();
    }
}
