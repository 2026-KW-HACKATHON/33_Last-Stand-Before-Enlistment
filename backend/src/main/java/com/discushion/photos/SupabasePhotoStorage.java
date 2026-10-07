package com.discushion.photos;

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import tools.jackson.databind.json.JsonMapper;
import static com.discushion.photos.PhotoFailure.Reason.*;

/** HTTP adapter is gated until actual bucket/wire and issuance bounds have been verified in #30. */
public final class SupabasePhotoStorage implements PhotoStorage {
    private final URI origin;
    private final String secret;
    private final String bucket;
    private final Duration issuanceAllowance;
    private final Clock clock;
    private final HttpClient http;
    private final Duration timeout;
    private final JsonMapper json=JsonMapper.builder().build();
    public SupabasePhotoStorage(URI origin,String secret,String bucket,Duration issuanceAllowance,Clock clock) {
        this(origin,secret,bucket,issuanceAllowance,clock,HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NEVER).build());
    }
    SupabasePhotoStorage(URI origin,String secret,String bucket,Duration issuanceAllowance,Clock clock,HttpClient http) {
        this(origin,secret,bucket,issuanceAllowance,clock,http,Duration.ofSeconds(30));
    }
    SupabasePhotoStorage(URI origin,String secret,String bucket,Duration issuanceAllowance,Clock clock,HttpClient http,Duration timeout) {
        if(origin==null || origin.getHost()==null || !"https".equals(origin.getScheme()) || origin.getUserInfo()!=null
            || origin.getQuery()!=null || origin.getFragment()!=null || !"".equals(origin.getPath()))
            throw new IllegalArgumentException("Storage origin must be an HTTPS origin without a trailing slash");
        if(secret==null || secret.isBlank() || secret.startsWith("<") || bucket==null || !bucket.matches("[a-zA-Z0-9_-]+")
            || issuanceAllowance==null || issuanceAllowance.isNegative() || issuanceAllowance.isZero())
            throw new IllegalArgumentException("Verified Storage configuration is required");
        if(timeout==null || timeout.toMillis()<1) throw new IllegalArgumentException("Positive response deadline required");
        this.origin=origin; this.secret=secret; this.bucket=bucket; this.issuanceAllowance=issuanceAllowance; this.clock=clock; this.http=http; this.timeout=timeout;
    }
    @Override public Instant authorizationUpperBound(Instant now) {return now.plusSeconds(7200).plus(issuanceAllowance);}
    @Override public Upload createUpload(String key,String mime) {
        requireKey(key);
        var response=text(request("/object/upload/sign/"+bucket+"/"+key)
            .header("x-upsert","false").header("Content-Type","application/json")
            .POST(HttpRequest.BodyPublishers.ofString("{}" )).build());
        if(response.statusCode()<200 || response.statusCode()>=300) throw unavailable();
        try {
            String relative=json.readTree(response.body()).get("url").asString();
            URI upload=URI.create(origin+"/storage/v1"+relative);
            String expected="/storage/v1/object/upload/sign/"+bucket+"/"+key;
            if(!origin.getHost().equals(upload.getHost()) || !"https".equals(upload.getScheme())
                || !expected.equals(upload.getPath()) || upload.getFragment()!=null) throw unavailable();
            String token=null;
            for(String item:upload.getRawQuery().split("&")) if(item.startsWith("token=")) token=item.substring(6);
            if(token==null) throw unavailable();
            var decoded=json.readTree(Base64.getUrlDecoder().decode(token.split("\\.")[1]));
            // The trusted HTTPS response supplies this token; it is NOT used to authenticate an app member.
            if(!decoded.has("exp") || !decoded.get("exp").isIntegralNumber()) throw unavailable();
            Instant expires=Instant.ofEpochSecond(decoded.get("exp").asLong());
            if(!expires.isAfter(clock.instant())) throw unavailable();
            return new Upload(upload.toString(),"PUT","RAW",Map.of("Content-Type",mime,"x-upsert","false"),expires);
        } catch(Exception error) {throw unavailable();}
    }
    @Override public Optional<InputStream> open(String key) {
        requireKey(key);
        long deadline=System.nanoTime()+timeout.toNanos();
        try {
            var response=receive(request("/object/authenticated/"+bucket+"/"+key).GET().build(),true);
            if(response.statusCode()>=200 && response.statusCode()<300) return Optional.of(new DeadlineImageInput(response.body(),deadline));
            String error=new String(response.body(),StandardCharsets.UTF_8);
            // Do not mistake a missing bucket, authentication failure or outage for missing object.
            var node=json.readTree(error);
            String code=node.has("code")?node.get("code").asString():"";
            if((response.statusCode()==404 || response.statusCode()==400) && "NoSuchKey".equals(code)) return Optional.empty();
            throw unavailable();
        } catch(PhotoFailure error) {throw error;}
        catch(Exception error) {if(error instanceof InterruptedException) Thread.currentThread().interrupt(); throw unavailable();}
    }
    @Override public void remove(String key) {
        requireKey(key);
        String body=json.writeValueAsString(Map.of("prefixes",java.util.List.of(key)));
        var response=text(request("/object/"+bucket).header("Content-Type","application/json")
            .method("DELETE",HttpRequest.BodyPublishers.ofString(body)).build());
        if(response.statusCode()<200 || response.statusCode()>=300) throw unavailable();
    }
    @Override public String publicUrl(String key) {requireKey(key); return origin+"/storage/v1/object/public/"+bucket+"/"+key;}
    @Override public boolean uploadsDrained(String key,Instant authorizationExpiresAt) {
        // No current verified provider barrier. Keep DELETE_PENDING, even after absence/expiry.
        return false;
    }
    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(URI.create(origin+"/storage/v1"+path)).timeout(timeout)
            .header("apikey",secret).header("Authorization","Bearer "+secret);
    }
    private record TextResponse(int statusCode,String body) {}
    static final class DeadlineImageInput extends ByteArrayInputStream {
        final long deadline;
        DeadlineImageInput(byte[] bytes,long deadline){super(bytes);this.deadline=deadline;}
    }
    private TextResponse text(HttpRequest request) {
        var response=receive(request,false);
        return new TextResponse(response.statusCode(),new String(response.body(),StandardCharsets.UTF_8));
    }
    /** Finish the bounded body before handing image inspection an in-memory stream. */
    private HttpResponse<byte[]> receive(HttpRequest request,boolean object) {
        CompletableFuture<HttpResponse<byte[]>> pending=null;
        var subscriber=new AtomicReference<LimitedBody>();
        try {
            pending=http.sendAsync(request,info->{
                boolean image=object && info.statusCode()>=200 && info.statusCode()<300;
                var body=new LimitedBody(image?PhotoContent.MAX_BYTES:65_536,image);
                subscriber.set(body); return body;
            });
            return pending.get(timeout.toMillis(),TimeUnit.MILLISECONDS);
        } catch(InterruptedException failure) {
            Thread.currentThread().interrupt(); throw unavailable();
        } catch(ExecutionException failure) {
            if(failure.getCause() instanceof PhotoFailure photo) throw photo;
            throw unavailable();
        } catch(TimeoutException | RuntimeException failure) {throw unavailable();}
        finally {
            if(subscriber.get()!=null) subscriber.get().cancel();
            if(pending!=null && !pending.isDone()) pending.cancel(true);
        }
    }
    private static final class LimitedBody implements HttpResponse.BodySubscriber<byte[]> {
        private final int limit;
        private final boolean image;
        private final CompletableFuture<byte[]> result=new CompletableFuture<>();
        private final ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        private Flow.Subscription subscription;
        private boolean cancelled;
        LimitedBody(int limit,boolean image){this.limit=limit;this.image=image;}
        public CompletionStage<byte[]> getBody(){return result;}
        public synchronized void onSubscribe(Flow.Subscription value){
            if(subscription!=null || cancelled){value.cancel();return;}
            subscription=value;value.request(1);
        }
        public synchronized void onNext(List<ByteBuffer> items){
            if(cancelled || result.isDone()) return;
            for(var item:items){
                if(item.remaining()>limit-bytes.size()){
                    result.completeExceptionally(image?new PhotoFailure(PHOTO_SIZE_EXCEEDED):unavailable());cancel();return;
                }
                byte[] chunk=new byte[item.remaining()];item.get(chunk);bytes.writeBytes(chunk);
            }
            subscription.request(1);
        }
        public synchronized void onError(Throwable error){result.completeExceptionally(unavailable());}
        public synchronized void onComplete(){result.complete(bytes.toByteArray());}
        synchronized void cancel(){cancelled=true;if(subscription!=null)subscription.cancel();}
    }
    private static void requireKey(String key) {
        if(key==null || !key.matches("post-photos/[1-9][0-9]*/[0-9a-f-]{36}")) throw unavailable();
    }
    private static PhotoFailure unavailable() {return new PhotoFailure(PHOTO_STORAGE_UNAVAILABLE);}
}
