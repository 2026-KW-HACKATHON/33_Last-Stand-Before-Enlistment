package com.discushion.identity;

import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Optional;
import static com.discushion.identity.PrivyHttpJson.unavailable;

/** Subject must come from the verified request actor. No email lookup, linking, caching or user creation. */
public final class PrivyVerifiedEmailSource {
    private final String appId;
    private final String appSecret;
    private final URI base;
    private final PrivyHttpJson transport;
    private final Clock clock;

    public PrivyVerifiedEmailSource(String appId, String appSecret, Clock clock) {
        this(appId, appSecret, URI.create("https://api.privy.io/"), PrivyHttpJson.client(), clock, Duration.ofSeconds(10));
    }

    PrivyVerifiedEmailSource(String appId, String appSecret, URI base, HttpClient client, Clock clock, Duration timeout) {
        this.appId = appId;
        this.appSecret = appSecret;
        this.base = PrivyHttpJson.base(base);
        this.transport = new PrivyHttpJson(client, timeout);
        this.clock = clock;
    }

    Optional<VerifiedEmail> find(String verifiedSubject) {
        if (!PrivyHttpJson.configured(appId) || !appId.matches("[A-Za-z0-9_-]{1,128}")
                || !PrivyHttpJson.configured(appSecret) || appSecret.codePoints().anyMatch(Character::isISOControl)) throw unavailable();
        if (verifiedSubject == null || !verifiedSubject.matches("did:privy:[A-Za-z0-9_-]{1,128}")) {
            throw new IdentityFailure(IdentityFailure.Reason.INVALID_TOKEN);
        }
        String authorization = "Basic " + Base64.getEncoder().encodeToString((appId + ":" + appSecret).getBytes(StandardCharsets.UTF_8));
        var body = transport.get(base.resolve("v1/users/" + verifiedSubject), appId, authorization);
        if (!body.has("id") || !body.get("id").isString() || !verifiedSubject.equals(body.get("id").asString())
                || !body.has("linked_accounts") || !body.get("linked_accounts").isArray()) throw unavailable();
        if (body.has("is_guest") && (!body.get("is_guest").isBoolean() || body.get("is_guest").asBoolean())) return Optional.empty();
        if (body.hasNonNull("frozen_at")) return Optional.empty();
        var emails = new ArrayList<VerifiedEmail>();
        int emailAccounts = 0;
        for (var account : body.get("linked_accounts")) {
            if (!account.isObject() || !account.has("type") || !account.get("type").isString()) throw unavailable();
            if (!"email".equals(account.get("type").asString())) continue;
            emailAccounts++;
            if (!account.has("address") || !account.get("address").isString()) throw unavailable();
            String address = account.get("address").asString();
            if (address.isBlank() || address.codePoints().anyMatch(c -> Character.isWhitespace(c) || Character.isISOControl(c))) throw unavailable();
            var timestamp = account.has("latest_verified_at") ? account.get("latest_verified_at") : account.get("verified_at");
            if (timestamp == null || timestamp.isNull()) continue;
            if (!timestamp.isIntegralNumber() || !timestamp.canConvertToLong()) throw unavailable();
            long seconds = timestamp.asLong();
            Instant verifiedAt;
            try { verifiedAt = Instant.ofEpochSecond(seconds); }
            catch (RuntimeException failure) { throw unavailable(); }
            if (seconds < 0 || verifiedAt.isAfter(clock.instant())) throw unavailable();
            emails.add(new VerifiedEmail(verifiedSubject, address, verifiedAt));
        }
        return emailAccounts == 1 && emails.size() == 1 ? Optional.of(emails.get(0)) : Optional.empty();
    }
}
