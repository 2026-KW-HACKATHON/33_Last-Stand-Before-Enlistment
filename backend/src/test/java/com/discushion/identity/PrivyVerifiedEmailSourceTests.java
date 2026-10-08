package com.discushion.identity;

import com.discushion.contracts.identity.LocalMember;
import com.discushion.contracts.identity.VerifiedActor;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static org.assertj.core.api.Assertions.*;

/** Generated provider responses only; these tests do not send a real OTP. */
class PrivyVerifiedEmailSourceTests {
    private static final String APP = "synthetic-issue6-app";
    private static final String SECRET = "synthetic-issue6-secret";
    private static final String SUBJECT = "did:privy:synthetic-issue6-user";
    private static final Instant NOW = Instant.parse("2026-10-08T00:00:00Z");
    private static final long VERIFIED = NOW.minusSeconds(60).getEpochSecond();

    @Test void returnsVerifiedProviderFactsAndUsesOnlyServerCredentials() throws Exception {
        try (var server = new PrivyTestServer()) {
            server.json(user(account("user@example.invalid", Long.toString(VERIFIED))));
            var email = source(server).find(SUBJECT).orElseThrow();
            assertThat(email).isEqualTo(new VerifiedEmail(SUBJECT, "user@example.invalid", Instant.ofEpochSecond(VERIFIED)));
            assertThat(email.toString()).doesNotContain(SUBJECT, "example.invalid");
            assertThat(server.requests).hasSize(1);
            var request = server.requests.get(0);
            assertThat(request.path()).isEqualTo("/v1/users/" + SUBJECT);
            assertThat(request.method()).isEqualTo("GET");
            assertThat(request.appId()).isEqualTo(APP);
            assertThat(request.authorization()).isEqualTo("Basic " + Base64.getEncoder()
                .encodeToString((APP + ":" + SECRET).getBytes(StandardCharsets.UTF_8)));
        }
    }

    @Test void noEmailAndOauthEmailCannotSupplyAnOtpEmail() throws Exception {
        try (var server = new PrivyTestServer()) {
            server.json(user());
            assertThat(source(server).find(SUBJECT)).isEmpty();
            server.json(user("{\"type\":\"google_oauth\",\"email\":\"user@example.invalid\",\"verified_at\":" + VERIFIED + "}"));
            assertThat(source(server).find(SUBJECT)).isEmpty();
        }
    }

    @Test void missingOrNullVerificationCannotBecomeVerified() throws Exception {
        try (var server = new PrivyTestServer()) {
            for (var account : List.of(
                    "{\"type\":\"email\",\"address\":\"user@example.invalid\"}",
                    account("user@example.invalid", "null"),
                    "{\"type\":\"email\",\"address\":\"user@example.invalid\",\"latest_verified_at\":null,\"verified_at\":" + VERIFIED + "}")) {
                server.json(user(account));
                assertThat(source(server).find(SUBJECT)).isEmpty();
            }
        }
    }

    @Test void supportsVerifiedAtOnlyWhenLatestFieldIsAbsent() throws Exception {
        try (var server = new PrivyTestServer()) {
            server.json(user("{\"type\":\"email\",\"address\":\"user@example.invalid\",\"verified_at\":" + VERIFIED + "}"));
            assertThat(source(server).find(SUBJECT).orElseThrow().verifiedAt()).isEqualTo(Instant.ofEpochSecond(VERIFIED));
        }
    }

    @Test void multipleEmailAccountsRequireAnExplicitFutureSelectionContract() throws Exception {
        try (var server = new PrivyTestServer()) {
            for (var second : List.of(Long.toString(VERIFIED), "null")) {
                server.json(user(account("one@example.invalid", Long.toString(VERIFIED)), account("two@example.invalid", second)));
                assertThat(source(server).find(SUBJECT)).isEmpty();
            }
        }
    }

    @Test void guestOrFrozenUserCannotProvideAnEligibleEmail() throws Exception {
        try (var server = new PrivyTestServer()) {
            var normal = user(account("user@example.invalid", Long.toString(VERIFIED)));
            server.json(normal.substring(0, normal.length() - 1) + ",\"is_guest\":true}");
            assertThat(source(server).find(SUBJECT)).isEmpty();
            server.json(normal.substring(0, normal.length() - 1) + ",\"frozen_at\":" + VERIFIED + "}");
            assertThat(source(server).find(SUBJECT)).isEmpty();
        }
    }

    @Test void wrongSubjectAndMalformedProviderShapeFailClosed() throws Exception {
        try (var server = new PrivyTestServer()) {
            for (var body : List.of(
                    user().replace(SUBJECT, "did:privy:other-user"),
                    "{\"linked_accounts\":[]}",
                    "{\"id\":\"" + SUBJECT + "\",\"linked_accounts\":{}}",
                    user("null"), user("{\"type\":1}"))) {
                server.json(body);
                unavailable(() -> source(server).find(SUBJECT));
            }
        }
    }

    @Test void malformedOrFutureTimestampAndUnsafeAddressFailClosed() throws Exception {
        try (var server = new PrivyTestServer()) {
            for (var timestamp : List.of("-1", "1.5", "\"not-time\"", "9223372036854775808", Long.toString(NOW.plusSeconds(1).getEpochSecond()))) {
                server.json(user(account("user@example.invalid", timestamp)));
                unavailable(() -> source(server).find(SUBJECT));
            }
            server.json(user(account("user @example.invalid", Long.toString(VERIFIED))));
            unavailable(() -> source(server).find(SUBJECT));
        }
    }

