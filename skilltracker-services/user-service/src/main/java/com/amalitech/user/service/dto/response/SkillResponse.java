package com.amalitech.user.service.dto.response;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * DTO for returning skill information to the client.
 */
public record SkillResponse(
        UUID id,
        String name,
        String description,
        String category,
        String iconUrl,
        Set<String> supportedTaskTypes,
        Map<String, Long> levelXpMap
) implements Serializable {}