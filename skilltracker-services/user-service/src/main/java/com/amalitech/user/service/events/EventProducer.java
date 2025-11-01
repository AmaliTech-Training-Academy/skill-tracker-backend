package com.amalitech.user.service.events;

import com.amalitech.common.event.events.UserOnboardingCompletedEvent;

/**
 * Interface for publishing events from the User Service.
 */
public interface EventProducer {

    /**
     * Publishes an event when a user completes onboarding with skill selections.
     * This triggers personalized task generation in the task service.
     */
    void publishOnboardingCompleted(UserOnboardingCompletedEvent event);
}
