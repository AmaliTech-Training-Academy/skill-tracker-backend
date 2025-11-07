package com.amalitech.analytics.service.service.interfaces;

import com.amalitech.analytics.service.events.AnalyticsUpdateEvent;

import java.util.UUID;

/**
 * Defines the contract for dispatching analytics update notifications
 * to connected clients or downstream systems.
 *
 * <p>This interface provides a consistent API for broadcasting analytics
 * updates while allowing multiple implementations (e.g., WebSocket,
 * message broker, or email notification strategies).</p>
 *
 * <p><strong>Design Goals:</strong></p>
 * <ul>
 *   <li>Promote dependency inversion by referencing this interface in other services.</li>
 *   <li>Enable easy mocking during unit testing.</li>
 *   <li>Allow replacement or extension (e.g., push notifications, async queues).</li>
 * </ul>
 *
 * @since 1.0
 */
public interface AnalyticsNotificationServiceInterface {

    /**
     * Handles analytics update events triggered after successful database commits.
     * Implementations are responsible for deciding when and how to push updates.
     *
     * @param event the analytics update event containing the affected user ID
     */
    void handleAnalyticsUpdate(AnalyticsUpdateEvent event);

    /**
     * Sends the latest dashboard data to the specified user.
     * Implementations may debounce, throttle, or batch updates.
     *
     * @param userId the unique identifier of the user to notify
     */
    void sendDashboardUpdate(UUID userId);
}
