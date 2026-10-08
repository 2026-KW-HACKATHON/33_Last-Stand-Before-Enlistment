package com.discushion.identity;

/** Returns a verified provider subject, never a client-selected local member ID. */
@FunctionalInterface
public interface AccessTokenVerifier {
    String verify(String accessToken);
}
