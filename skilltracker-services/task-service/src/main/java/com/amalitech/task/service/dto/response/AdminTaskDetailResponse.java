package com.amalitech.task.service.dto.response;

import com.amalitech.task.service.model.content.TaskContent;
import java.time.LocalDateTime;
import java.util.UUID;

public record AdminTaskDetailResponse(
        UUID taskId,
        UUID taskDefinitionId,
        String title,
        String description,
        String skillName,
        String type,
        String difficulty,
        TaskContent content,
        int version,
        boolean isPublished,
        Integer estimatedDurationInMinutes,
        Integer xpReward,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}