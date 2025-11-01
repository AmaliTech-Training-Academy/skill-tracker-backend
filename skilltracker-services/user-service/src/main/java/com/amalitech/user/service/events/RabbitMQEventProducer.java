package com.amalitech.user.service.events;

import com.amalitech.common.event.events.UserOnboardingCompletedEvent;
import com.amalitech.user.service.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

        rabbitTemplate.convertAndSend(
            RabbitMQConfig.USER_EXCHANGE,
            RabbitMQConfig.ONBOARDING_COMPLETED_ROUTING_KEY,
            event
        );
    }
}
