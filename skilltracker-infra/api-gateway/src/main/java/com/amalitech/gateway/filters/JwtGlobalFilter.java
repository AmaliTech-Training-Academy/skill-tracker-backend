package com.amalitech.gateway.filters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A global filter for JWT token validation and request enrichment in Spring Cloud Gateway.
 * This filter intercepts all incoming requests, validates JWT tokens for non-whitelisted paths,
 * and enriches requests with user information extracted from valid tokens.
 *
 * <p>The filter performs the following operations:
 * <ul>
 *   <li>Checks if the request path is whitelisted (public endpoints)</li>
 *   <li>Extracts JWT token from Authorization header, query param, or accessToken cookie</li>
 *   <li>Validates the token using the configured JWT decoder</li>
 *   <li>Enriches the request with user ID and roles from the token</li>
 *   <li>Returns UNAUTHORIZED for invalid or missing tokens on protected paths</li>
 * </ul>
 *
 * <p>Special handling for WebSocket connections (/ws):
 * <ul>
 *   <li>Requires authentication (token must be present)</li>
 *   <li>Enriches request with X-User-Id and X-User-Roles headers</li>
 *   <li>Allows downstream services to identify authenticated users</li>
 * </ul>
 *
 * <p>This filter runs with highest priority to ensure authentication happens before other filters.
 */
@Component
public class JwtGlobalFilter implements GlobalFilter, Ordered {
    private static final Logger log = LoggerFactory.getLogger(JwtGlobalFilter.class);

    private static final String ACCESS_TOKEN_COOKIE_NAME = "accessToken";

    private final ReactiveJwtDecoder jwtDecoder;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Value("${app.security.whitelist}")
    private List<String> whitelist;

