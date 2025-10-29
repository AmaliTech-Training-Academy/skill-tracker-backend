package com.amalitech.notification.service.listener;

import com.amalitech.common.event.events.SubmissionEvaluatedEvent;
import com.amalitech.common.event.events.SubmissionExecutedEvent;
import com.amalitech.notification.service.config.RabbitMQConfig;
import com.amalitech.notification.service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubmissionEventListener {

    private final NotificationService notificationService;

    @RabbitListener(queues = RabbitMQConfig.EXECUTED_QUEUE)
    public void handleSubmissionExecuted(SubmissionExecutedEvent event) {
        log.info("Received SubmissionExecutedEvent for submission: {}", event.getSubmissionId());
        
        try {
            notificationService.sendExecutionResults(event);
            log.info("Successfully processed SubmissionExecutedEvent for submission: {}", 
                    event.getSubmissionId());
        } catch (Exception e) {
            log.error("Error processing SubmissionExecutedEvent for submission: {}", 
                    event.getSubmissionId(), e);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.EVALUATED_QUEUE)
    public void handleSubmissionEvaluated(SubmissionEvaluatedEvent event) {
        log.info("Received SubmissionEvaluatedEvent for submission: {}", event.getSubmissionId());
        
        try {
            notificationService.sendEvaluationFeedback(event);
            log.info("Successfully processed SubmissionEvaluatedEvent for submission: {}", 
                    event.getSubmissionId());
        } catch (Exception e) {
            log.error("Error processing SubmissionEvaluatedEvent for submission: {}", 
                    event.getSubmissionId(), e);
        }
    }
}
