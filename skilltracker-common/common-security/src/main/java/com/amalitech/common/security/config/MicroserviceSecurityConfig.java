package com.amalitech.common.security.config;

import com.amalitech.common.security.filter.HeaderAuthenticationFilter;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration for microservices in a distributed architecture.
 *
 * <p>This configuration is designed for services that sit behind an API Gateway
 * which handles JWT validation. It configures Spring Security to:
 * <ul>
 *   <li>Extract authentication from HTTP headers (X-User-Id, X-User-Roles)</li>
 *   <li>Operate in a stateless mode (no sessions)</li>
 *   <li>Enable method-level security annotations (@PreAuthorize, @PostAuthorize, etc.)</li>
 *   <li>Permit all requests at the HTTP level (authorization handled via method security)</li>
 * </ul>
 *
 * <p><strong>Architecture Context:</strong> In this setup, the API Gateway validates
 * JWT tokens and forwards user information via headers. Microservices trust these
 * headers to establish the security context for method-level authorization.
 *
 * <p><strong>Security Considerations:</strong>
 * <ul>
 *   <li>Services using this configuration must NOT be directly accessible from the internet</li>
 *   <li>All traffic should route through the trusted API Gateway</li>
 *   <li>Network-level security should prevent direct access to microservices</li>
 * </ul>
 *
 * <p>This configuration is only active for servlet-based web applications.
 *
 * @see HeaderAuthenticationFilter
 * @see EnableMethodSecurity
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class MicroserviceSecurityConfig {

    @Bean
    public HeaderAuthenticationFilter headerAuthenticationFilter() {
        return new HeaderAuthenticationFilter();
    }

    /**
     * Configures the Spring Security filter chain for microservice authentication.
     *
     * <p>This method sets up the following security configurations:
     * <ul>
     *   <li><strong>CSRF Protection:</strong> Disabled (stateless API with no browser sessions)</li>
     *   <li><strong>Session Management:</strong> Stateless (no HTTP sessions created)</li>
     *   <li><strong>Authorization:</strong> All requests permitted at HTTP level;
     *       actual authorization enforced via method security annotations</li>
     *   <li><strong>Custom Filter:</strong> Adds {@link HeaderAuthenticationFilter} before
     *       the standard username/password authentication filter</li>
     * </ul>
     *
     * <p><strong>Why permit all requests?</strong> This configuration delegates authorization
     * to method-level security annotations (@PreAuthorize, etc.) on service methods, providing
     * fine-grained control. The API Gateway ensures only authenticated requests reach the service.
     *
     * @param http the {@link HttpSecurity} to configure
     * @return the configured {@link SecurityFilterChain}
     * @throws Exception if an error occurs during configuration
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(authz -> authz
                        .anyRequest().permitAll()
                )
                .addFilterBefore(headerAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
