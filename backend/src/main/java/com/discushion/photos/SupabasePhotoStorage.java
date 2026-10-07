package com.discushion.photos;

import java.io.InputStream;
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
    private final JsonMapper json=JsonMapper.builder().build();
    public SupabasePhotoStorage(URI origin,String secret,String bucket,Duration issuanceAllowance,Clock clock) {
        this(origin,secret,bucket,issuanceAllowance,clock,HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NEVER).build());
    }
    SupabasePhotoStorage(URI origin,String secret,String bucket,Duration issuanceAllowance,Clock clock,HttpClient http) {
        if(origin==null || origin.getHost()==null || !"https".equals(origin.getScheme()) || origin.getUserInfo()!=null
            || origin.getQuery()!=null || origin.getFragment()!=null || !"".equals(origin.getPath()))
            throw new IllegalArgumentException("Storage origin must be an HTTPS origin without a trailing slash");
        if(secret==null || secret.isBlank() || secret.startsWith("<") || bucket==null || !bucket.matches("[a-zA-Z0-9_-]+")
            || issuanceAllowance==null || issuanceAllowance.isNegative() || issuanceAllowance.isZero())
            throw new IllegalArgumentException("Verified Storage configuration is required");
        this.origin=origin; this.secret=secret; this.bucket=bucket; this.issuanceAllowance=issuanceAllowance; this.clock=clock; this.http=http;
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
        try {
            var response=http.send(request("/object/authenticated/"+bucket+"/"+key).GET().build(),HttpResponse.BodyHandlers.ofInputStream());
            if(response.statusCode()>=200 && response.statusCode()<300) return Optional.of(response.body());
            String error;
            try(var body=response.body()) {error=new String(body.readNBytes(4096),java.nio.charset.StandardCharsets.UTF_8);}
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
        return HttpRequest.newBuilder(URI.create(origin+"/storage/v1"+path)).timeout(Duration.ofSeconds(30))
            .header("apikey",secret).header("Authorization","Bearer "+secret);
    }
    private HttpResponse<String> text(HttpRequest request) {
        try {return http.send(request,HttpResponse.BodyHandlers.ofString());}
        catch(Exception error) {if(error instanceof InterruptedException) Thread.currentThread().interrupt(); throw unavailable();}
    }
    private static void requireKey(String key) {
        if(key==null || !key.matches("post-photos/[1-9][0-9]*/[0-9a-f-]{36}")) throw unavailable();
    }
    private static PhotoFailure unavailable() {return new PhotoFailure(PHOTO_STORAGE_UNAVAILABLE);}
}
