package com.amalitech.analytics.service.dto;
import com.amalitech.analytics.service.model.UserGoal;
import com.amalitech.analytics.service.model.enums.GoalStatus;
import com.amalitech.analytics.service.model.enums.GoalType;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;


/**
 * DTO for the complete goal object, returned by the CRUD API.
 */
public record UserGoalDTO(
        UUID id,
        UUID userId,
        UUID skillId,
        String skillName,
        GoalType goalType,
        int targetValue,
        int initialValue,
        int currentValue,
        GoalStatus status,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate deadline,
        Instant createdAt,
        Instant completedAt
) {
    public static UserGoalDTO fromEntity(UserGoal goal) {
        return new UserGoalDTO(
                goal.getId(),
                goal.getUserId(),
                goal.getSkillId(),
                goal.getSkillName(),
                goal.getGoalType(),
                goal.getTargetValue(),
                goal.getInitialValue(),
                goal.getCurrentValue(),
                goal.getStatus(),
                goal.getDeadline(),
                goal.getCreatedAt(),
                goal.getCompletedAt()
        );
    }
}