    @Test void missingCredentialsAndInjectedSubjectNeverCallProvider() throws Exception {
        try (var server = new PrivyTestServer()) {
            for (var invalid : new String[]{null, " ", "<not-configured>", "secret\ninjected"}) {
                var source = new PrivyVerifiedEmailSource(APP, invalid, server.base(), PrivyHttpJson.client(),
                    Clock.fixed(NOW, ZoneOffset.UTC), Duration.ofSeconds(3));
                unavailable(() -> source.find(SUBJECT));
            }
            for (var subject : new String[]{null, "user", "did:privy:", "did:privy:../user", "did:privy:user?q=x", "did:privy:user name"}) {
                assertThatThrownBy(() -> source(server).find(subject)).isInstanceOfSatisfying(IdentityFailure.class,
                    failure -> assertThat(failure.reason()).isEqualTo(IdentityFailure.Reason.INVALID_TOKEN));
            }
            assertThat(server.requests).isEmpty();
        }
    }

    @Test void providerFailuresRedirectsAndInvalidJsonAreSanitized() throws Exception {
        try (var server = new PrivyTestServer(); var trap = new PrivyTestServer()) {
            for (var status : List.of(400, 401, 403, 404, 429, 500, 503)) {
                server.respond(status, "application/json", "{\"secret\":\"synthetic-provider-body\"}");
                unavailable(() -> source(server).find(SUBJECT));
            }
            server.redirect(trap.base());
            unavailable(() -> source(server).find(SUBJECT));
            assertThat(trap.requests).isEmpty();
            for (var body : List.of("not-json", user() + "{}", "{\"id\":\"a\",\"id\":\"b\"}", "x".repeat(1_048_577))) {
                server.json(body);
                unavailable(() -> source(server).find(SUBJECT));
            }
            server.respond(200, "text/html", user());
            unavailable(() -> source(server).find(SUBJECT));
        }
    }

    @Test void timeoutCoversBothHeadersAndStalledBody() throws Exception {
        for (var bodyDelay : List.of(false, true)) {
            try (var server = new PrivyTestServer()) {
                server.json(user());
                server.delay(bodyDelay ? 0 : 700, bodyDelay ? 700 : 0);
                var source = new PrivyVerifiedEmailSource(APP, SECRET, server.base(), PrivyHttpJson.client(),
                    Clock.fixed(NOW, ZoneOffset.UTC), Duration.ofMillis(200));
                var started = System.nanoTime();
                unavailable(() -> source.find(SUBJECT));
                assertThat(Duration.ofNanos(System.nanoTime() - started)).isLessThan(Duration.ofSeconds(2));
            }
        }
    }

    @Test void providerEmailFactsAreNotCached() throws Exception {
        try (var server = new PrivyTestServer()) {
            var source = source(server);
            server.json(user(account("one@example.invalid", Long.toString(VERIFIED))));
            assertThat(source.find(SUBJECT).orElseThrow().address()).isEqualTo("one@example.invalid");
            server.json(user(account("two@example.invalid", Long.toString(VERIFIED))));
            assertThat(source.find(SUBJECT).orElseThrow().address()).isEqualTo("two@example.invalid");
            assertThat(server.requests).hasSize(2);
        }
    }

    @Test void serviceRequiresTrustedActorAndAllowsPreSignupActorWithoutGrantingMembership() throws Exception {
        try (var server = new PrivyTestServer()) {
            server.json(user(account("user@example.invalid", Long.toString(VERIFIED))));
            var anonymous = new AuthenticatedEmailService(Optional::empty, source(server));
            assertThatThrownBy(anonymous::currentVerifiedEmail).isInstanceOfSatisfying(IdentityFailure.class,
                failure -> assertThat(failure.reason()).isEqualTo(IdentityFailure.Reason.INVALID_TOKEN));
            assertThat(server.requests).isEmpty();
            for (var member : List.of(Optional.<LocalMember>empty(), Optional.of(new LocalMember(1, Optional.empty())))) {
                var actor = new VerifiedActor(SUBJECT, member);
                var service = new AuthenticatedEmailService(() -> Optional.of(actor), source(server));
                assertThat(service.currentVerifiedEmail().orElseThrow().privySubject()).isEqualTo(SUBJECT);
                assertThat(actor.member()).isEqualTo(member);
            }
        }
    }

    @Test void providerNetworkLookupMustRunBeforeDbWriteTransaction() throws Exception {
        try (var server = new PrivyTestServer()) {
            var service = new AuthenticatedEmailService(() -> Optional.of(new VerifiedActor(SUBJECT, Optional.empty())), source(server));
            TransactionSynchronizationManager.setActualTransactionActive(true);
            try {
                assertThatThrownBy(service::currentVerifiedEmail).isInstanceOf(IllegalStateException.class);
                assertThat(server.requests).isEmpty();
            } finally { TransactionSynchronizationManager.setActualTransactionActive(false); }
        }
    }

    private static PrivyVerifiedEmailSource source(PrivyTestServer server) {
        return new PrivyVerifiedEmailSource(APP, SECRET, server.base(), PrivyHttpJson.client(), Clock.fixed(NOW, ZoneOffset.UTC), Duration.ofSeconds(3));
    }
    private static String account(String address, String timestamp) {
        return "{\"type\":\"email\",\"address\":\"" + address + "\",\"latest_verified_at\":" + timestamp + "}";
    }
    private static String user(String... accounts) {
        return "{\"id\":\"" + SUBJECT + "\",\"linked_accounts\":[" + String.join(",", accounts) + "]}";
    }
    private static void unavailable(Runnable action) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(IdentityFailure.class, failure -> {
            assertThat(failure.reason()).isEqualTo(IdentityFailure.Reason.PROVIDER_UNAVAILABLE);
            assertThat(failure.toString()).doesNotContain(SECRET, SUBJECT, "example.invalid", "synthetic-provider-body");
            assertThat(failure.getCause()).isNull();
        });
    }
}
