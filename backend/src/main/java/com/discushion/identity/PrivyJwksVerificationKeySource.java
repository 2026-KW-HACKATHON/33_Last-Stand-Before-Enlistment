package com.discushion.identity;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import java.net.URI;
import java.net.http.HttpClient;
import java.security.interfaces.ECPublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import static com.discushion.identity.PrivyHttpJson.unavailable;

/** Fixed official app JWKS route, verified from Privy's node SDK. JWT jku/x5u never select the destination. */
public final class PrivyJwksVerificationKeySource implements VerificationKeySource {
    private record AppKey(String id, ECPublicKey key) {}
    private record Snapshot(Instant fetchedAt, List<AppKey> keys) {}
    private final String appId;
    private final URI base;
    private final PrivyHttpJson transport;
    private final Clock clock;
    private final Duration cacheAge;
    private final Duration refreshCooldown;
    private Snapshot snapshot;
    private Instant failureRetryAt = Instant.MIN;

    public PrivyJwksVerificationKeySource(String appId, Clock clock) {
        this(appId, URI.create("https://api.privy.io/"), PrivyHttpJson.client(), clock,
            Duration.ofMinutes(60), Duration.ofMinutes(10), Duration.ofSeconds(10));
    }

    PrivyJwksVerificationKeySource(String appId, URI base, HttpClient client, Clock clock,
            Duration cacheAge, Duration refreshCooldown, Duration timeout) {
        if (cacheAge.isZero() || cacheAge.isNegative() || refreshCooldown.isNegative()) throw new IllegalArgumentException("Invalid cache duration");
        this.appId = appId;
        this.base = PrivyHttpJson.base(base);
        this.transport = new PrivyHttpJson(client, timeout);
        this.clock = clock;
        this.cacheAge = cacheAge;
        this.refreshCooldown = refreshCooldown;
    }

    @Override
    public synchronized ECPublicKey find(String keyId) {
        if (!PrivyHttpJson.configured(appId) || !appId.matches("[A-Za-z0-9_-]{1,128}")) throw unavailable();
        var now = clock.instant();
        if (snapshot != null && !now.isBefore(snapshot.fetchedAt()) && now.isBefore(snapshot.fetchedAt().plus(cacheAge))) {
            var existing = select(snapshot.keys(), keyId);
            if (existing != null) return existing;
            if (now.isBefore(snapshot.fetchedAt().plus(refreshCooldown))) throw unavailable();
        }
        if (now.isBefore(failureRetryAt)) throw unavailable();
        try {
            var body = transport.get(base.resolve("v1/apps/" + appId + "/jwks.json"), appId, null);
            var set = JWKSet.parse(body.toString());
            if (set.getKeys().isEmpty() || set.getKeys().size() > 100) throw unavailable();
            var ids = new HashSet<String>();
            var keys = new ArrayList<AppKey>();
            for (var key : set.getKeys()) {
                if (key.getKeyID() != null && !ids.add(key.getKeyID())) throw unavailable();
                if (key.isPrivate()) throw unavailable();
                if (key instanceof ECKey ec && Curve.P_256.equals(ec.getCurve())
                        && (ec.getAlgorithm() == null || JWSAlgorithm.ES256.equals(ec.getAlgorithm()))
                        && (ec.getKeyUse() == null || KeyUse.SIGNATURE.equals(ec.getKeyUse()))
                        && (ec.getKeyOperations() == null || ec.getKeyOperations().contains(com.nimbusds.jose.jwk.KeyOperation.VERIFY))) {
                    keys.add(new AppKey(ec.getKeyID(), ec.toECPublicKey()));
                }
            }
            if (keys.isEmpty()) throw unavailable();
            snapshot = new Snapshot(now, List.copyOf(keys));
            failureRetryAt = Instant.MIN;
        } catch (Exception failure) {
            failureRetryAt = now.plusSeconds(5);
            throw unavailable();
        }
        var key = select(snapshot.keys(), keyId);
        if (key == null) throw unavailable();
        return key;
    }

    private static ECPublicKey select(List<AppKey> keys, String id) {
        if (id == null) return keys.size() == 1 ? keys.get(0).key() : null;
        return keys.stream().filter(key -> id.equals(key.id())).map(AppKey::key).findFirst().orElse(null);
    }
}
