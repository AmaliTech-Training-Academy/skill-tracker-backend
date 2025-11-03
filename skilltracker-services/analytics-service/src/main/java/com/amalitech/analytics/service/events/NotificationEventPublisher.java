package com.amalitech.analytics.service.events;


import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class NotificationEventPublisher {
    /**
     * Publishes a milestone event (AC 8).
     */
    public void publishMilestoneEvent(UUID userId, String title, String message) {

        System.out.printf("PUBLISHING EVENT: User %d -> %s: %s%n", userId, title, message);
    }
}