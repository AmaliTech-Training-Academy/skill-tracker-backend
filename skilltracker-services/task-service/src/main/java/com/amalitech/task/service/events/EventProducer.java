package com.amalitech.task.service.events;

import com.amalitech.common.event.events.SubmissionCreatedEvent;
import com.amalitech.common.event.events.SubmissionEvaluatedEvent;
import com.amalitech.common.event.events.TaskCompletedEvent;
import com.amalitech.task.service.dto.request.BatchGenerationRequest;
import com.amalitech.task.service.dto.request.GenerateTaskRequest;

/**
 * Abstract interface for an event producer.
 *
 * This decouples the business logic (e.g., SubmissionService) from the
 * specific messaging technology (e.g., RabbitMQ, Kafka).
 */
public interface EventProducer {

    /**
     * Publishes a request to generate a batch of tasks.
     */
    void requestBatchTaskGeneration(BatchGenerationRequest request);

    /**
     * Publishes a request for a single, admin-generated task.
     */
    void requestSpecificTaskGeneration(GenerateTaskRequest request);

    /**
     * Publishes an event when a new submission is created.
     *
     * @param submissionDTO The DTO of the submission to be evaluated.
     */
    void publishSubmissionCreated(SubmissionCreatedEvent submissionDTO);

    /**
     * Publishes an event when a submission has been evaluated.
     *
     * @param submissionDTO The DTO of the fully evaluated submission.
     */
    void publishSubmissionEvaluated(SubmissionEvaluatedEvent submissionDTO);

    /**
     * Publishes an event when a task has been completed by a user.
     *
     * @param event The {@link TaskCompletedEvent} containing completion details.
     */
    void publishTaskCompleted(TaskCompletedEvent event);
}