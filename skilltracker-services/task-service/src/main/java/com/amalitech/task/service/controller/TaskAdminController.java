package com.amalitech.task.service.controller;

import com.amalitech.common.security.dto.response.ApiResponse;
import com.amalitech.task.service.dto.response.AdminTaskDetailResponse;
import com.amalitech.task.service.dto.response.AdminTaskSummaryResponse;
import com.amalitech.task.service.service.TaskService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/tasks")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAuthority('ADMIN')")
public class TaskAdminController {

    private final TaskService taskService;

    /**
     * GET /api/v1/admin/tasks
     * Retrieves a paginated LIST of task summaries.
     * Supports pagination: ?page=0&size=20&sort=createdAt,desc
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<AdminTaskSummaryResponse>>> getAllTasks(
            @PageableDefault(
                    size = 20,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        log.info("Admin request: Get all tasks, page: {}", pageable.getPageNumber());
        Page<AdminTaskSummaryResponse> tasks = taskService.getAllTasksForAdmin(pageable);
        return ResponseEntity.ok(
                ApiResponse.success("Tasks retrieved successfully", tasks, null)
        );
    }

    /**
     * GET /api/v1/admin/tasks/{id}
     * Retrieves the full DETAILS of a single task, including its content.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminTaskDetailResponse>> getTaskById(
            @PathVariable UUID id
    ) {
        log.info("Admin request: Get task details for ID: {}", id);

        AdminTaskDetailResponse taskDetail = taskService.getTaskForAdmin(id);

        return ResponseEntity.ok(
                ApiResponse.success("Task details retrieved successfully", taskDetail, null)
        );
    }
}