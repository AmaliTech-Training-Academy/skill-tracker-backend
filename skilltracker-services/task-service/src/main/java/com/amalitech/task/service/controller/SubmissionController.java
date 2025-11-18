package com.amalitech.task.service.controller;

import com.amalitech.common.security.dto.response.ApiError;
import com.amalitech.common.security.dto.response.ApiResponse;
import com.amalitech.task.service.dto.TaskSubmissionDTO;
import com.amalitech.task.service.dto.request.RunCodeRequest;
import com.amalitech.task.service.dto.request.SubmitAnswerRequest;
import com.amalitech.task.service.dto.response.RunCodeResponse;
import com.amalitech.task.service.dto.response.SubmissionResponse;
import com.amalitech.task.service.exception.InvalidUserIdException;
import com.amalitech.task.service.service.CodeExecutionProcessor;
import com.amalitech.task.service.service.SubmissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for handling user task submissions and retrieval of submission results
 * <p>
 * This component is responsible for receiving user answers to AI-generated challenges,
 * initiating the evaluation process, and providing status updates or final results.
 * It enforces authentication via the {@code @PreAuthorize} annotation, relying on the
 * upstream authentication filter to populate the user principal.
 */
@RestController
@RequestMapping("/api/v1/submissions")
@RequiredArgsConstructor
@Slf4j
public class SubmissionController {

    private final SubmissionService submissionService;
    private final CodeExecutionProcessor codeExecutionProcessor;

    /**
     * Accepts a user's answer to a SkillBoost challenge and initiates the AI evaluation process.
     * <p>
     * This endpoint requires an authenticated user and uses the {@code @AuthenticationPrincipal}
     * to extract the user's ID for linking the submission. The response uses HTTP 202 ACCEPTED
     * status, indicating that the submission has been successfully received and evaluation
     * will occur asynchronously.
     *
     * @param userIdPrincipal The authenticated user's ID string, derived from the security principal.
     * @param request The {@link SubmitAnswerRequest} containing the task ID and the user's answer.
     * @return A {@link ResponseEntity} containing a {@link ApiResponse} with the newly created
     * submission ID and its initial status (e.g., PENDING).
     * @throws IllegalArgumentException if the user ID principal cannot be parsed into a UUID.
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<SubmissionResponse>> submitTask(
            @AuthenticationPrincipal String userIdPrincipal,
            @Valid @RequestBody SubmitAnswerRequest request
    ) {
        UUID userId;
        try {
            userId = UUID.fromString(userIdPrincipal);
        } catch (Exception e) {
            log.error("Invalid User ID format in security principal: {}", userIdPrincipal);
            throw new InvalidUserIdException("Invalid User ID format in token.", e);
        }
        log.info("Submission received from authenticated user: {}", userId);

        TaskSubmissionDTO submissionDTO = submissionService.createSubmission(request, userId);

        SubmissionResponse responseData = new SubmissionResponse(submissionDTO.getId(), submissionDTO.getStatus());
        ApiResponse<SubmissionResponse> apiResponse = ApiResponse.success(
                "Submission accepted for evaluation.",
                responseData,
                null
        );

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(apiResponse);
    }

    /**
     * Retrieves the details and current status of a specific user submission by its ID.
     * <p>
     * This endpoint is crucial for the client to poll for evaluation results (e.g., grades, AI feedback)
     * once the submission process is complete. Authorization logic (not shown, but typically in the
     * service layer) must ensure the user can only view their own submissions or submissions they are
     * authorized to view (e.g., team managers).
     *
     * @param id The UUID of the submission to retrieve.
     * @return A {@link ResponseEntity} containing a {@link ApiResponse} with the full
     * {@link TaskSubmissionDTO}, including evaluation results if available.
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<TaskSubmissionDTO>> getSubmission(
            @PathVariable("id") UUID id
    ) {
        log.info("Fetching submission by ID: {}", id);

        TaskSubmissionDTO submissionDTO = submissionService.getSubmissionById(id);

        ApiResponse<TaskSubmissionDTO> response = ApiResponse.success(
                "Submission retrieved successfully.",
                submissionDTO,
                null
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Executes user code against test cases and returns immediate results.
     * <p>
     * This endpoint allows users to test their code before submitting for formal evaluation.
     * Results are returned synchronously with execution metrics and test case comparisons.
     * No submission record is created, and no evaluation score is assigned.
     *
     * @param userIdPrincipal The authenticated user's ID string.
     * @param request The {@link RunCodeRequest} containing task ID, code, and language ID.
     * @return A {@link ResponseEntity} containing a {@link ApiResponse} with test execution results.
     */
    @PostMapping("/run-code")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> runCode(
            @AuthenticationPrincipal String userIdPrincipal,
            @Valid @RequestBody RunCodeRequest request
    ) {
        log.info("Code execution request from user: {} for task: {}", userIdPrincipal, request.taskId());

        try {
            RunCodeResponse response = codeExecutionProcessor.executeCode(
                    request.taskId(),
                    request.code(),
                    request.languageId()
            ).block(); // Synchronous execution

            log.info("Code execution completed for user: {}, task: {}", userIdPrincipal, request.taskId());

            ApiResponse<RunCodeResponse> apiResponse = ApiResponse.success(
                    "Code executed successfully",
                    response,
                    null
            );

            return ResponseEntity.ok(apiResponse);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid request for code execution: {}", e.getMessage());
            ApiError errorResponse = ApiError.of(
                    400,
                    "Invalid request",
                    e.getMessage(),
                    "/api/v1/submissions/run-code",
                    new ArrayList<>(),
                    null
            );
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            log.error("Code execution failed for user: {}, task: {}", userIdPrincipal, request.taskId(), e);
            ApiError errorResponse = ApiError.of(
                    500,
                    "Code execution failed",
                    e.getMessage(),
                    "/api/v1/submissions/run-code",
                    new ArrayList<>(),
                    null
            );
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}