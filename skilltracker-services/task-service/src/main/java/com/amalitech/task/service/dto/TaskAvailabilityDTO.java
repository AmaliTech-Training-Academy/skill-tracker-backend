package com.amalitech.task.service.dto;

import com.amalitech.task.service.model.enums.TaskDifficulty;
import lombok.Builder;

/**
 * An immutable Data Transfer Object (DTO) that reports the current inventory status
 * of pre-generated tasks for a specific skill and difficulty level within the SkillBoost platform.
 * <p>
 * This DTO is strategically used by the platform's adaptive learning engine and front-end clients
 * to assess task readiness. It dictates whether the system should immediately serve an existing task
 * or trigger a costly, asynchronous AI task generation process to replenish the inventory. The use
 * of a Java record ensures the data contract is clean, concise, and immutable, which is vital
 * for predictable data flow.
 *
 * @param skillName The name of the skill being queried (e.g., "Spring Boot REST APIs").
 * @param difficulty The difficulty level being queried, defined by the {@link TaskDifficulty} enum.
 * @param availableTaskCount The number of tasks currently available in the database cache for this
 * skill/difficulty combination.
 * @param needsGeneration A boolean flag indicating whether the existing task count has dropped
 * below a predetermined minimum threshold, requiring new AI task generation.
 */
@Builder
public record TaskAvailabilityDTO(
        String skillName,
        TaskDifficulty difficulty,
        Integer availableTaskCount,
        Boolean needsGeneration
) { }