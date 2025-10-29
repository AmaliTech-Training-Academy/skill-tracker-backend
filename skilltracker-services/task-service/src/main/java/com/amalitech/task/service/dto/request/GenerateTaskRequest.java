package com.amalitech.task.service.dto.request;

import com.amalitech.task.service.model.enums.TaskDifficulty;
import com.amalitech.task.service.model.enums.TaskType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;

public record GenerateTaskRequest(
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


