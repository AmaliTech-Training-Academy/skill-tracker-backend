package com.amalitech.task.service.service;

import com.amalitech.task.service.dto.request.SubmitAnswerRequest;
import com.amalitech.task.service.model.TaskSubmission;

import java.util.UUID;

/**
 * Service interface for managing TaskSubmissions.
 * Handles creation (user-facing) and updates (internal, from evaluation-service).
 */
public interface SubmissionService {

    /**
     * Creates a new submission, saves it as PENDING, and publishes an
     * event for the evaluation-service to consume.
     *
     * @param request The user's answer submission
     * @param userId  The ID of the user submitting the answer
     * @return The newly created TaskSubmission entity with PENDING status
     */
    TaskSubmission createSubmission(SubmitAnswerRequest request, UUID userId);

    /**
     * Updates an existing submission with results from the evaluation-service.
     * This method is intended for internal service-to-service communication.
     *
     * @param submissionId     The ID of the submission to update
     * @param evaluationResult A DTO or TaskSubmission object containing the feedback, score, and status
     * @return The updated TaskSubmission entity with COMPLETED or ERROR status
     */
    TaskSubmission updateSubmission(UUID submissionId, TaskSubmission evaluationResult);
}