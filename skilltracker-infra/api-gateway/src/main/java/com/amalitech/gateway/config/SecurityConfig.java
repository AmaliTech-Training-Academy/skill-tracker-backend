package com.amalitech.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.reactive.CorsConfigurationSource;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Security configuration for the API Gateway with reactive WebFlux support.
 * 
 * Configures:
 * <ul>
 *   <li>JWT validation using HS256 algorithm</li>
 *   <li>OAuth2 resource server with JWT bearer tokens</li>
 *   <li>CORS handling integrated with global CORS configuration</li>
 *   <li>CSRF protection disabled (stateless API)</li>
 *   <li>HTTP Basic and Form Login disabled (JWT-based authentication only)</li>
 * </ul>
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {
    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private final JwtGatewayConfig jwtConfig;

    public SecurityConfig(JwtGatewayConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
    }

    /**
     * Configures the security filter chain for WebFlux with JWT and CORS support.
     * 
     * <p>Applies the following security policies:
     * <ul>
     *   <li>CSRF protection disabled (stateless API architecture)</li>
     *   <li>HTTP Basic authentication disabled</li>
     *   <li>Form-based login disabled</li>
     *   <li>CORS configured from the provided {@link CorsConfigurationSource}</li>
     *   <li>All requests permitted at the exchange level (JWT validation happens in filters)</li>
     *   <li>OAuth2 resource server enabled with JWT decoder</li>
     * </ul>
     *
     * @param http the {@link ServerHttpSecurity} to configure
     * @param corsConfigurationSource the CORS configuration source to apply
     * @return the configured {@link SecurityWebFilterChain}
     */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http, CorsConfigurationSource corsConfigurationSource) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .authorizeExchange(exchange -> exchange
                        .anyExchange().permitAll()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtDecoder(jwtDecoder()))
                )
                .build();
    }

    /**
     * Creates a reactive JWT decoder for validating JWT tokens using HS256 algorithm.
     * 
     * <p>The decoder uses the JWT secret from {@link JwtGatewayConfig} and validates
     * tokens with HMAC SHA-256 signature verification. The secret must be at least
     * 32 characters long as enforced by {@link JwtGatewayConfig#getSecret()}.
     *
     * @return a configured {@link ReactiveJwtDecoder} for JWT validation
     * @throws IllegalStateException if JWT secret is invalid or too short
     */
    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        log.info("Creating ReactiveJwtDecoder bean for JWT validation...");

        String secret = jwtConfig.getSecret();
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "HmacSHA256");

        return NimbusReactiveJwtDecoder.withSecretKey(secretKeySpec)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }
}
