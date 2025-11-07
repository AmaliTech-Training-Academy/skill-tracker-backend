package com.amalitech.analytics.service.events;

import org.springframework.context.ApplicationEvent;
import java.util.UUID;

/**
 * Application event published when analytics data for a specific user
 * has been successfully committed to the database.
 * <p>
 * This event can be listened to by any Spring-managed component that
 * implements an {@link org.springframework.context.ApplicationListener}
 * or uses the {@link org.springframework.context.event.EventListener} annotation.
 * </p>
 *
 * <p><strong>Typical Use Cases:</strong></p>
 * <ul>
 *   <li>Invalidating cached analytics data after updates.</li>
 *   <li>Triggering downstream notification or reporting workflows.</li>
 *   <li>Synchronizing analytics data across distributed services.</li>
 * </ul>
 *
 * <p>The event carries the {@code userId} of the affected user so that
 * listeners can handle only relevant updates.</p>
 *
 * <pre>{@code
 * @EventListener
 * public void handleAnalyticsUpdate(AnalyticsUpdateEvent event) {
 *     UUID updatedUserId = event.getUserId();
 *     cacheService.evictUserDashboard(updatedUserId);
 * }
 * }</pre>
 *
 * @see org.springframework.context.ApplicationEventPublisher
 * @see org.springframework.context.event.EventListener
 * @since 1.0
 */
public class AnalyticsUpdateEvent extends ApplicationEvent {

    private final UUID userId;

    /**
     * Constructs a new {@code AnalyticsUpdateEvent}.
     *
     * @param source the object on which the event initially occurred (never {@code null})
     * @param userId the unique identifier of the user whose analytics data was updated
     */
    public AnalyticsUpdateEvent(Object source, UUID userId) {
        super(source);
        this.userId = userId;
    }

    /**
     * Returns the unique identifier of the user associated with this event.
     *
     * @return the affected user's {@link UUID}
     */
    public UUID getUserId() {
        return userId;
    }
}

