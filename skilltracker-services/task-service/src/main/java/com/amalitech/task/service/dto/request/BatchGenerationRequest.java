package com.amalitech.task.service.dto.request;

import com.amalitech.task.service.model.enums.TaskDifficulty;
import com.amalitech.task.service.model.enums.TaskType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.util.UUID;

/**
 * An immutable data carrier (DTO) used by clients (or other services)
 * to formally request the AI Task Generator to produce a batch of new challenges.
 * <p>
 * This record ensures that all required parameters—skill, difficulty, and quantity—
 * are provided and validated before the generation process begins, preventing wasted
 * computation time by the large language model (LLM) agents.
 *
 * @param skillName The specific skill domain for which tasks are requested (e.g., "Spring Boot Microservices", "Technical Writing"). Must be present.
 * @param difficulty The desired difficulty level of the tasks, defined by the {@link TaskDifficulty} enum (e.g., EASY, MEDIUM, HARD). Must be present.
 * @param requiredCount The number of tasks the client wishes to receive in the batch. Must be at least 1.
 * @param taskType The preferred format for the generated tasks, defined by the {@link TaskType} enum (e.g., CODING_CHALLENGE, MCQ, VERBAL). Optional; if null, the system may default or generate mixed types.
 */
public record BatchGenerationRequest(
        @NotNull(message = "User ID is required")
        UUID userId,

        @NotBlank(message = "Skill name is required")
        String skillName,

        @NotNull(message = "Difficulty is required")
        TaskDifficulty difficulty,

        @NotNull
        @Min(value = 1, message = "Must request at least 1 task")
        Integer requiredCount,

        TaskType taskType

) implements Serializable {}