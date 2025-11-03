package com.amalitech.common.event.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SkillEvent {
    public enum EventType {
        SKILL_CREATED, SKILL_UPDATED, SKILL_DELETED,
        SKILL_COMPLETED, SKILL_STARTED, SKILL_LEVEL_UP
    }

    private EventType eventType;
    private UUID skillId;
    private String name, description, category, iconUrl;
    private Set<String> supportedTaskTypes;
    private Map<String, Long> levelXpMap;
}