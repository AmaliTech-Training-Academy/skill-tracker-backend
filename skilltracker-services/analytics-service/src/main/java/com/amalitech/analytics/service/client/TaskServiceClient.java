package com.amalitech.analytics.service.client;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TaskServiceClient {
    public String getSkillName(UUID skillId) {
        return "Skill " + skillId;
    }
}
