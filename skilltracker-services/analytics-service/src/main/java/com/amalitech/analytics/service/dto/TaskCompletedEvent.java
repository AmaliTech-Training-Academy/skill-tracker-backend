package com.amalitech.analytics.service.dto;

import com.amalitech.analytics.service.model.enums.TaskType;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 Represents an event when a user completes a task.
 */
public record TaskCompletedEvent(
        UUID userId,
        UUID skillId,
        String taskId,
        String taskDescription,
        Integer totalXpEarned,
        Boolean passed,
        Instant completedAt,
        TaskType taskType,
        String taskDifficulty,
        Map<String, Integer> rubricsScores
) {}
