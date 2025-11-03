package com.amalitech.user.service.dto.request;

import com.amalitech.user.service.model.enums.DifficultyLevel;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SkillSelection(
        @NotNull
        UUID skillId,
        @NotNull
        DifficultyLevel level
) {}
