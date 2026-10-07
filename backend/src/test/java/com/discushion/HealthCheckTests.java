package com.discushion;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.profiles.active=local", "spring.config.import="})
class HealthCheckTests {

    @LocalServerPort
    private int port;

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();

    @Test
    void anonymousRequestReturnsTheHealthContractWithoutADatabase() throws Exception {
        var response = client.send(request("/health").GET().build(), HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type")).hasValue("application/json");
        assertThat(response.body()).isEqualTo("{\"data\":{\"status\":\"UP\"}}");
    }

    @Test
    void unsupportedHealthMethodIsRejected() throws Exception {
        var response = client.send(request("/health").POST(HttpRequest.BodyPublishers.noBody()).build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(405);
        assertThat(response.body()).doesNotContain("stackTrace", "DataSourceAutoConfiguration");
    }

    @Test
    void unknownEndpointIsNotReportedAsHealthy() throws Exception {
        var response = client.send(request("/unknown").GET().build(), HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.body()).doesNotContain("\"status\":\"UP\"");
    }

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .timeout(Duration.ofSeconds(5));
    }
}
