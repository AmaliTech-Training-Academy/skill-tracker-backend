package com.amalitech.notification.service.util;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Centralized utility for handling security-related principal extractions.
 */
@Component
public class SecurityUtils {

    /**
     * Extracts the user's UUID from the Authentication principal.
     * WARNING: This implementation is still fragile and assumes the principal
     * is a String UUID. A better long-term solution would be a custom
     * UserDetails object or JWT principal.
     */
    public UUID extractUserIdFromAuth(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalArgumentException("Authentication principal is null.");
        }

        String userIdStr = (String) authentication.getPrincipal();

        try {
            return UUID.fromString(userIdStr);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Authentication principal is not a valid UUID string.", e);
        }
    }
}