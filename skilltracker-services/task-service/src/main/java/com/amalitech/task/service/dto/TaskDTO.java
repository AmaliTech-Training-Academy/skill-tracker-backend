package com.amalitech.task.service.dto;

import com.amalitech.task.service.model.content.TaskContent;
import com.amalitech.task.service.model.enums.TaskDifficulty;
import com.amalitech.task.service.model.enums.TaskType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Data Transfer Object (DTO) representing a Task.
 * This class is used to expose task-related information
 * to clients (e.g., REST responses) without exposing internal entities.
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
    private List<StarterCodeDTO> starterCodes;

}

