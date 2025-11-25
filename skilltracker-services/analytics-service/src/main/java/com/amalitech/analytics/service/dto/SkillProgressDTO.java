package com.amalitech.analytics.service.dto;

import java.util.UUID;



/**
 * Immutable DTO representing a user's current progress in a single skill.
 * <p>
 * Used in dashboard responses. Includes level, XP progression, and performance metrics.
 * </p>
 */
public record SkillProgressDTO(
        UUID skillId,
        String skillName,
        Double averageScore,
        Double proficiency,
        Integer tasksCompleted,
        Integer taskSubmitted,
        Integer tasksFailed,
        Integer currentXp,
        String currentLevel,
        String nextLevel,
        Integer xpToNextLevel,
        Integer currentLevelTotalXp
) {}

