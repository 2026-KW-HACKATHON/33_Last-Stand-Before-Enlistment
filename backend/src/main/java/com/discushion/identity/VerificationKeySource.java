package com.discushion.identity;

import java.security.interfaces.ECPublicKey;

/** Implementations supply app-pinned public keys; never follow a token's jku/x5u URL. */
@FunctionalInterface
public interface VerificationKeySource {
    ECPublicKey find(String keyId);
}
