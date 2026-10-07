package com.discushion.photos;

import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
import static com.discushion.photos.PhotoContentTests.assertReason;
import static com.discushion.photos.PhotoFailure.Reason.*;

class SupabasePhotoStorageTests {
    private static final String KEY="post-photos/42/12345678-1234-1234-1234-123456789abc";
    private static final Clock CLOCK=Clock.fixed(Instant.parse("2026-10-08T00:00:00Z"),ZoneOffset.UTC);
    private SupabasePhotoStorage storage(HttpClient http) {
        return new SupabasePhotoStorage(URI.create("https://example.invalid"),"synthetic-server-only","photos",Duration.ofSeconds(100),CLOCK,http);
    }
    @SuppressWarnings("unchecked")
    private <T> HttpResponse<T> response(int status,T body) {
        var result=(HttpResponse<T>)mock(HttpResponse.class);when(result.statusCode()).thenReturn(status);when(result.body()).thenReturn(body);return result;
    }
    @Test void emitsOnlyRawUploadHeadersAndStoresNoAdminKeyInGrant() throws Exception {
        var http=mock(HttpClient.class);
        String payload=Base64.getUrlEncoder().withoutPadding().encodeToString(("{\"exp\":"+CLOCK.instant().plusSeconds(7200).getEpochSecond()+"}").getBytes());
        var signed=response(200,"{\"url\":\"/object/upload/sign/photos/"+KEY+"?token=a."+payload+".b\"}");
        var signedBytes=response(200,signed.body().getBytes());
        when(http.sendAsync(any(),any(HttpResponse.BodyHandler.class))).thenReturn(java.util.concurrent.CompletableFuture.completedFuture(signedBytes));
        var grant=storage(http).createUpload(KEY,"image/png");
        assertThat(grant.headers()).containsEntry("Content-Type","image/png").containsEntry("x-upsert","false").doesNotContainKeys("Authorization","apikey");
        assertThat(grant.method()).isEqualTo("PUT");assertThat(grant.bodyMode()).isEqualTo("RAW");
        assertThat(grant.toString()).doesNotContain("token",payload,"synthetic-server-only");
        var request=ArgumentCaptor.forClass(HttpRequest.class);verify(http).sendAsync(request.capture(),any(HttpResponse.BodyHandler.class));
        assertThat(request.getValue().method()).isEqualTo("POST");
        assertThat(request.getValue().headers().firstValue("x-upsert")).contains("false");
    }
    @Test void distinguishesMissingObjectFromMissingBucketAndAuthOutage() throws Exception {
        var http=mock(HttpClient.class);
        when(http.sendAsync(any(),any(HttpResponse.BodyHandler.class))).thenAnswer(call->java.util.concurrent.CompletableFuture.completedFuture(response(404,"{\"code\":\"NoSuchKey\"}".getBytes())));
        assertThat(storage(http).open(KEY)).isEmpty();
        when(http.sendAsync(any(),any(HttpResponse.BodyHandler.class))).thenAnswer(call->java.util.concurrent.CompletableFuture.completedFuture(response(404,"{\"code\":\"NoSuchBucket\"}".getBytes())));
        assertReason(()->storage(http).open(KEY),PHOTO_STORAGE_UNAVAILABLE);
        when(http.sendAsync(any(),any(HttpResponse.BodyHandler.class))).thenAnswer(call->java.util.concurrent.CompletableFuture.completedFuture(response(403,"{\"code\":\"NoSuchKey\"}".getBytes())));
        assertReason(()->storage(http).open(KEY),PHOTO_STORAGE_UNAVAILABLE);
    }
    @Test void missingDrainProofNeverClaimsDeletionEvenAfterExpiry() {
        assertThat(storage(mock(HttpClient.class)).uploadsDrained(KEY,CLOCK.instant().minusSeconds(100000))).isFalse();
        assertThatThrownBy(()->storage(mock(HttpClient.class)).open("../../other-bucket/secret")).isInstanceOf(PhotoFailure.class).hasMessage("PHOTO_STORAGE_UNAVAILABLE");
    }
    @Test void stalledImageBodyStopsAtProductionDeadline() throws Exception {
        withServer((exchange,release)->{
            exchange.sendResponseHeaders(200,100);exchange.getResponseBody().write(137);exchange.getResponseBody().flush();
            release.await(35,java.util.concurrent.TimeUnit.SECONDS);
        },storage->{
            long start=System.nanoTime();
            assertReason(()->PhotoContent.inspect(storage.open(KEY).orElseThrow(),"image/png"),PHOTO_STORAGE_UNAVAILABLE);
            assertThat(Duration.ofNanos(System.nanoTime()-start)).isLessThan(Duration.ofSeconds(34));
        },Duration.ofSeconds(30));
    }
    @Test void stalledErrorAndMetadataBodiesAreAlsoCancelled() throws Exception {
        for(boolean metadata: new boolean[]{false,true}) {
            withServer((exchange,release)->{
                exchange.sendResponseHeaders(metadata?200:404,100);exchange.getResponseBody().write('{');exchange.getResponseBody().flush();
                release.await(5,java.util.concurrent.TimeUnit.SECONDS);
            },storage->{
                if(metadata) assertReason(()->storage.createUpload(KEY,"image/png"),PHOTO_STORAGE_UNAVAILABLE);
                else assertReason(()->storage.open(KEY),PHOTO_STORAGE_UNAVAILABLE);
            },Duration.ofSeconds(1));
        }
    }
    @Test void oversizedMetadataIsRejectedWhileReceiving() throws Exception {
        withServer((exchange,release)->{
            exchange.sendResponseHeaders(200,0);exchange.getResponseBody().write(new byte[65_537]);
        },storage->assertReason(()->storage.createUpload(KEY,"image/png"),PHOTO_STORAGE_UNAVAILABLE),Duration.ofSeconds(3));
    }
    @Test void oversizedObjectPreservesPhotoSizeFailure() throws Exception {
        withServer((exchange,release)->{
            exchange.sendResponseHeaders(200,0);exchange.getResponseBody().write(new byte[PhotoContent.MAX_BYTES+1]);
        },storage->assertReason(()->storage.open(KEY),PHOTO_SIZE_EXCEEDED),Duration.ofSeconds(5));
    }
    @Test void completeObjectReachesRealImageValidation() throws Exception {
        withServer((exchange,release)->{
            byte[] png=PhotoContentTests.png();exchange.sendResponseHeaders(200,png.length);exchange.getResponseBody().write(png);
        },storage->assertThat(PhotoContent.inspect(storage.open(KEY).orElseThrow(),"image/png").mime()).isEqualTo("image/png"),Duration.ofSeconds(5));
    }
    @FunctionalInterface interface Serve {
        void handle(com.sun.net.httpserver.HttpExchange exchange,java.util.concurrent.CountDownLatch release) throws Exception;
    }
    @SuppressWarnings("unchecked")
    private void withServer(Serve serve,java.util.function.Consumer<SupabasePhotoStorage> assertion,Duration timeout) throws Exception {
        var server=com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1",0),0);
        var release=new java.util.concurrent.CountDownLatch(1);
        var executor=java.util.concurrent.Executors.newCachedThreadPool();server.setExecutor(executor);
        server.createContext("/",exchange->{try(exchange){serve.handle(exchange,release);}catch(Exception ignored){}});server.start();
        var client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).followRedirects(HttpClient.Redirect.NEVER).build();
        var routing=mock(HttpClient.class);
        when(routing.sendAsync(any(),any(HttpResponse.BodyHandler.class))).thenAnswer(call->{
            HttpRequest original=call.getArgument(0);
            var request=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+server.getAddress().getPort()+original.uri().getRawPath()))
                .timeout(original.timeout().orElseThrow()).method(original.method(),original.bodyPublisher().orElse(HttpRequest.BodyPublishers.noBody())).build();
            return client.sendAsync(request,(HttpResponse.BodyHandler<byte[]>)call.getArgument(1));
        });
        try {
            var storage=timeout.equals(Duration.ofSeconds(30))?storage(routing):new SupabasePhotoStorage(URI.create("https://example.invalid"),"synthetic-server-only","photos",Duration.ofSeconds(100),CLOCK,routing,timeout);
            assertion.accept(storage);
        } finally {release.countDown();server.stop(0);executor.shutdownNow();}
    }
}
