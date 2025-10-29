package com.amalitech.task.service.dto.events;

import java.io.Serializable;
import java.util.UUID;

public record SkillEventDTO(
        UUID id,
        String name,
        String description,
        String eventType // e.g., "SKILL_CREATED", "SKILL_UPDATED"
) implements Serializable {}