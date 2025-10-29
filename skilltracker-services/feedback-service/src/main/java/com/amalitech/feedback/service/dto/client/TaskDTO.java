package com.amalitech.feedback.service.dto.client;

import com.amalitech.feedback.service.dto.client.content.TaskContent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * CLIENT DTO we get from task-service (GET /api/v1/tasks/{id}).
 * This is a FAITHFUL COPY of the TaskDTO from the task-service,
 * including support for polymorphic TaskContent.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskDTO {

    private UUID id;
    private String title;
    private String description;
    private String skillName;
    private Integer version;
    private Integer xpReward;
    private Integer estimatedDuration;

    private TaskType type;
    private TaskDifficulty difficulty;

    /**
     * The polymorphic task content (e.g., CodingTaskContent).
     */
    private TaskContent content;

    public enum TaskType {
        MULTIPLE_CHOICE,
        CODING,
        ESSAY
    }

    public enum TaskDifficulty {
        EASY,
        MEDIUM,
        HARD
    }
}