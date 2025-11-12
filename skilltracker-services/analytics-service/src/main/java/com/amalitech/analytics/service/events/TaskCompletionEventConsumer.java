package com.amalitech.analytics.service.events;

import com.amalitech.analytics.service.dto.TaskCompletedEvent;
import com.amalitech.analytics.service.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@RabbitListener(
        queues = RabbitMQConstants.TASK_COMPLETION_QUEUE,
        containerFactory = "rabbitListenerContainerFactory"
)
public class TaskCompletionEventConsumer {


    private final AnalyticsService analyticsService;
    @RabbitListener(queues = RabbitMQConstants.TASK_COMPLETION_QUEUE)
    public void onTaskCompleted(TaskCompletedEvent event) {
        try {
            analyticsService.processTaskCompletion(event);
        } catch (Exception e) {
            log.error("Failed to process event for user {}. Sending to DLQ.", event.userId(), e);
        }
    }
}
