package com.amalitech.task.service.dto.events;

import java.io.Serializable;
import java.util.UUID;

public record UserEventDTO(
        UUID id,
        String fullName,
        String email,
        String role,
        String eventType // e.g., "USER_CREATED", "USER_UPDATED"
) implements Serializable {}
