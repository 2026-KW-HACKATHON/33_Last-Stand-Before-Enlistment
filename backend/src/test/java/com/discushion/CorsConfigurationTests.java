package com.discushion;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.junit.jupiter.api.Assertions.*;

class CorsConfigurationTests {
    @Test void preflightAllowsExactOriginAndAllFeatureHeadersWithoutAuthentication() throws Exception {
        var request = new MockHttpServletRequest("OPTIONS", "/api/v1/posts/1");
        request.setServletPath("/api/v1/posts/1");
        request.addHeader("Origin", "https://fe.example.test");
        request.addHeader("Access-Control-Request-Method", "PATCH");
        request.addHeader("Access-Control-Request-Headers", "authorization,content-type,x-post-share-token");
        var response = new MockHttpServletResponse();
        var called = new AtomicBoolean(false);
        CorsConfiguration.filter("https://fe.example.test,http://localhost:3000").doFilter(request, response, (a,b) -> called.set(true));
        assertEquals(200, response.getStatus());
        assertEquals("https://fe.example.test", response.getHeader("Access-Control-Allow-Origin"));
        assertNull(response.getHeader("Access-Control-Allow-Credentials"));
        assertTrue(response.getHeader("Access-Control-Allow-Headers").toLowerCase().contains("x-post-share-token"));
        assertFalse(called.get());
    }
    @Test void rejectsUnknownOriginAndEmptyConfiguration() throws Exception {
        for (var configured : new String[]{"", "https://fe.example.test"}) {
            var request = new MockHttpServletRequest("GET", "/api/v1/regions");
            request.setServletPath("/api/v1/regions"); request.addHeader("Origin", "https://evil.example.test");
            var response = new MockHttpServletResponse();
            CorsConfiguration.filter(configured).doFilter(request,response,(a,b) -> fail("Rejected request reached feature"));
            assertEquals(403,response.getStatus()); assertNull(response.getHeader("Access-Control-Allow-Origin"));
        }
    }
    @Test void actualResponsePreservesCorsIncludingFeatureErrors() throws Exception {
        var request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setServletPath("/api/v1/auth/login"); request.addHeader("Origin", "https://fe.example.test");
        var response = new MockHttpServletResponse();
        CorsConfiguration.filter("https://fe.example.test").doFilter(request,response,(a,b) -> response.setStatus(401));
        assertEquals(401,response.getStatus()); assertEquals("https://fe.example.test",response.getHeader("Access-Control-Allow-Origin"));
    }
    @Test void rejectsWildcardExternalHttpPathsAndCredentials() {
        for (var value : new String[]{"*", "https://*.example.test", "http://fe.example.test", "https://fe.example.test/", "https://fe.example.test/x", "https://user@fe.example.test", "https://fe.example.test?x=1", "https://fe.example.test,"}) {
            assertThrows(IllegalArgumentException.class,() -> CorsConfiguration.origins(value));
        }
    }
}
