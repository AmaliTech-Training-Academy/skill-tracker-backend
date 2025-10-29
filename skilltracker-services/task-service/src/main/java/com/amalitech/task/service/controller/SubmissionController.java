package com.amalitech.task.service.controller;

import com.amalitech.common.security.dto.response.ApiResponse;
import com.amalitech.task.service.dto.TaskSubmissionDTO;
import com.amalitech.task.service.dto.request.SubmitAnswerRequest;
import com.amalitech.task.service.dto.response.SubmissionResponse;
import com.amalitech.task.service.service.SubmissionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/submissions")
@RequiredArgsConstructor
@Slf4j
public class SubmissionController {

    private final SubmissionService submissionService;

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
            throw new IllegalArgumentException("Invalid User ID format in token.");
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
}