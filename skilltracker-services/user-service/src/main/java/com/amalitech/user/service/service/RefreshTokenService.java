package com.amalitech.user.service.service;


import com.amalitech.user.service.model.RefreshToken;
import com.amalitech.user.service.model.User;
import com.amalitech.user.service.repository.RefreshTokenRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

@Service
public class RefreshTokenService {
    private final RefreshTokenRepository repository;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository repository) {
        this.repository = repository;
    }

    /**
     * Creates a new refresh token for the user and returns the plain token.
     *
     * @param user the user to associate the token with
     * @return the plain refresh token
     */
    public String createToken(User user) {
        String plainToken = generateRandomToken();
        RefreshToken rt = new RefreshToken();
        rt.setUser(user);
        rt.setToken(hashToken(plainToken));
        rt.setExpiresAt(Instant.now().plusSeconds(604800)); // 7 days
        rt.setRevoked(false);
        repository.save(rt);
        return plainToken;
    }

    /**
     * Validates the provided plain refresh token by hashing it and checking against stored tokens.
     * Ensures the token is not revoked or expired.
     *
     * @param plainToken the plain refresh token from the client
     * @return the valid RefreshToken entity
     * @throws RuntimeException if token is invalid, revoked, or expired
     */
    public RefreshToken validateToken(String plainToken) {
        String hashedToken = hashToken(plainToken);
        RefreshToken rt = repository.findByToken(hashedToken)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));
        if (rt.isRevoked() || rt.getExpiresAt().isBefore(Instant.now())) {
            throw new RuntimeException("Invalid refresh token");
        }
        return rt;
    }

    /**
     * Rotates the refresh token: revokes the current one, creates a new one, and returns the new plain token.
     *
     * @param rt the current valid RefreshToken to rotate
     * @return the new plain refresh token
     */
    public String rotateToken(RefreshToken rt) {
        // Revoke the old token
        rt.setRevoked(true);
        rt.setRotatedAt(Instant.now());
        repository.save(rt);

        // Create a new token
        String newPlainToken = generateRandomToken();
        RefreshToken newRt = new RefreshToken();
        newRt.setUser(rt.getUser());
        newRt.setToken(hashToken(newPlainToken));
        newRt.setExpiresAt(Instant.now().plusSeconds(604800)); // 7 days
        newRt.setRevoked(false);
        repository.save(newRt);

        return newPlainToken;
    }

    /**
     * Revokes the specified refresh token.
     *
     * @param plainToken the plain refresh token to revoke
     */
    public void revokeToken(String plainToken) {
        String hashedToken = hashToken(plainToken);
        RefreshToken rt = repository.findByToken(hashedToken)
                .orElseThrow(() -> new RuntimeException("Refresh token not found"));
        rt.setRevoked(true);
        repository.save(rt);
    }

    /**
     * Revokes all refresh tokens for the given user.
     *
     * @param user the user whose tokens to revoke
     */
    public void revokeAllForUser(User user) {
        List<RefreshToken> tokens = repository.findByUser(user);
        tokens.forEach(token -> token.setRevoked(true));
        repository.saveAll(tokens);
    }

    /**
     * Generates a cryptographically secure random token string.
     *
     * @return a base64url-encoded random string
     */
    private String generateRandomToken() {
        byte[] randomBytes = new byte[64];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    /**
     * Hashes the plain token using SHA-256 for secure storage and comparison.
     *
     * @param plainToken the plain token to hash
     * @return the base64url-encoded SHA-256 hash
     */
    private String hashToken(String plainToken) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = md.digest(plainToken.getBytes("UTF-8"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashedBytes);
        } catch (NoSuchAlgorithmException | java.io.UnsupportedEncodingException e) {
            throw new RuntimeException("Failed to hash token", e);
        }
    }
}