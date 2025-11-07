package com.amalitech.task.service.dto.request;

import com.amalitech.task.service.model.enums.TaskDifficulty;
import com.amalitech.task.service.model.enums.TaskType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.util.UUID;

/**
 * An immutable data carrier (DTO) used to request the explicit, targeted generation of a single task
 * by the AI Task Agent.
 * <p>
 * This record provides the highly specific parameters necessary for the generative AI
 * (DeepSeek API or specialized agents) to create a task that aligns perfectly with a user's
 * current learning context. It enforces data integrity through JSR-303 (Jakarta Validation)
 * constraints.
 *
 * @param taskType The specific format required for the output task, defined by the {@link TaskType} enum (e.g., CODING_CHALLENGE, MCQ). Must be present.
 * @param skillName The overarching skill domain to which the task belongs (e.g., "Data Structures and Algorithms"). Must be present.
 * @param difficulty The required difficulty level, defined by the {@link TaskDifficulty} enum (e.g., EASY, MEDIUM). Must be present.
 * @param topic The narrow, specific topic within the skill domain (e.g., "Binary Search Tree Insertion"). Must be present.
 * @param languageName The programming language or natural language context for the task (e.g., "Java", "English"). Optional, but highly recommended for technical tasks.
 */
public record GenerateTaskRequest(
        @NotNull
        UUID userId,

        @NotNull(message = "Task type is required")
        TaskType taskType,

        @NotBlank(message = "Skill name is required")
        String skillName,

        @NotNull(message = "Difficulty is required")
        TaskDifficulty difficulty,

        @NotBlank(message = "Topic is required")
        String topic,

        String languageName
) implements Serializable {}