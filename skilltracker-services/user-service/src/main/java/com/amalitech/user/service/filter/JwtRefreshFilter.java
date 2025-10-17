package com.amalitech.user.service.filter;


import com.amalitech.user.service.model.RefreshToken;
import com.amalitech.user.service.model.User;
import com.amalitech.user.service.service.RefreshTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;


import java.io.IOException;

/**
 * Custom filter to handle refresh token validation from HttpOnly cookies for the /api/v1/auth/refresh endpoint.
 * Extracts the refresh token from the cookie, validates it, and sets the authentication context if valid.
 * If invalid or missing, returns a 401 Unauthorized response.
 */
@Component
public class JwtRefreshFilter extends OncePerRequestFilter {

    private final RefreshTokenService refreshTokenService;
    private final UserDetailsService userDetailsService;

    public JwtRefreshFilter(RefreshTokenService refreshTokenService, UserDetailsService userDetailsService) {
        this.refreshTokenService = refreshTokenService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (!"/api/v1/auth/refresh".equals(request.getServletPath())) {
            filterChain.doFilter(request, response);
            return;
        }

        String refreshTokenValue = getCookieValue(request, "refreshToken");
        if (refreshTokenValue == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Refresh token required");
            return;
        }

        try {
            RefreshToken rt = refreshTokenService.validateToken(refreshTokenValue);
            User user = rt.getUser();


            UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());

            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);

            filterChain.doFilter(request, response);
        } catch (Exception e) {
            SecurityContextHolder.clearContext();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid refresh token");
        }
    }

    /**
     * Extracts a cookie value by name from the request.
     *
     * @param request the HTTP request
     * @param cookieName the name of the cookie
     * @return the cookie value or null if not found
     */
    private String getCookieValue(HttpServletRequest request, String cookieName) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (cookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}