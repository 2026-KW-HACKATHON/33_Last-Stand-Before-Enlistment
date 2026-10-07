package com.discushion.photos;

import java.io.ByteArrayInputStream;
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
        when(http.send(any(),any(HttpResponse.BodyHandler.class))).thenReturn(signed);
        var grant=storage(http).createUpload(KEY,"image/png");
        assertThat(grant.headers()).containsEntry("Content-Type","image/png").containsEntry("x-upsert","false").doesNotContainKeys("Authorization","apikey");
        assertThat(grant.method()).isEqualTo("PUT");assertThat(grant.bodyMode()).isEqualTo("RAW");
        assertThat(grant.toString()).doesNotContain("token",payload,"synthetic-server-only");
        var request=ArgumentCaptor.forClass(HttpRequest.class);verify(http).send(request.capture(),any(HttpResponse.BodyHandler.class));
        assertThat(request.getValue().method()).isEqualTo("POST");
        assertThat(request.getValue().headers().firstValue("x-upsert")).contains("false");
    }
    @Test void distinguishesMissingObjectFromMissingBucketAndAuthOutage() throws Exception {
        var http=mock(HttpClient.class);
        when(http.send(any(),any(HttpResponse.BodyHandler.class))).thenAnswer(call->response(404,new ByteArrayInputStream("{\"code\":\"NoSuchKey\"}".getBytes())));
        assertThat(storage(http).open(KEY)).isEmpty();
        when(http.send(any(),any(HttpResponse.BodyHandler.class))).thenAnswer(call->response(404,new ByteArrayInputStream("{\"code\":\"NoSuchBucket\"}".getBytes())));
        assertReason(()->storage(http).open(KEY),PHOTO_STORAGE_UNAVAILABLE);
        when(http.send(any(),any(HttpResponse.BodyHandler.class))).thenAnswer(call->response(403,new ByteArrayInputStream("{\"code\":\"NoSuchKey\"}".getBytes())));
        assertReason(()->storage(http).open(KEY),PHOTO_STORAGE_UNAVAILABLE);
    }
    @Test void missingDrainProofNeverClaimsDeletionEvenAfterExpiry() {
        assertThat(storage(mock(HttpClient.class)).uploadsDrained(KEY,CLOCK.instant().minusSeconds(100000))).isFalse();
        assertThatThrownBy(()->storage(mock(HttpClient.class)).open("../../other-bucket/secret")).isInstanceOf(PhotoFailure.class).hasMessage("PHOTO_STORAGE_UNAVAILABLE");
    }
}
