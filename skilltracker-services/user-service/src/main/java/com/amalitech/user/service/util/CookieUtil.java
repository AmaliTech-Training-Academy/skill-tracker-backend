package com.amalitech.user.service.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CookieUtil {
    @Value("${cookie.domain}")
    private String cookieDomain;

    @Value("${cookie.secure:true}")
    private boolean cookieSecure;

    @Value("${cookie.same-site:Strict}")
    private String sameSite;

    @Value("${cookie.path:/}")
    private String cookiePath;

    /**
     * Retrieves the value of a cookie by name from the request.
     * @param request The HTTP request containing the cookies.
     * @param name The name of the cookie to retrieve.
     * @return The cookie value if found, or null if not present.
     */
    public String getCookieValue(HttpServletRequest request, String name) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (name.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    /**
     * Sets a secure, HttpOnly cookie in the response.
     * @param response The HTTP response to add the Set-Cookie header to.
     * @param name Cookie name (e.g., "accessToken").
     * @param value Cookie value (e.g., JWT string).
     * @param maxAgeSeconds Expiration in seconds (align with token TTL).
     */
    public void setSecureCookie(HttpServletResponse response, String name, String value, long maxAgeSeconds) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("Cookie value cannot be null or empty");
        }

        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(sameSite)
                .path(cookiePath)
                .domain(cookieDomain)
                .maxAge(maxAgeSeconds)
                .build();

        response.addHeader("Set-Cookie", cookie.toString());
    }

    /**
     * Clears a cookie by setting maxAge=0 (expires immediately).
     */
    public void clearCookie(HttpServletResponse response, String name) {
        ResponseCookie cookie = ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(sameSite)
                .path(cookiePath)
                .domain(cookieDomain)
                .maxAge(0)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }
}
