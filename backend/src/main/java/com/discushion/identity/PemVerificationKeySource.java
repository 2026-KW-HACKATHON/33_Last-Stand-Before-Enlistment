package com.discushion.identity;

import java.security.KeyFactory;
import java.security.interfaces.ECPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/** Parses only a trusted app public SPKI key supplied by server configuration. No HTTP lookup. */
public final class PemVerificationKeySource implements VerificationKeySource {
    private final ECPublicKey key;

    public PemVerificationKeySource(String trustedPublicPem) {
        try {
            if (!trustedPublicPem.startsWith("-----BEGIN PUBLIC KEY-----")
                    || !trustedPublicPem.stripTrailing().endsWith("-----END PUBLIC KEY-----")) {
                throw new IllegalArgumentException("Expected a public SPKI key");
            }
            String encoded = trustedPublicPem.replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "").replaceAll("\\s", "");
            key = (ECPublicKey) KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(encoded)));
            if (!com.nimbusds.jose.jwk.Curve.P_256.equals(com.nimbusds.jose.jwk.Curve.forECParameterSpec(key.getParams()))) {
                throw new IllegalArgumentException("Expected ES256 P-256 public key");
            }
        } catch (Exception error) {
            throw new IllegalArgumentException("Invalid app verification public key");
        }
    }

    @Override public ECPublicKey find(String keyId) { return key; }
}
