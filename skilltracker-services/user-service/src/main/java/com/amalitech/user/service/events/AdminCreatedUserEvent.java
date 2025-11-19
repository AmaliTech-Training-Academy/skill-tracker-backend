package com.amalitech.user.service.events;

import java.util.UUID;

/**
 * An immutable event published when an admin successfully creates a new user.
 * This event contains all necessary data for the EmailService listener.
 */
public record AdminCreatedUserEvent(
        UUID userId,
        String email,
        String rawPassword,
        String adminEmail,
        String loginUrl
) {}
