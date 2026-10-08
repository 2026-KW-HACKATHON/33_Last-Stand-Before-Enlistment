package com.discushion;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/** Exact configured FE origins. No wildcard origins, credential cookies or preview-domain patterns. */
@Configuration(proxyBeanMethods = false)
public class CorsConfiguration {
    static List<String> origins(String configured) {
        if (configured == null || configured.isBlank()) return List.of();
        return Arrays.stream(configured.split(",", -1)).map(String::trim).map(value -> {
            URI uri;
            try { uri = URI.create(value); } catch (IllegalArgumentException failure) { throw new IllegalArgumentException("Invalid CORS origin"); }
            boolean https = "https".equals(uri.getScheme());
            boolean local = "http".equals(uri.getScheme()) && List.of("localhost", "127.0.0.1", "[::1]").contains(uri.getHost());
            if ((!https && !local) || uri.getHost() == null || uri.getRawUserInfo() != null
                    || uri.getRawQuery() != null || uri.getRawFragment() != null || !uri.getRawPath().isEmpty()
                    || value.contains("*") || uri.getPort() == 0 || uri.getPort() < -1 || uri.getPort() > 65535) {
                throw new IllegalArgumentException("CORS requires exact HTTPS origins or explicit local development origins");
            }
            return value;
        }).distinct().toList();
    }

    static CorsFilter filter(String configured) {
        var policy = new org.springframework.web.cors.CorsConfiguration();
        policy.setAllowedOrigins(origins(configured));
        policy.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        policy.setAllowedHeaders(List.of("Accept", "Authorization", "Content-Type", "X-Post-Share-Token"));
        policy.setExposedHeaders(List.of("Retry-After", "WWW-Authenticate"));
        policy.setAllowCredentials(false);
        policy.setMaxAge(3600L);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/v1/**", policy);
        return new CorsFilter(source);
    }

    @Bean
    FilterRegistrationBean<CorsFilter> corsFilter(Environment environment) {
        var registration = new FilterRegistrationBean<>(filter(environment.getProperty("CORS_ALLOWED_ORIGINS", "")));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
