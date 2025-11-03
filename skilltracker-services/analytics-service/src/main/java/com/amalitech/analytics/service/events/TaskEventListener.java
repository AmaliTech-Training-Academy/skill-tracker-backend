package com.amalitech.analytics.service.events;


import com.amalitech.analytics.service.dto.response.TaskCompletedEvent;
import com.amalitech.analytics.service.model.enums.TaskType;
import com.amalitech.analytics.service.service.AnalyticsService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener; // Spring AMQP
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskEventListener {

    private final AnalyticsService analyticsService;


    /**
     * Listens to the queue where Task Service sends "task completed" events.
     */
    @RabbitListener(queues = "${skillboost.rabbitmq.task-completed-queue}")
    @Transactional
    public void handleTaskCompleted(TaskCompletedEvent event) {
        try {
            log.info("Received TaskCompletedEvent for user: {}", event.getUserId());

            TaskType type = TaskType.valueOf(event.getTaskType().toUpperCase());

            analyticsService.processNewProgress(
                    event.getUserId(),
                    event.getSkillId(),
                    event.getTaskId(),
                    event.getScore(),
                    type,
                    event.getRubricsScores()
            );
        } catch (Exception e) {
            log.error("Failed to process TaskCompletedEvent: {}", e.getMessage());
        }
    }
}
