package com.amalitech.task.service.events;

import com.amalitech.common.event.events.UserOnboardingCompletedEvent;
import com.amalitech.task.service.service.TaskGenerationListener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

/**
 * Consumer for user onboarding events that triggers personalized task generation.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserOnboardingEventConsumer {

    private final TaskGenerationListener taskGenerationListener;

    @RabbitListener(queues = "user.onboarding.task_service.q")
    public void handleOnboardingCompleted(UserOnboardingCompletedEvent event) {
        log.info("Processing onboarding completed event for user: {} with {} skills",
                event.getUserId(), event.getSelectedSkills().size());

        try {
            taskGenerationListener.generateTasksAfterOnboarding(event);

            log.info("Successfully delegated task generation for user onboarding: {}", event.getUserId());

        } catch (Exception e) {
            log.error("Failed to delegate task generation for onboarding event: {}", event.getUserId(), e);
            throw new AmqpRejectAndDontRequeueException("Task generation delegation failed", e);
        }
    }
}