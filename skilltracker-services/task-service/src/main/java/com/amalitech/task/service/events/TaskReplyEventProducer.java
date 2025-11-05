package com.amalitech.task.service.events;

import com.amalitech.common.event.events.TaskGenerationFailedEvent;
import com.amalitech.common.event.events.TaskGenerationSucceededEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class TaskReplyEventProducer {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchanges.task-reply}")
    private String taskReplyExchange;

    @Value("${rabbitmq.routing-keys.task-success}")
    private String taskSuccessRoutingKey;

    @Value("${rabbitmq.routing-keys.task-failed}")
    private String taskFailedRoutingKey;

    public void publishTaskGenerationSucceeded(TaskGenerationSucceededEvent event) {
        log.info("Publishing task generation success for user: {}", event.getUserId());
        rabbitTemplate.convertAndSend(taskReplyExchange, taskSuccessRoutingKey, event);
    }

    public void publishTaskGenerationFailed(TaskGenerationFailedEvent event) {
        log.warn("Publishing task generation FAILED for user: {}", event.getUserId());
        rabbitTemplate.convertAndSend(taskReplyExchange, taskFailedRoutingKey, event);
    }
}