package com.amalitech.task.service.dto.events;

import java.io.Serializable;
import java.util.UUID;

/**
 * An immutable Data Transfer Object (DTO) representing a user identity or profile event.
 * <p>
 * This record is used in the SkillBoost event-driven architecture to propagate user changes
 * (creation, updates) from the central User Service to consuming microservices. Its immutability
 * and implementation of {@code Serializable} make it suitable for reliable and safe
 * transmission over message queues (e.g., Kafka).
 *
 * @param id The unique identifier (UUID) of the user who is the subject of the event.
 * @param fullName The user's full name, useful for logging and displaying in analytics.
 * @param email The user's primary email address.
 * @param role The user's primary role or security clearance (e.g., "STUDENT", "ADMIN", "CORPORATE_MANAGER").
 * @param eventType The specific type of user event that occurred (e.g., "USER_CREATED", "USER_UPDATED", "USER_DELETED").
 */
public record UserEventDTO(
        UUID id,
        String fullName,
        String email,
        String role,
        String eventType // e.g., "USER_CREATED", "USER_UPDATED"
) implements Serializable {}