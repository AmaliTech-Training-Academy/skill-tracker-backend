package com.amalitech.task.service.listener;

import com.amalitech.common.event.events.SubmissionEvaluatedEvent;
import com.amalitech.task.service.service.SubmissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubmissionListener {

    private final SubmissionService submissionService;

    @RabbitListener(queues = "submission.evaluated.q")
    public void handleSubmissionEvaluated(SubmissionEvaluatedEvent event) {
        log.info("Received evaluated result for submission: {}", event.getSubmissionId());
        try {
            submissionService.updateSubmissionFromEvent(event);

            // TODO: Add logic to notify user via WebSocket
            // 2. Push notification to user via WebSocket
            // webSocketService.sendResult(event.getUserId(), event);

        } catch (Exception e) {
            log.error("Failed to process evaluated submission: {}. Error: {}", event.getSubmissionId(), e.getMessage());
            throw new AmqpRejectAndDontRequeueException("Processing failed", e);
        }
    }
}