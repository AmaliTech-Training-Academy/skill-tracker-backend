package com.amalitech.common.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Custom Spring Security Filter for handling delegated authentication in a microservices environment.
 * <p>
 * This filter operates under the assumption that an upstream service (e.g., an API Gateway)
 * has already validated the user's token (JWT/OAuth2) and propagated the essential user identity
 * and authorization data via custom HTTP headers. It extracts the 'X-User-Id' and 'X-User-Roles'
 * headers and constructs a stateless {@link Authentication} object to populate the
 * {@link SecurityContextHolder}.
 * <p>
 * NOTE: This mechanism is inherently vulnerable to header forgery if the service is not
 * strictly protected via internal network policies (Kubernetes NetworkPolicy, etc.).
 * @see org.springframework.web.filter.OncePerRequestFilter
 */
@Component
public class HeaderAuthenticationFilter extends OncePerRequestFilter {


    /**
     * * By default, OncePerRequestFilter skips "ASYNC" dispatches (which happen when
     * a Mono/Flux completes and the response is written).
     * * We return 'false' to ensure this filter runs AGAIN during the dispatch phase,
     * re-populating the SecurityContext from the headers so the response can be written
     * without a 403 Access Denied error.
     */
    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }
    /**
     * Performs the internal filtering logic, executed once per request.
     * <p>
     * It extracts the user ID and roles from custom headers. If a user ID is present,
     * it splits the roles string (comma-separated), converts them into Spring
     * authorities, and sets the resulting stateless {@link Authentication} object
     * onto the current request's Security Context.
     *
     * @param request The servlet request we are processing.
     * @param response The servlet response we are creating.
     * @param filterChain The filter chain we are processing.
     * @throws ServletException If an exception occurs that interferes with the filter's normal operation.
     * @throws IOException If an input or output exception occurs.
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
                        .map(role -> new SimpleGrantedAuthority(role.trim()))
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
