package com.amalitech.task.service.listener;

import com.amalitech.common.event.events.SubmissionEvaluatedEvent;
import com.amalitech.task.service.service.SubmissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Component responsible for consuming asynchronous events from the message broker related
 * to the task evaluation process.
 * <p>
 * Specifically, this listener handles the final result published by the downstream
 * Evaluation Service, updating the status and results of the submission in the local
 * persistence layer. It serves as the bridge for re-integrating asynchronous results
 * back into the Task Service's data model.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SubmissionListener {

    private final SubmissionService submissionService;

    /**
     * Listens for the {@code SubmissionEvaluatedEvent} published by the feedback Service
     * and processes the results.
     * <p>
     * The method is bound to the {@code submission.evaluated.q} queue. It delegates the
     * task of updating the submission record with the scores and feedback to the
     * {@link SubmissionService}. Any unrecoverable exception thrown during processing
     * results in the message being rejected and not re-queued, preventing poison message issues.
     *
     * @param event The {@link SubmissionEvaluatedEvent} containing the final evaluation details.
     * @throws AmqpRejectAndDontRequeueException if an error occurs during processing that should not trigger a retry.
     */
    @RabbitListener(queues = "submission.evaluated.q")
    public void handleSubmissionEvaluated(SubmissionEvaluatedEvent event) {
        log.info("Received evaluated result for submission: {}", event.getSubmissionId());
        try {
            submissionService.updateSubmissionFromEvent(event);

        } catch (Exception e) {
            log.error("Failed to process evaluated submission: {}. Error: {}", event.getSubmissionId(), e.getMessage());
            throw new AmqpRejectAndDontRequeueException("Processing failed", e);
        }
    }
}