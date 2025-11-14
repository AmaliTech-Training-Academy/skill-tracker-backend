package com.amalitech.task.service.service;

import com.amalitech.task.service.dto.TaskAvailabilityDTO;
import com.amalitech.task.service.dto.TaskDTO;
import com.amalitech.task.service.dto.request.GenerateTaskRequest;
import com.amalitech.task.service.dto.request.McqRequestDTO;
import com.amalitech.task.service.dto.request.UserProfileRequestDTO;
import com.amalitech.task.service.dto.response.*;
import com.amalitech.task.service.exception.ResourceNotFoundException;
import com.amalitech.task.service.model.enums.TaskDifficulty;
import com.amalitech.task.service.model.enums.TaskType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Service interface for managing and retrieving Tasks.
 * This defines the contract for all task-related business logic.
 */
public interface TaskService {

    McqResponseDTO generateMCQ(McqRequestDTO taskDTO) throws Exception;

    LearningPathResponseDTO generateLearningPath(UserProfileRequestDTO userProfileRequestDTO) throws IOException;

    LearningPathResponseDTO getTaskByUserIdAndCurrentSkill(String userId, String currentSkill);

    /**
     * Get personalized tasks for a user based on their skill and difficulty.
     * Triggers asynchronous generation if no cached tasks are available.
     *
     * @param userId The ID of the user
     * @param skillName The name of the skill
     * @param limit The maximum number of tasks to return
     * @return A list of TaskDTOs, which may be empty if tasks are being generated.
     */
    List<TaskDTO> getPersonalizedTasks(UUID userId, String skillName, TaskType taskType, int limit);

    /**
     * Get tasks for a specific skill and difficulty combination.
     * Triggers asynchronous generation if no cached tasks are available.
     *
     * @param skillName The name of the skill
     * @param difficulty The requested task difficulty
     * @param limit The maximum number of tasks to return
     * @return A list of TaskDTOs, which may be empty if tasks are being generated.
     */
    List<TaskDTO> getTasksForSkillAndDifficulty(String skillName, TaskDifficulty difficulty, TaskType taskType, int limit);

    /**
     * Retrieves a single, detailed task by its ID, including starter code
     * and visible test cases.
     *
     * @param taskId The unique identifier of the task to retrieve.
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
    /**
     * Retrieves a paginated list of all tasks for administrative purposes.
     * @param pageable The pagination information (page, size, sort).
     * @return A paginated list of lightweight task summaries.
     */
    Page<AdminTaskSummaryResponse> getAllTasksForAdmin(Pageable pageable);

    /**
     * Retrieves the full details of a single task for the admin view.
     * @param taskId The ID of the task to retrieve.
     * @return The detailed task DTO including its content.
     * @throws ResourceNotFoundException if task is not found.
     */
    AdminTaskDetailResponse getTaskForAdmin(UUID taskId);

    /**
     * Handles the business logic for an admin's request to generate a task.
     * It creates the message payload and publishes it to the queue.
     *
     * @param requestBody The task generation parameters from the admin.
     * @param adminUserId The UUID of the admin making the request (for notifications).
     */
    void requestSpecificTaskGeneration(GenerateTaskRequest requestBody, UUID adminUserId);

    /**
     * Retrieves tasks grouped by status (pending/completed) for a specific user.
     * Tasks are filtered based on the user's skill profile and only published tasks are returned.
     * Both pending and completed task lists support independent pagination.
     *
     * @param userId The ID of the user
     * @param pendingPage Page number for pending tasks
     * @param pendingSize Page size for pending tasks
     * @param completedPage Page number for completed tasks
     * @param completedSize Page size for completed tasks
     * @return UserTasksResponse containing paginated pending and completed tasks
     */
    UserTasksResponse getUserTasksGroupedByStatus(UUID userId, int pendingPage, int pendingSize,
                                                  int completedPage, int completedSize,
                                                  String skillName, String completedPeriod);

}
