package com.amalitech.user.service.events;

import com.amalitech.common.event.events.SkillEvent;
import com.amalitech.common.event.events.UserOnboardingCompletedEvent;
import com.amalitech.user.service.config.RabbitMQConfig;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * RabbitMQ implementation of EventProducer for publishing user-related events.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RabbitMQEventProducer implements EventProducer {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publishOnboardingCompleted(UserOnboardingCompletedEvent event) {
        log.info("Publishing onboarding completed event for user: {} with {} skills",
                event.getUserId(), event.getSelectedSkills().size());

        CorrelationData correlationData = new CorrelationData(event.getUserId().toString() + ":" + System.currentTimeMillis());

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.USER_EXCHANGE,
                    RabbitMQConfig.ONBOARDING_COMPLETED_ROUTING_KEY,
                    event,
                    correlationData
            );
        } catch (Exception e) {
            log.error(
                    "CRITICAL: Failed to publish UserOnboardingCompletedEvent for user {} after all retries.",
                    event.getUserId(),
                    e
            );
            throw new RuntimeException("Event publish failed catastrophically after all retries.", e);
        }
    }

    @Override
    public void publishSkillEvent(SkillEvent event) {
        log.info("Publishing {} event for skill: {}", event.getEventType(), event.getSkillId());

        CorrelationData correlationData = new CorrelationData("skill:" + event.getSkillId() + ":" + System.currentTimeMillis());

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.USER_EXCHANGE,
                    "skill." + event.getEventType().name().toLowerCase(),
                    event,
                    correlationData
            );
        } catch (Exception e) {
            log.error(
                    "CRITICAL: Failed to publish SkillEvent for skill {} after all retries.",
                    event.getSkillId(),
                    e
            );
            throw new RuntimeException("Skill event publish failed catastrophically after all retries.", e);
        }
    }
}