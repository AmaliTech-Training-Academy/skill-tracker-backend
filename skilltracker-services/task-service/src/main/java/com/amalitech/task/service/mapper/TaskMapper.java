package com.amalitech.task.service.mapper;

import com.amalitech.task.service.dto.TaskDTO;
import com.amalitech.task.service.model.Task;

/**
 * Interface for manually mapping Task entities to DTOs.
 * This decouples the service from the DTO conversion logic.
 */
public interface TaskMapper {

    /**
     * Converts a Task entity to a TaskDTO.
     * This implementation will derive the skillName from the
     * nested TaskDefinition.
     *
     * @param task The Task entity to convert.
     * @return The corresponding TaskDTO.
     */
    TaskDTO toDTO(Task task);
}
