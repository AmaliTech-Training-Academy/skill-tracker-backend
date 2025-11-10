package com.amalitech.analytics.service.dto;

import jakarta.validation.constraints.NotNull;

public record RubricScoreDTO(
        @NotNull Integer score,
        @NotNull Integer maxScore,
        @NotNull Integer percentage)
{}
