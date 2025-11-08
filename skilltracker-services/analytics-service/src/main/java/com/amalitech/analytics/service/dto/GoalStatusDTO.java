package com.amalitech.analytics.service.dto;

import com.amalitech.analytics.service.model.enums.GoalStatus;
import com.amalitech.analytics.service.model.enums.GoalType;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for displaying goal status on the dashboard.
 */
public record GoalStatusDTO(
        UUID goalId,
        String description,
        GoalType type,
        int currentValue,
        int targetValue,
        int initialValue,
        double progressPercentage,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate deadline,
        String status
) {}