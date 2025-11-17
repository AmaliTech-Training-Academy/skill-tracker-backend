package com.amalitech.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * CORS configuration for the API Gateway.
 * Enables cross-origin requests with configurable allowed origins, methods, and headers.
 *
 * Configuration is sourced from the externalized properties:
 * - spring.cloud.gateway.server.webflux.globalcors.cors-configurations
 *
 * This bean ensures CORS headers are properly set on all gateway responses,
 * including preflight (OPTIONS) requests.
 */
@Configuration
public class CorsConfig {

    @Value("${spring.cloud.gateway.server.webflux.globalcors.cors-configurations.[/**].allowed-origins:}")
    private String allowedOrigins;

    @Value("${spring.cloud.gateway.server.webflux.globalcors.cors-configurations.[/**].allowed-methods:GET,POST,PUT,DELETE,OPTIONS,PATCH}")
    private String allowedMethods;

    @Value("${spring.cloud.gateway.server.webflux.globalcors.cors-configurations.[/**].allowed-headers:*}")
    private String allowedHeaders;

    @Value("${spring.cloud.gateway.server.webflux.globalcors.cors-configurations.[/**].allow-credentials:true}")
    private boolean allowCredentials;

    @Value("${spring.cloud.gateway.server.webflux.globalcors.cors-configurations.[/**].max-age:3600}")
    private long maxAge;

    /**
     * Creates a CorsConfigurationSource bean that applies CORS configuration
     * to all paths matching /** pattern.
     *
     * @return a {@link CorsConfigurationSource} configured with the gateway's CORS settings
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfig = new CorsConfiguration();

        if (allowedOrigins != null && !allowedOrigins.isEmpty()) {
            List<String> origins = Arrays.asList(allowedOrigins.split(","));
            corsConfig.setAllowedOrigins(origins);
        }

        if (allowedMethods != null && !allowedMethods.isEmpty()) {
            List<String> methods = Arrays.asList(allowedMethods.split(","));
            corsConfig.setAllowedMethods(methods);
        }

        if (allowedHeaders != null && !allowedHeaders.isEmpty() && !allowedHeaders.equals("*")) {
            List<String> headers = Arrays.asList(allowedHeaders.split(","));
            corsConfig.setAllowedHeaders(headers);
        } else {
            corsConfig.addAllowedHeader("*");
        }

        corsConfig.setAllowCredentials(allowCredentials);
        corsConfig.setMaxAge(maxAge);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return source;
    }
}
