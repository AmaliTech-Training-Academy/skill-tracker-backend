package com.amalitech.user.service.dto.request;

import com.amalitech.user.service.model.enums.DifficultyLevel;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Represents a user's selection of a skill with a specific difficulty level.
 * Used in onboarding to specify which skills the user wants to track and at what proficiency level.
 *
 * @param skillId the unique identifier of the selected skill
 * @param level the desired difficulty level for the skill
 */
public record SkillSelection(
        @NotNull
        UUID skillId,
        @NotNull
        DifficultyLevel level
) {}
