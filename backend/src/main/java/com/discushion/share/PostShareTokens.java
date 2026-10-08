package com.discushion.share;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Dedicated HMAC protocol, not a Privy access token. No operating fallback key or token persistence. */
final class PostShareTokens {
    static final long LIFETIME_SECONDS = 7 * 24 * 60 * 60;
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private final Clock clock;
    private final String configuredKey;
    private final SecureRandom random;
    record Claim(long postId, Instant expiresAt) {}

    PostShareTokens(Clock clock, String configuredKey, SecureRandom random) {
        this.clock = clock; this.configuredKey = configuredKey; this.random = random;
    }
    String issue(long postId) {
        validPostId(postId);
        long issued = clock.instant().getEpochSecond();
        if (issued < 0) throw new IllegalStateException("Invalid server clock");
        byte[] nonce = new byte[16]; random.nextBytes(nonce);
        String payload = "v1." + postId + "." + issued + "." + Math.addExact(issued, LIFETIME_SECONDS) + "." + ENCODER.encodeToString(nonce);
        return payload + "." + ENCODER.encodeToString(mac(payload));
    }
    Claim verify(String token) {
        if (token == null || token.length() > 256) throw ShareFailure.invalidToken();
        String[] parts = token.split("\\.", -1);
        if (parts.length != 6 || !parts[0].equals("v1") || !parts[1].matches("[1-9][0-9]{0,15}")
                || !parts[2].matches("0|[1-9][0-9]{0,10}") || !parts[3].matches("0|[1-9][0-9]{0,10}")
                || !parts[4].matches("[A-Za-z0-9_-]{22}") || !parts[5].matches("[A-Za-z0-9_-]{43}")) throw ShareFailure.invalidToken();
        long postId, issued, expires;
        byte[] signature;
        try {
            postId = Long.parseLong(parts[1]); issued = Long.parseLong(parts[2]); expires = Long.parseLong(parts[3]);
            if (postId > 9007199254740991L || expires - issued != LIFETIME_SECONDS) throw ShareFailure.invalidToken();
            byte[] nonce = Base64.getUrlDecoder().decode(parts[4]); signature = Base64.getUrlDecoder().decode(parts[5]);
            if (nonce.length != 16 || signature.length != 32 || !ENCODER.encodeToString(nonce).equals(parts[4])
                    || !ENCODER.encodeToString(signature).equals(parts[5])) throw ShareFailure.invalidToken();
        } catch (IllegalArgumentException invalid) { throw ShareFailure.invalidToken(); }
        String payload = String.join(".", Arrays.copyOf(parts, 5));
        if (!MessageDigest.isEqual(mac(payload), signature)) throw ShareFailure.invalidToken();
        var claim = new Claim(postId, Instant.ofEpochSecond(expires));
        if (clock.instant().getEpochSecond() < issued) throw ShareFailure.invalidToken();
        requireCurrent(claim);
        return claim;
    }
    void requireCurrent(Claim claim) {
        if (!clock.instant().isBefore(claim.expiresAt())) throw ShareFailure.invalidToken();
    }
    private byte[] mac(String payload) {
        if (configuredKey == null || configuredKey.isBlank() || configuredKey.length() > 4096)
            throw new IllegalStateException("Share signing configuration unavailable");
        byte[] key;
        try { key = Base64.getDecoder().decode(configuredKey); }
        catch (IllegalArgumentException invalid) { throw new IllegalStateException("Invalid share signing configuration"); }
        if (key.length < 32) throw new IllegalStateException("Share signing configuration too short");
        try {
            var mac = Mac.getInstance("HmacSHA256"); mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(("discushion:post-share:" + payload).getBytes(StandardCharsets.US_ASCII));
        } catch (GeneralSecurityException failure) { throw new IllegalStateException("Share signing unavailable"); }
        finally { Arrays.fill(key, (byte) 0); }
    }
    static long validPostId(String value) {
        if (value == null || !value.matches("[1-9][0-9]{0,15}")) throw new ShareFailure(400, "VALIDATION_ERROR");
        try { long id = Long.parseLong(value); validPostId(id); return id; }
        catch (NumberFormatException invalid) { throw new ShareFailure(400, "VALIDATION_ERROR"); }
    }
    private static void validPostId(long id) {
        if (id < 1 || id > 9007199254740991L) throw new ShareFailure(400, "VALIDATION_ERROR");
    }
}
