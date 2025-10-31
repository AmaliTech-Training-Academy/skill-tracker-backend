package com.amalitech.user.service.dto.request;

import com.amalitech.user.service.model.enums.DifficultyLevel;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record OnboardingRequest(
        @NotEmpty
        List<SkillSelectionDto> skills
) {}

