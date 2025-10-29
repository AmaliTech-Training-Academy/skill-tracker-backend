package com.amalitech.task.service.config;

import com.amalitech.common.security.filter.HeaderAuthenticationFilter;

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
 * Global security configuration for the SkillBoost Task Service microservice.
 * <p>
 * This configuration enables Spring Security, sets up method-level security,
 * and defines the security filter chain for stateless API access. It is
 * designed to work in conjunction with a token-based authentication mechanism,
 * typically where an API Gateway has already validated a token or a custom
 * filter is used for token inspection/authentication in a microservices' environment.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * Defines the custom authentication filter responsible for extracting
     * and validating the token (e.g., JWT) from the request headers.
     * <p>
     * This registers {@code HeaderAuthenticationFilter} as a Spring bean, resolving
     * the dependency injection error.
     *
     * @return The HeaderAuthenticationFilter bean.
     */
    @Bean
    public HeaderAuthenticationFilter headerAuthenticationFilter() {
        return new HeaderAuthenticationFilter();
    }

    /**
     * Defines the main security filter chain for HTTP requests.
     * <p>
     * It configures the service as stateless, disables CSRF, and enforces
     * token-based authentication for all core endpoints. The {@code HeaderAuthenticationFilter}
     * is inserted early in the chain to authenticate the user before standard
     * filters run.
     *
     * @param http The HttpSecurity object to configure.
     * @param headerAuthenticationFilter The filter bean automatically injected by Spring.
     * @return The configured SecurityFilterChain.
     * @throws Exception if configuration errors occur.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            HeaderAuthenticationFilter headerAuthenticationFilter
    ) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**", "/api/test/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(
                        headerAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                )
                .build();
    }
}