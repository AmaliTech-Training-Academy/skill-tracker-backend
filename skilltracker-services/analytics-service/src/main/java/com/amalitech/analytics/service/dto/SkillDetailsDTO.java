package com.amalitech.analytics.service.dto;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.micrometer.common.lang.NonNull;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)  // Ignore extra JSON fields from cache

/**
 * Immutable DTO representing skill metadata and XP thresholds for leveling.
 * <p>
 * Used to compute current level, next level, and XP progression from raw XP values.
 * </p>
 */
public record SkillDetailsDTO(
        @NonNull UUID skillId,
        @NonNull String skillName,
        int intermediateXpThreshold,
        int advancedXpThreshold
) {

    /** Beginner threshold is always 0. */
    private static final int BEGINNER_XP_THRESHOLD = 0;

    /**
     * Constructs a {@code SkillDetailsDTO} from JSON input.
     * <p>
     * Marked with {@code @JsonCreator} to enable deserialization via Jackson.
     * </p>
     */
    @JsonCreator
    public SkillDetailsDTO(
            @JsonProperty("skillId") @NonNull UUID skillId,
            @JsonProperty("skillName") @NonNull String skillName,
            @JsonProperty("intermediateXpThreshold") int intermediateXpThreshold,
            @JsonProperty("advancedXpThreshold") int advancedXpThreshold
    ) {
        this.skillId = skillId;
        this.skillName = skillName;
        this.intermediateXpThreshold = intermediateXpThreshold;
        this.advancedXpThreshold = advancedXpThreshold;
    }

    /**
     * Returns the XP threshold required to reach the given level.
     *
     * @param level the target level (case-insensitive)
     * @return XP required to enter this level; {@code 0} for unknown levels
     */
    public int getXpForLevel(@NonNull String level) {
        return switch (level.toUpperCase()) {
            case "BEGINNER" -> BEGINNER_XP_THRESHOLD;
            case "INTERMEDIATE" -> intermediateXpThreshold;
            case "ADVANCED" -> advancedXpThreshold;
            default -> 0;
        };
    }

    /**
     * Determines the current skill level based on earned XP.
     *
     * @param xp total XP earned in this skill
     * @return current level as a string: "BEGINNER", "INTERMEDIATE", or "ADVANCED"
     */
    public String getCurrentLevel(int xp) {
        if (xp >= advancedXpThreshold) return "ADVANCED";
        if (xp >= intermediateXpThreshold) return "INTERMEDIATE";
        return "BEGINNER";
    }

    /**
     * Returns the next level after the current one.
     * <p>
     * If already at "ADVANCED", returns "MASTER" (terminal level).
     * </p>
     *
     * @param currentLevel the current level
     * @return next level as a string
     */
    public String getNextLevel(@NonNull String currentLevel) {
        return switch (currentLevel.toUpperCase()) {
            case "BEGINNER" -> "INTERMEDIATE";
            case "INTERMEDIATE" -> "ADVANCED";
            default -> "BEGINNER";
        };
    }

    /** Helper to expose beginner threshold consistently. */
    private int beginnerXpThreshold() {
        return BEGINNER_XP_THRESHOLD;
    }
}