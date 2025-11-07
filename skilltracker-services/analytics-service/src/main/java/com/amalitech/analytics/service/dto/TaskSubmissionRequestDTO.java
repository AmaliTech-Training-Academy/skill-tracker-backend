package com.amalitech.analytics.service.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.amalitech.analytics.service.model.enums.TaskType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;

public record TaskSubmissionRequestDTO(
        @NotNull UUID userId,
        @NotNull UUID skillId,
        @NotNull String taskId,
        @NotNull String taskDescription,
        @NotNull int totalXpEarned,
        @NotNull Boolean passed,
        @NotNull TaskType taskType,
        @NotNull String taskDifficulty,
        Map<String, Integer> rubricsScores,
        @NotNull Instant completedAt
) {

    public TaskCompletedEvent toEvent() {
        return new TaskCompletedEvent(
                userId,
                skillId,
                taskId,
                taskDescription,
                totalXpEarned,
                passed,
                completedAt != null ? completedAt : Instant.now(),
                taskType,
                taskDifficulty,
                rubricsScores
        );
    }
}
