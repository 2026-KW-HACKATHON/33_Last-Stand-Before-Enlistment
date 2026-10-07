package com.discushion.identity;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Only server-owned destinations/headers enter this transport. Never follows redirects or logs responses. */
final class PrivyHttpJson {
    private static final int MAX_BODY_BYTES = 1_048_576;
    private final HttpClient client;
    private final Duration timeout;
    private final JsonMapper json = JsonMapper.builder()
        .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
        .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build();

    PrivyHttpJson(HttpClient client, Duration timeout) {
        if (client.followRedirects() != HttpClient.Redirect.NEVER || client.connectTimeout().isEmpty()
                || timeout.toMillis() < 1) throw new IllegalArgumentException("Bounded nonredirecting client required");
        this.client = client;
        this.timeout = timeout;
    }

    static HttpClient client() {
        return HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build();
    }

    JsonNode get(URI uri, String appId, String basicAuthorization) {
        CompletableFuture<HttpResponse<byte[]>> pending = null;
        try {
            var request = HttpRequest.newBuilder(uri).timeout(timeout).header("Accept", "application/json")
                .header("privy-app-id", appId);
            if (basicAuthorization != null) request.header("Authorization", basicAuthorization);
            pending = client.sendAsync(request.GET().build(), info -> new LimitedBody());
            var response = pending.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (response.statusCode() != 200) throw unavailable();
            String type = response.headers().firstValue("Content-Type").orElse("").split(";", 2)[0].trim();
            if (!"application/json".equalsIgnoreCase(type)) throw unavailable();
            byte[] bytes = response.body();
            if (bytes.length == 0) throw unavailable();
            var tree = json.readTree(bytes);
            if (tree == null || !tree.isObject()) throw unavailable();
            return tree;
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            throw unavailable();
        } catch (ExecutionException | TimeoutException | RuntimeException failure) {
            throw unavailable();
        } finally {
            if (pending != null && !pending.isDone()) pending.cancel(true);
        }
    }

    /** Bounds bytes while receiving; the outer deadline covers a stalled body as well as headers. */
    private static final class LimitedBody implements HttpResponse.BodySubscriber<byte[]> {
        private final CompletableFuture<byte[]> result = new CompletableFuture<>();
        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        private Flow.Subscription subscription;

        @Override public CompletionStage<byte[]> getBody() { return result; }
        @Override public void onSubscribe(Flow.Subscription value) {
            if (subscription != null) { value.cancel(); return; }
            subscription = value;
            value.request(1);
        }
        @Override public void onNext(List<ByteBuffer> items) {
            for (var item : items) {
                if (item.remaining() > MAX_BODY_BYTES - bytes.size()) {
                    subscription.cancel();result.completeExceptionally(unavailable());return;
                }
                var chunk = new byte[item.remaining()];item.get(chunk);bytes.writeBytes(chunk);
            }
            subscription.request(1);
        }
        @Override public void onError(Throwable failure) { result.completeExceptionally(unavailable()); }
        @Override public void onComplete() { result.complete(bytes.toByteArray()); }
    }

    static boolean configured(String value) {
        return value != null && !value.isBlank() && !value.startsWith("<");
    }

    static URI base(URI uri) {
        boolean official = "https".equals(uri.getScheme()) && "api.privy.io".equals(uri.getHost()) && uri.getPort() == -1;
        boolean loopback = "http".equals(uri.getScheme()) && "127.0.0.1".equals(uri.getHost()) && uri.getPort() > 0;
        if ((!official && !loopback) || uri.getUserInfo() != null || uri.getRawQuery() != null || uri.getFragment() != null
                || !"/".equals(uri.getPath())) throw new IllegalArgumentException("Invalid trusted provider base");
        return uri;
    }

    static IdentityFailure unavailable() { return new IdentityFailure(IdentityFailure.Reason.PROVIDER_UNAVAILABLE); }
}
