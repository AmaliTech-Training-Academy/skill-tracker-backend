package com.amalitech.user.service.security.util;

import com.amalitech.user.service.model.enums.Role;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-expiration-ms}")
    private long expiration;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * Generates a signed JWT access token with email, user ID, and roles.
     *
     * @param email the user's email
     * @param role the user's role
     * @param id the user's unique identifier
     * @return a signed JWT access token
     */
    public String generateAccessToken(String email, Role role, UUID id) {
        return Jwts.builder()
                .setSubject(email)
                .setId(id.toString())
                .claim("userId", id.toString())
                .claim("roles", List.of(role.name()))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extracts the email (subject) from the given JWT token.
     *
     * @param token the JWT token
     * @return the email contained in the token
     */
    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }


    /**
     * Extracts user roles from the given JWT token.
     *
     * @param token the JWT token
     * @return a list of roles assigned to the user
     */
    public List<String> extractRoles(String token) {
        return parseClaims(token).get("roles", List.class);
    }


    /**
     * Parses and validates the claims from a JWT token.
     *
     * @param token the JWT token
     * @return the claims contained within the token
     */
    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Retrieves the expiration date from a JWT token.
     *
     * @param token the JWT token
     * @return the expiration date
     */
    public Date getExpirationDate(String token) {
        return parseClaims(token).getExpiration();
    }

    /**
     * Calculates how many seconds remain until the JWT token expires.
     * Useful for setting TTL in Redis for blacklisted tokens.
     *
     * @param token the JWT token
     * @return remaining time in seconds before expiration
     */
    public long getExpirationSeconds(String token) {
        Date expirationDate = getExpirationDate(token);
        long remaining = (expirationDate.getTime() - System.currentTimeMillis()) / 1000;
        return Math.max(remaining, 0);
    }

    /**
     * Validates a JWT token by ensuring its signature and expiration are valid.
     *
     * @param token the JWT token
     * @return true if valid, false otherwise
     */
    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}