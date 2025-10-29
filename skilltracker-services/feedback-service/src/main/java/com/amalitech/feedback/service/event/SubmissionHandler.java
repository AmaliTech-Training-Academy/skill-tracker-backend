package com.amalitech.feedback.service.event;

import com.amalitech.common.event.events.SubmissionCreatedEvent;

/**
 * Abstract interface for handling an incoming submission.
 *
 * This decouples the core business logic from the specific
 * messaging technology (e.g., RabbitMQ, Kafka).
 */
public interface SubmissionHandler {

    /**
     * Processes a single submission for evaluation.
     * This method contains the full orchestration logic.
     *
     * @param submissionDTO The submission to be processed.
     */
    void handleSubmission(SubmissionCreatedEvent submissionDTO);
}
