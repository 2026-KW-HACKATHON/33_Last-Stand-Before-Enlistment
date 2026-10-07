package com.discushion.photos;

import java.io.InputStream;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/** Server-only adapter. Implementations must not log signed URLs, credentials or raw failures. */
public interface PhotoStorage {
    /** A verified provider bound, including issuance latency and clock uncertainty, before calling it. */
    Instant authorizationUpperBound(Instant now);
    Upload createUpload(String key, String contentType);
    Optional<InputStream> open(String key);
    void remove(String key);
    String publicUrl(String key);
    /** Must prove that every prior upload is finished and cannot recreate this key. Time alone is insufficient. */
    boolean uploadsDrained(String key, Instant authorizationExpiresAt);

    record Upload(String url, String method, String bodyMode, Map<String,String> headers, Instant expiresAt) {
        public Upload { headers = Map.copyOf(headers); }
        @Override public String toString() { return "Upload[redacted]"; }
    }
}