    public JwtGlobalFilter(ReactiveJwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    /**
     * Filters incoming requests to validate JWT tokens and enrich requests with user information.
     *
     * <p>The filter logic:
     * <ol>
     *   <li>WebSocket paths (/ws/**): Always require authentication and enrich headers</li>
     *   <li>Whitelisted paths: Allow without authentication</li>
     *   <li>Protected paths: Require authentication</li>
     * </ol>
     *
     * @param exchange the current server web exchange
     * @param chain the gateway filter chain to delegate to
     * @return a {@link Mono} that completes when the filter chain completes
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        if (path.startsWith("/ws")) {
            return handleWebSocketRequest(exchange, chain, request, path);
        }

        if (isWhitelisted(path)) {
            log.debug("Path {} is whitelisted, skipping authentication", path);
            return chain.filter(exchange);
        }

        return handleProtectedRequest(exchange, chain, request, path);
    }

    /**
     * Handles WebSocket connection requests with required authentication.
     *
     * @param exchange the current server web exchange
     * @param chain the gateway filter chain
     * @param request the HTTP request
     * @param path the request path
     * @return a {@link Mono} that completes when processing is done
     */
    private Mono<Void> handleWebSocketRequest(ServerWebExchange exchange, GatewayFilterChain chain,
                                              ServerHttpRequest request, String path) {
        String token = extractToken(request);

        if (token == null) {
            log.warn("WebSocket connection attempt without token for path: {}", path);
            return unauthorized(exchange);
        }

        return this.jwtDecoder.decode(token)
                .flatMap(jwt -> {
                    ServerHttpRequest enrichedRequest = enrichRequest(request, jwt);
                    return chain.filter(exchange.mutate().request(enrichedRequest).build());
                })
                .onErrorResume(e -> {
                    log.error("WebSocket authentication failed for path {}: {}", path, e.getMessage());
                    return unauthorized(exchange);
                });
    }

    /**
     * Handles protected (non-whitelisted) requests that require authentication.
     *
     * @param exchange the current server web exchange
     * @param chain the gateway filter chain
     * @param request the HTTP request
     * @param path the request path
     * @return a {@link Mono} that completes when processing is done
     */
    private Mono<Void> handleProtectedRequest(ServerWebExchange exchange, GatewayFilterChain chain,
                                              ServerHttpRequest request, String path) {
        String token = extractToken(request);

        if (token == null) {
            log.warn("Missing Authorization header or '{}' cookie for non-whitelisted path: {}",
                    ACCESS_TOKEN_COOKIE_NAME, path);
            return unauthorized(exchange);
        }

        return this.jwtDecoder.decode(token)
                .flatMap(jwt -> {
                    ServerHttpRequest enrichedRequest = enrichRequest(request, jwt);
                    JwtAuthenticationToken authentication = createAuthentication(jwt);

                    return chain.filter(exchange.mutate().request(enrichedRequest).build())
                            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
                })
                .onErrorResume(e -> {
                    log.error("Invalid token for path {}: {}", path, e.getMessage());
                    return unauthorized(exchange);
                });
    }

    /**
     * Creates a JwtAuthenticationToken from the validated JWT.
     * This sets the Spring Security context so that downstream filters recognize the user as authenticated.
     *
     * Note: Authorities are created WITHOUT the "ROLE_" prefix to match the format expected by
     * downstream microservices that use HeaderAuthenticationFilter.
     *
     * @param jwt the validated JWT token
     * @return a {@link JwtAuthenticationToken} containing user authorities
     */
    private JwtAuthenticationToken createAuthentication(Jwt jwt) {
        List<String> rolesList = jwt.getClaimAsStringList("roles");

        List<SimpleGrantedAuthority> authorities;
        if (rolesList == null || rolesList.isEmpty()) {
            String role = jwt.getClaimAsString("roles");
            authorities = (role != null)
                    ? List.of(new SimpleGrantedAuthority(role))
                    : List.of();
        } else {
            authorities = rolesList.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        }

        return new JwtAuthenticationToken(jwt, authorities);
    }

    /**
     * Extracts the JWT token from the request in the following order:
     * 1. Authorization header (Bearer token)
     * 2. Query parameter (token=...)
     * 3. Cookie (accessToken)
     *
     * @param request the incoming HTTP request
     * @return the JWT token string, or {@code null} if not found
     */
    private String extractToken(ServerHttpRequest request) {
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        String queryToken = request.getQueryParams().getFirst("token");
        if (StringUtils.hasText(queryToken)) {
            log.debug("Token extracted from query parameter");
            return queryToken;
        }

        // Try cookie (for browser-based clients)
        HttpCookie accessTokenCookie = request.getCookies().getFirst(ACCESS_TOKEN_COOKIE_NAME);
        if (accessTokenCookie != null) {
            log.debug("Token extracted from cookie");
            return accessTokenCookie.getValue();
        }

        return null;
    }

    /**
     * Checks if the given request path matches any pattern in the whitelist.
     * Uses Ant-style pattern matching for flexible path configuration.
     *
     * @param path the request path to check
     * @return {@code true} if the path is whitelisted, {@code false} otherwise
     */
    private boolean isWhitelisted(String path) {
        return whitelist.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    /**
     * Enriches the request with user information extracted from the JWT token.
     * Adds the following headers to the request:
     * <ul>
     *   <li>X-User-Id: The user's unique identifier</li>
     *   <li>X-User-Roles: Comma-separated list of user roles</li>
     * </ul>
     *
     * @param request the original HTTP request
     * @param jwt the validated JWT token containing user claims
     * @return a new {@link ServerHttpRequest} with added user information headers
     */
    private ServerHttpRequest enrichRequest(ServerHttpRequest request, Jwt jwt) {
        String userId = jwt.getClaim("userId");
        if (userId == null) {
            userId = jwt.getId();
            log.warn("JWT is missing 'userId' claim, falling back to 'jti'. " +
                    "Ensure user-service is deployed with the latest JwtUtil.");
        }

        List<String> rolesList = jwt.getClaimAsStringList("roles");
        String rolesHeader;

        if (rolesList == null || rolesList.isEmpty()) {
            String role = jwt.getClaimAsString("roles");
            rolesHeader = (role != null) ? role : "";
        } else {
            rolesHeader = String.join(",", rolesList);
        }

        log.debug("Enriching request. X-User-Id: {}, X-User-Roles: {}", userId, rolesHeader);

        return request.mutate()
                .header("X-User-Id", userId)
                .header("X-User-Roles", rolesHeader)
                .build();
    }

    /**
     * Creates an UNAUTHORIZED (401) response for requests with invalid or missing tokens.
     *
     * @param exchange the current server web exchange
     * @return a {@link Mono} that completes when the response is set
     */
    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    /**
     * Returns the order of this filter in the filter chain.
     * A lower value indicates a higher priority.
     *
     * @return {@link Ordered#HIGHEST_PRECEDENCE}, ensuring this filter runs early in the filter chain
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}