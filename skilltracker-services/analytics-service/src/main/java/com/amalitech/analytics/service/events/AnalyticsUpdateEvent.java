package com.amalitech.analytics.service.events;

import org.springframework.context.ApplicationEvent;
import java.util.UUID;

/**
 * An internal Spring event fired after analytics data is successfully
 * committed to the database.
 */
public class AnalyticsUpdateEvent extends ApplicationEvent {
    private final UUID userId;

    public AnalyticsUpdateEvent(Object source, UUID userId) {
        super(source);
        this.userId = userId;
    }

    public UUID getUserId() {
        return userId;
    }
}
