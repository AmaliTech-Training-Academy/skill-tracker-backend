package com.amalitech.user.service.dto.request;

import com.amalitech.user.service.model.enums.DifficultyLevel;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
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
        @NotNull(message = "Skill ID is required")
        UUID skillId,

        @NotNull(message = "Skill level is required")
        DifficultyLevel level
) {
    /**
     * Custom creator to normalize the level string to uppercase before enum conversion.
     * This allows users to send "beginner", "Beginner", or "BEGINNER" - all will work.
     */
    @JsonCreator
    public SkillSelection(
            @JsonProperty("skillId") UUID skillId,
            @JsonProperty("level") String level) {
        this(skillId, DifficultyLevel.valueOf(level.toUpperCase()));
    }

    /**
     * Standard constructor for programmatic use
     */
    public SkillSelection(UUID skillId, DifficultyLevel level) {
        this.skillId = skillId;
        this.level = level;
    }
}
