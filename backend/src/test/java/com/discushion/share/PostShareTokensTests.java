package com.discushion.share;

import java.security.SecureRandom;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class PostShareTokensTests {
    private static final Instant NOW = Instant.parse("2026-10-08T00:00:00Z");
    private final AtomicReference<Instant> time = new AtomicReference<>(NOW);
    private final Clock clock = new Clock() {
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return Clock.fixed(instant(), zone); }
        @Override public Instant instant() { return time.get(); }
    };
    private static String key() { byte[] bytes = new byte[32]; new SecureRandom().nextBytes(bytes); return Base64.getEncoder().encodeToString(bytes); }
    private final String key = key();
    private final PostShareTokens tokens = new PostShareTokens(clock, key, new SecureRandom());

    @Test void newLinksDifferWhileBothRemainReusableForExactlySevenDays() {
        String first = tokens.issue(101), second = tokens.issue(101);
        assertThat(second).isNotEqualTo(first);
        for (String token : List.of(first, second)) {
            assertThat(tokens.verify(token).postId()).isEqualTo(101);
            assertThat(tokens.verify(token).expiresAt()).isEqualTo(NOW.plusSeconds(604800));
            assertThat(tokens.verify(token).postId()).isEqualTo(101);
        }
    }
    @Test void claimAndSignatureTamperingNeverProducesTrustedContext() {
        String token = tokens.issue(101);
        for (int index = 1; index < 6; index++) {
            var parts = token.split("\\.");
            parts[index] = switch (index) {
                case 1 -> "102";
                case 2, 3 -> Long.toString(Long.parseLong(parts[index]) + 1);
                default -> (parts[index].startsWith("A") ? "B" : "A") + parts[index].substring(1);
            };
            assertInvalid(String.join(".", parts));
        }
    }
    @Test void expiredAndNotYetIssuedTokensAreDeniedAtTheBoundary() {
        String token = tokens.issue(101);
        time.set(NOW.plusSeconds(604800).minusNanos(1)); assertThat(tokens.verify(token).postId()).isEqualTo(101);
        time.set(NOW.plusSeconds(604800)); assertInvalid(token);
        time.set(NOW.minusSeconds(1)); assertInvalid(token);
    }
    @Test void malformedUnboundedAndUnsupportedTokensAreDenied() {
        String token = tokens.issue(101);
        for (String bad : Arrays.asList(null, "", " ", "v1", token + ".extra", token.replace("v1.", "v2."),
                "x".repeat(257), token + "=", token.replace(".101.", ".9007199254740992."))) assertInvalid(bad);
        for (String id : new String[]{"0", "-1", "01", "1e3", "1.0", "9007199254740992"})
            assertThatThrownBy(() -> PostShareTokens.validPostId(id)).isInstanceOf(ShareFailure.class);
    }
    @Test void absentMalformedAndWeakKeysNeverUseAnOperatingFallback() {
        for (String bad : Arrays.asList(null, "", "not base64!", Base64.getEncoder().encodeToString(new byte[16])))
            assertThatThrownBy(() -> new PostShareTokens(clock, bad, new SecureRandom()).issue(101)).isInstanceOf(IllegalStateException.class);
    }
    @Test void onlyTheDedicatedIssuingKeyCanValidateItsTokens() {
        String token = tokens.issue(101);
        assertThatThrownBy(() -> new PostShareTokens(clock, key(), new SecureRandom()).verify(token))
                .isInstanceOfSatisfying(ShareFailure.class, error -> assertThat(error.code).isEqualTo("UNAUTHORIZED"));
        assertThat(new PostShareTokens(clock, key, new SecureRandom()).verify(token).postId()).isEqualTo(101);
    }
    private void assertInvalid(String token) {
        assertThatThrownBy(() -> tokens.verify(token)).isInstanceOfSatisfying(ShareFailure.class, error -> {
            assertThat(error.status).isEqualTo(401); assertThat(error.code).isEqualTo("UNAUTHORIZED");
        });
    }
}
