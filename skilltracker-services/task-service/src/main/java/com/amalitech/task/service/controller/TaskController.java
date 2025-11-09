package com.amalitech.task.service.controller;

import com.amalitech.common.security.dto.response.ApiResponse;
import com.amalitech.task.service.dto.TaskDTO;
import com.amalitech.task.service.model.enums.TaskType;
import com.amalitech.task.service.dto.request.McqRequestDTO;
import com.amalitech.task.service.dto.request.UserProfileRequestDTO;
import com.amalitech.task.service.dto.response.LearningPathResponseDTO;
import com.amalitech.task.service.dto.response.McqResponseDTO;
import com.amalitech.task.service.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<TaskDTO>>> getPersonalizedTasks(
            @RequestParam String skillName,
            @RequestParam TaskType taskType,
            @RequestParam(defaultValue = "5") int limit,
            Authentication authentication
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        List<TaskDTO> tasks = taskService.getPersonalizedTasks(userId, skillName, taskType, limit);

        return ResponseEntity.ok(
                ApiResponse.success("Tasks retrieved successfully", tasks, null)
        );
    }
}