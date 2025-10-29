
package com.amalitech.task.service.service;

import com.amalitech.task.service.dto.TaskSubmissionDTO;
import com.amalitech.task.service.dto.request.SubmitAnswerRequest;
import com.amalitech.common.event.events.SubmissionEvaluatedEvent;

import java.util.UUID;

/**
 * Service interface for managing TaskSubmissions.
 * Handles creation (user-facing) and updates (internal, from evaluation-service).
 *
 * This interface uses DTOs for all public contracts.
 */
public interface SubmissionService {

    /**
     * Creates a new submission, saves it as PENDING, and publishes an
     * event for the evaluation-service to consume.
     *
     * @param request The user's answer submission
     * @param userId  The ID of the user submitting the answer
     * @return The DTO of the newly created TaskSubmission
     */
    TaskSubmissionDTO createSubmission(SubmitAnswerRequest request, UUID userId);

    /**
     * Retrieves a submission by ID with all feedback and results.
     *
     * @param submissionId The submission ID
     * @return The submission DTO with feedback
     */
    TaskSubmissionDTO getSubmissionById(UUID submissionId);

    /**
     * Updates a submission based on an event received from the feedback-service.
     * This is called by the RabbitMQ listener.
     */
    void updateSubmissionFromEvent(SubmissionEvaluatedEvent event);
}