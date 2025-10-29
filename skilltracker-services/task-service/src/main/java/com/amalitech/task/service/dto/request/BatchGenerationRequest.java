package com.amalitech.task.service.dto.request;

import com.amalitech.task.service.model.enums.TaskDifficulty;
import com.amalitech.task.service.model.enums.TaskType;
import jakarta.validation.constraints.Min; // Import for validation
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * An immutable data carrier for requesting a batch of tasks.
 */
public record BatchGenerationRequest(
        @NotBlank(message = "Skill name is required")
        String skillName,

        @NotNull(message = "Difficulty is required")
        TaskDifficulty difficulty,

        @NotNull
        @Min(value = 1, message = "Must request at least 1 task")
        Integer requiredCount,

        TaskType taskType

) implements Serializable {
}