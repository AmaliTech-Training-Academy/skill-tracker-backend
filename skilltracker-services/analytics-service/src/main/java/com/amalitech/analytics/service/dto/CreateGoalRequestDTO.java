package com.amalitech.analytics.service.dto;

import com.amalitech.analytics.service.model.enums.GoalType;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for creating a new goal.
 */
public record CreateGoalRequestDTO(
        @NotNull UUID skillId,
        @NotNull GoalType goalType,
        @NotNull @Min(1) Integer targetValue,
        @FutureOrPresent LocalDate deadline // Optional, can be null
) {}