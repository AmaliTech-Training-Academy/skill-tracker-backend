package com.amalitech.common.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.Authentication;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A servlet filter that extracts user authentication information from HTTP headers
 * and populates the Spring Security context.
 *
 * <p>This filter is designed for microservice architectures where the API Gateway
 * handles JWT validation and forwards authenticated user information via headers.
 * It reads the following headers:
 * <ul>
 *   <li><strong>X-User-Id</strong>: The authenticated user's unique identifier</li>
 *   <li><strong>X-User-Roles</strong>: Comma-separated list of user roles</li>
 * </ul>
 *
 * <p>The filter creates a {@link UsernamePasswordAuthenticationToken} and sets it
 * in the {@link SecurityContextHolder}, making the user information available to
 * downstream security checks and business logic.
 *
 * <p><strong>Security Note:</strong> This filter should only be used in services
 * behind a trusted API Gateway that validates JWTs. Direct exposure to external
 * requests would be a security vulnerability as headers can be easily spoofed.
 *
 * @see OncePerRequestFilter
 * @see UsernamePasswordAuthenticationToken
 */
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    /**
     * Processes each HTTP request to extract authentication information from headers
     * and populate the Spring Security context.
     *
     * <p>The filter performs the following operations:
     * <ol>
     *   <li>Extracts the user ID from the X-User-Id header</li>
     *   <li>Extracts and parses roles from the X-User-Roles header (comma-separated)</li>
     *   <li>Creates an {@link Authentication} object with the extracted information</li>
     *   <li>Sets the authentication in the {@link SecurityContextHolder}</li>
     *   <li>Proceeds with the filter chain</li>
     * </ol>
     *
     * <p>If the X-User-Id header is missing or empty, no authentication is set,
     * and the request proceeds unauthenticated.
     *
     * @param request the HTTP request containing potential authentication headers
     * @param response the HTTP response
     * @param filterChain the filter chain to continue processing the request
     * @throws ServletException if an error occurs during request processing
     * @throws IOException if an I/O error occurs during request processing
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String userId = request.getHeader("X-User-Id");
        String rolesHeader = request.getHeader("X-User-Roles");

        if (userId != null && !userId.isEmpty()) {
            List<SimpleGrantedAuthority> authorities;
            if (rolesHeader != null && !rolesHeader.isEmpty()) {
                authorities = Arrays.stream(rolesHeader.split(","))
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());
            } else {
                authorities = Collections.emptyList();
            }

            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userId, null, authorities
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}
