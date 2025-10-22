package com.amalitech.common.event.dto;

import java.util.UUID;

public record SkillEventDto(
        EventType eventType,
        UUID id,
        String name,
        String description
) {
    public enum EventType {
        CREATED,
        UPDATED,
        DELETED
    }
}
