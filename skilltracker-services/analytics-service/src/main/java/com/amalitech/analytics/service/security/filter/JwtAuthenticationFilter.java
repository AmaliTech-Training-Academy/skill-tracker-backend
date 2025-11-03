package com.amalitech.analytics.service.security.filter;

import com.amalitech.analytics.service.security.util.JwtUtil;
import com.amalitech.analytics.service.util.CookieUtil;
import com.amalitech.analytics.service.util.RedisUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Filter that intercepts HTTP requests to validate JWT access tokens.
 * <p>
 * This filter checks:
 * <ul>
 *     <li>Presence of an Authorization header with a Bearer token</li>
 *     <li>Whether the token is blacklisted in Redis.</li>
 *     <li>Whether the token is valid and not expired</li>
 * </ul>
 * <p>
 * If validation succeeds, the user is authenticated in the Spring Security context
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final RedisUtil redisUtil;
    private final JwtUtil jwtUtil;
    private final CookieUtil cookieUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil,
                                   RedisUtil redisUtil,
                                   CookieUtil cookieUtil) {
        this.redisUtil = redisUtil;
        this.jwtUtil = jwtUtil;
        this.cookieUtil = cookieUtil;
    }

    /**
     * Filters each incoming HTTP request to authenticate the user via JWT.
     *
     * <p>Steps performed:</p>
     * <ol>
     *     <li>Extracts the Authorization header and token</li>
     *     <li>Checks if the token is blacklisted in Redis</li>
     *     <li>Validates the JWT token and extracts user information</li>
     *     <li>Populates Spring Security context with authenticated user</li>
     * </ol>
     *
     * @param request the HTTP request
     * @param response the HTTP response
     * @param filterChain the filter chain
     * @throws ServletException if a servlet exception occurs
     * @throws IOException if an I/O exception occurs
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = cookieUtil.getCookieValue(request, "accessToken");

        if (token == null || token.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        System.out.printf("JWT token found in cookie (masked): {}", maskToken(token));

        if (log.isDebugEnabled()) {
            log.debug("JWT Token received from cookie (masked): {}", maskToken(token));
        }

        if (redisUtil.exists("blacklist:" + token)) {
            log.warn("Access token is blacklisted");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Access token revoked");
            return;
        }

        try {
            if (jwtUtil.isValid(token)) {
                String email = jwtUtil.extractEmail(token);
                List<String> roles = jwtUtil.extractRoles(token);
                System.out.printf("Token valid for user: [%s] with roles: %s%n", email, roles);
                List<GrantedAuthority> authorities = roles.stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        email, null, authorities);

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
                log.debug("Authenticated user: {}", email);
            }
        } catch (Exception e) {
            log.error("JWT Authentication failed for token (masked): {}", maskToken(token), e);
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Masks a token for logging purposes to avoid printing the full value.
     *
     * @param token the JWT token
     * @return a masked version of the token showing only the first and last 10 characters
     */
    private String maskToken(String token) {
        if (token.length() <= 20) return token;
        return token.substring(0, 10) + "..." + token.substring(token.length() - 10);
    }
}