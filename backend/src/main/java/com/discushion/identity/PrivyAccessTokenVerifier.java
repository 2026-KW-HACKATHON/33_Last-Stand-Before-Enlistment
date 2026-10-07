package com.discushion.identity;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jwt.SignedJWT;
import java.time.Clock;
import java.time.Duration;
import java.text.ParseException;
import static com.discushion.identity.IdentityFailure.Reason.*;

/** ES256 according to Privy's access-token contract; real app keys must be checked separately. */
public final class PrivyAccessTokenVerifier implements AccessTokenVerifier {
    private final String appId;
    private final VerificationKeySource keys;
    private final Clock clock;
    private final Duration skew;

    public PrivyAccessTokenVerifier(String appId, VerificationKeySource keys, Clock clock, Duration skew) {
        if (skew.isNegative()) throw new IllegalArgumentException("Negative clock tolerance");
        this.appId = appId;
        this.keys = keys;
        this.clock = clock;
        this.skew = skew;
    }

    @Override
    public String verify(String accessToken) {
        try {
            var jwt = SignedJWT.parse(accessToken);
            if (!JWSAlgorithm.ES256.equals(jwt.getHeader().getAlgorithm())) throw invalid();
            if (!com.nimbusds.jose.JOSEObjectType.JWT.equals(jwt.getHeader().getType())) throw invalid();
            if (appId == null || appId.isBlank() || appId.startsWith("<")) throw unavailable();
            java.security.interfaces.ECPublicKey key;
            try { key = keys.find(jwt.getHeader().getKeyID()); }
            catch (RuntimeException error) { throw unavailable(); }
            if (key == null) throw unavailable();
            if (!com.nimbusds.jose.jwk.Curve.P_256.equals(com.nimbusds.jose.jwk.Curve.forECParameterSpec(key.getParams()))) {
                throw unavailable();
            }
            ECDSAVerifier verifier;
            try { verifier = new ECDSAVerifier(key); }
            catch (com.nimbusds.jose.JOSEException error) { throw unavailable(); }
            if (!jwt.verify(verifier)) throw invalid();
            var claims = jwt.getJWTClaimsSet();
            var now = clock.instant();
            if (!"privy.io".equals(claims.getIssuer()) || !claims.getAudience().contains(appId)
                    || claims.getExpirationTime() == null || claims.getIssueTime() == null
                    || !now.minus(skew).isBefore(claims.getExpirationTime().toInstant())
                    || now.plus(skew).isBefore(claims.getIssueTime().toInstant())
                    || !claims.getIssueTime().before(claims.getExpirationTime())
                    || (claims.getNotBeforeTime() != null && now.plus(skew).isBefore(claims.getNotBeforeTime().toInstant()))
                    || claims.getSubject() == null || !claims.getSubject().startsWith("did:privy:")
                    || claims.getSubject().length() <= "did:privy:".length()
                    || claims.getSubject().chars().anyMatch(Character::isWhitespace)
                    || claims.getStringClaim("sid") == null || claims.getStringClaim("sid").isBlank()
                    || claims.getClaims().containsKey("linked_accounts")) throw invalid();
            return claims.getSubject();
        } catch (ParseException | com.nimbusds.jose.JOSEException | IllegalArgumentException error) {
            throw invalid();
        }
    }

    private static IdentityFailure invalid() { return new IdentityFailure(INVALID_TOKEN); }
    private static IdentityFailure unavailable() { return new IdentityFailure(PROVIDER_UNAVAILABLE); }
}
