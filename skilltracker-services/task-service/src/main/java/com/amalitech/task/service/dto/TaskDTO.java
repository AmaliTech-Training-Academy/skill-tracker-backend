package com.amalitech.task.service.dto;

import com.amalitech.task.service.model.content.TaskContent;
import com.amalitech.task.service.model.enums.TaskDifficulty;
import com.amalitech.task.service.model.enums.TaskType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Data Transfer Object (DTO) representing a single learning task or challenge.
 * <p>
 * This class is used to expose comprehensive task-related information to
 * clients (e.g., the front-end application or partner services via REST responses).
 * It acts as a standardized projection of the internal Task entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskDTO {
    private UUID id;
    private String title;
    private String description;
    private TaskType type;
    private TaskDifficulty difficulty;
    private TaskContent content;
    private Integer xpReward;
    private Integer estimatedDuration;
    private String skillName;
    private Integer version;
}