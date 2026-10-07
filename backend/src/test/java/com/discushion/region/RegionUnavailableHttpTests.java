package com.discushion.region;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import static org.assertj.core.api.Assertions.*;

/** Default local profile still serves Health; an absent DB is never presented as an empty successful catalog. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties={"spring.profiles.active=local", "spring.config.import="})
class RegionUnavailableHttpTests {
    @LocalServerPort int port;
    private HttpResponse<String> get(String path) throws Exception {
        return HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path))
            .timeout(java.time.Duration.ofSeconds(5)).GET().build(),HttpResponse.BodyHandlers.ofString());
    }

    @Test void missingDatabaseKeepsHealthAndReturnsMaskedInternalError() throws Exception {
        assertThat(get("/health").statusCode()).isEqualTo(200);
        var response=get("/api/v1/regions");
        assertThat(response.statusCode()).isEqualTo(500);
        assertThat(response.body()).contains("INTERNAL_ERROR","details","traceId")
            .doesNotContain("DataSource","Region DataSource","localhost","postgres","password","stackTrace");
    }

    @Test void invalidInputIsStillValidationErrorBeforeDatabaseAccess() throws Exception {
        var response=get("/api/v1/regions?size=101");
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("VALIDATION_ERROR","size","traceId").doesNotContain("INTERNAL_ERROR");
    }
}
