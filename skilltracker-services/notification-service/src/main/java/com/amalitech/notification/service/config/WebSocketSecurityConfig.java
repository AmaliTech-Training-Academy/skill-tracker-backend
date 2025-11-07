package com.amalitech.notification.service.config;

import com.amalitech.common.security.filter.HeaderAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configures the security settings for the WebSocket service.
 * This class defines the security filter chain for HTTP requests, including the
 * initial WebSocket handshake.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class WebSocketSecurityConfig {


    @Bean
    public HeaderAuthenticationFilter headerAuthenticationFilter() {
        return new HeaderAuthenticationFilter();
    }

    /**
     * Defines the security filter chain for the application.
     * It configures CSRF, session management, and request authorization.
     * The custom HeaderAuthenticationFilter is added to the chain to handle
     * authentication based on headers propagated by the API gateway.
     * @param http The HttpSecurity object to configure.
     * @return The configured SecurityFilterChain.
     * @throws Exception If an error occurs during configuration.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   HeaderAuthenticationFilter headerAuthenticationFilter) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/ws/**").authenticated()
                        .requestMatchers("/actuator/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(headerAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Provides a no-op AuthenticationManager to prevent Spring Security
     * from auto-configuring an in-memory user with generated password.
     *
     * <p>This service uses HeaderAuthenticationFilter for authentication,
     * not traditional AuthenticationManager-based authentication.</p>
     *
     * @return A pass-through authentication manager
     */
    @Bean
    public AuthenticationManager authenticationManager() {
        return authentication -> authentication;
    }
}
