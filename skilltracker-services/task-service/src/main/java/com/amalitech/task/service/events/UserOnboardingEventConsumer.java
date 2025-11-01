package com.amalitech.task.service.events;

import com.amalitech.common.event.events.UserOnboardingCompletedEvent;
import com.amalitech.task.service.service.TaskGenerationService;
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

    private final TaskGenerationService taskGenerationService;

    @RabbitListener(queues = "user.onboarding.task_service.q")
    public void handleOnboardingCompleted(UserOnboardingCompletedEvent event) {
        log.info("Processing onboarding completed event for user: {} with {} skills",
                event.getUserId(), event.getSelectedSkills().size());

        try {
            taskGenerationService.generateTasksAfterOnboarding(event);
            log.info("Successfully generated tasks for user onboarding: {}", event.getUserId());

        } catch (Exception e) {
            log.error("Failed to generate tasks for onboarding event: {}", event.getUserId(), e);
            throw new AmqpRejectAndDontRequeueException("Task generation failed", e);
        }
    }
}
