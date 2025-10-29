package com.amalitech.task.service.controller;

import com.amalitech.common.security.dto.response.ApiResponse;
import com.amalitech.task.service.dto.TaskDTO;
import com.amalitech.task.service.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
//    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<TaskDTO>> getTaskById(
            @PathVariable("id") UUID id
    ) {
        log.info("Fetching task by ID: {}", id);

        TaskDTO taskDTO = taskService.getTaskById(id);evak

        ApiResponse<TaskDTO> response = ApiResponse.success(
                "Task retrieved successfully.",
                taskDTO,
                ""
        );

        return ResponseEntity.ok(response);
    }
}