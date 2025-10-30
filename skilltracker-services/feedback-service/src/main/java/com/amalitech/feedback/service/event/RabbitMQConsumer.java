package com.amalitech.feedback.service.event;

import com.amalitech.common.event.events.SubmissionCreatedEvent;
import com.amalitech.feedback.service.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

/**
 * RabbitMQ-specific consumer.
 * Its ONLY job is to receive a message and pass it to the
 * abstract SubmissionHandler.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RabbitMQConsumer {

    private final SubmissionHandler submissionHandler;

    /**
     * Listens for our new, flat event from the common library.
     */
    @RabbitListener(queues = RabbitMQConfig.SUBMISSION_CREATED_QUEUE)
    public void onSubmissionCreated(SubmissionCreatedEvent event) {
        log.info("Received submission from RabbitMQ: {}", event.getSubmissionId());
        try {
            submissionHandler.handleSubmission(event);
        } catch (Exception e) {
            log.error("Unhandled exception from SubmissionHandler for ID: {}. ",
                    event.getSubmissionId(), e);
            throw e;
        }
    }
}