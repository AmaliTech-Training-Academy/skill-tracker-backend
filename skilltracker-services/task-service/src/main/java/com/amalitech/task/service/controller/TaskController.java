package com.amalitech.task.service.controller;

import com.amalitech.common.security.dto.response.ApiResponse;
import com.amalitech.task.service.dto.TaskDTO;
import com.amalitech.task.service.dto.request.McqRequestDTO;
import com.amalitech.task.service.dto.request.UserProfileRequestDTO;
import com.amalitech.task.service.dto.response.LearningPathResponseDTO;
import com.amalitech.task.service.dto.response.McqResponseDTO;
import com.amalitech.task.service.dto.response.UserTasksResponse;
import com.amalitech.task.service.model.enums.TaskType;
import com.amalitech.task.service.service.SkillService;
import com.amalitech.task.service.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Controller for managing and retrieving Task details.
 */
@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
@Slf4j
public class TaskController {

    private final TaskService taskService;
    private final SkillService skillService;

    /**
     * Retrieves a single, detailed task by its ID.
     * This endpoint is used by the feedback-service to get test cases
     * and by the frontend to display task details.
     *
     * Requires the caller to be authenticated (either a user or an internal service).
     *
     * @param id The unique identifier of the task to retrieve.
     * @return A ResponseEntity containing the TaskDTO wrapped in ApiResponse.
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<TaskDTO>> getTaskById(
            @PathVariable("id") UUID id
    ) {
        log.info("Fetching task by ID: {}", id);

        TaskDTO taskDTO = taskService.getTaskById(id);

        ApiResponse<TaskDTO> response = ApiResponse.success(
                "Task retrieved successfully.",
                taskDTO,
                ""
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/learning-path")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<LearningPathResponseDTO>> getLearningPathByUserId(
            @RequestParam("id") String userId,
            @RequestParam("skill") String skill
    ) {
        log.info("Fetching Learning path by user ID: {}", userId);

        LearningPathResponseDTO learningPath = taskService.getLPByUserIdAndCurrentSkill(userId, skill);

        ApiResponse<LearningPathResponseDTO> response = ApiResponse.success(
                "Learning path retrieved successfully.",
                learningPath,
                ""
        );

        return ResponseEntity.ok(response);
    }


    /**
     * This accepts an McqResponseDTO and returns a McqResponseDTO*/
    @PostMapping("/generate/mcq")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<McqResponseDTO>> generateMCQ(
            @RequestBody McqRequestDTO taskDTO
    ) throws Exception {
        McqResponseDTO mcqTask = taskService.generateMCQ(taskDTO);
        return ResponseEntity.ok(ApiResponse.success("MCQ Task Generated Successfully", mcqTask, ""));
    }

    @GetMapping("/getMCQs/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<McqResponseDTO>> getMCQsByUserId(
            @PathVariable("id") String id) {
        McqResponseDTO tasks = taskService.getMCQByUserId(id);
        return ResponseEntity.ok(ApiResponse.success("MCQ Task Generated Successfully", tasks, ""));
    }

    @PostMapping("/generate/learning-path")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<LearningPathResponseDTO>> generateLearningPath(
            @RequestBody() UserProfileRequestDTO userProfileRequestDTO
    ) throws IOException {
        LearningPathResponseDTO learningPathResponseDTO = taskService.generateLearningPath(userProfileRequestDTO);
        return ResponseEntity.ok(ApiResponse.success("Learning path Generated Successfully", learningPathResponseDTO, ""));
    }


    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<TaskDTO>>> getPersonalizedTasks(
            @RequestParam String skillName,
            @RequestParam(required = false) TaskType taskType,
            @RequestParam(defaultValue = "5") int limit,
            Authentication authentication
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        List<TaskDTO> tasks = taskService.getPersonalizedTasks(userId, skillName, taskType, limit);

        return ResponseEntity.ok(
                ApiResponse.success("Tasks retrieved successfully", tasks, null)
        );
    }

    /**
     * Retrieves tasks grouped by status (pending/completed) for the authenticated user.
     * Tasks are filtered based on the user's skill profile and only published tasks are returned.
     * Supports filtering by skill and a time period for completed tasks.
     *
     * @param pendingPage Page number for pending tasks (default: 0)
     * @param pendingSize Page size for pending tasks (default: 10)
     * @param completedPage Page number for completed tasks (default: 0)
     * @param completedSize Page size for completed tasks (default: 10)
     * @param skillName Optional. The name of the skill to filter by (e.g., "Python").
     * @param completedPeriod Optional. The time period to filter completed tasks
     * (e.g., "TODAY", "LAST_7_DAYS"). Default: "ALL_PERIODS".
     * @param authentication Spring Security authentication object containing userId
     * @return ResponseEntity containing UserTasksResponse with paginated pending and completed tasks
     */
    @GetMapping("/my-tasks")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserTasksResponse>> getUserTasks(
            @RequestParam(defaultValue = "0") int pendingPage,
            @RequestParam(defaultValue = "10") int pendingSize,
            @RequestParam(defaultValue = "0") int completedPage,
            @RequestParam(defaultValue = "10") int completedSize,
            @RequestParam(required = false) String skillName,
            @RequestParam(required = false, defaultValue = "ALL_PERIODS") String completedPeriod,
            Authentication authentication
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        UserTasksResponse response = taskService.getUserTasksGroupedByStatus(
                userId, pendingPage, pendingSize, completedPage, completedSize,
                skillName, completedPeriod
        );

        return ResponseEntity.ok(
                ApiResponse.success("Tasks retrieved successfully", response, null)
        );
    }
}