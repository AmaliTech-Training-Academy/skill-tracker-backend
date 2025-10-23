package com.amalitech.task.service.service;

import com.amalitech.task.service.dto.TaskAvailabilityDTO;
import com.amalitech.task.service.dto.TaskDTO;
import com.amalitech.task.service.exception.ResourceNotFoundException;
import com.amalitech.task.service.model.enums.TaskDifficulty;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for managing and retrieving Tasks.
 * This defines the contract for all task-related business logic.
 */
public interface TaskService {

    /**
     * Get personalized tasks for a user based on their skill and difficulty.
     * Triggers asynchronous generation if no cached tasks are available.
     *
     * @param userId The ID of the user
     * @param skillName The name of the skill
     * @param limit The maximum number of tasks to return
     * @return A list of TaskDTOs, which may be empty if tasks are being generated.
     */
    List<TaskDTO> getPersonalizedTasks(UUID userId, String skillName, int limit);

    /**
     * Get tasks for a specific skill and difficulty combination.
     * Triggers asynchronous generation if no cached tasks are available.
     *
     * @param skillName The name of the skill
     * @param difficulty The requested task difficulty
     * @param limit The maximum number of tasks to return
     * @return A list of TaskDTOs, which may be empty if tasks are being generated.
     */
    List<TaskDTO> getTasksForSkillAndDifficulty(String skillName, TaskDifficulty difficulty, int limit);

    /**
     * Retrieves a single, detailed task by its ID, including starter code
     * and visible test cases.
     *
     * @param taskId The
     * @return The detailed TaskDTO.
     * @throws ResourceNotFoundException if task is not found.
     */
    TaskDTO getTaskById(UUID taskId);

    /**
     * Checks the number of available (cached) tasks for a specific
     * skill and difficulty combination.
     *
     * @param skillName The name of the skill
     * @param difficulty The requested task difficulty
     * @return A DTO with the count and a flag if generation is needed.
     */
    TaskAvailabilityDTO checkTaskAvailability(String skillName, TaskDifficulty difficulty);
}