package com.amalitech.analytics.service.service;

import com.amalitech.analytics.service.dto.DashboardDTO;
import com.amalitech.analytics.service.events.AnalyticsUpdateEvent;
import com.amalitech.analytics.service.service.interfaces.AnalyticsNotificationServiceInterface;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * Service responsible for dispatching real-time analytics updates to connected WebSocket clients
 * after successful database transactions. Includes a per-user debounce mechanism to prevent redundant
 * or excessive message pushes when multiple updates occur in rapid succession.
 *
 * <p><strong>Debounce Mechanism:</strong></p>
 * <ul>
 *   <li>When multiple {@link AnalyticsUpdateEvent}s for the same user are published within a short time window,
 *       only one WebSocket notification is sent.</li>
 *   <li>The debounce interval is controlled by {@link #DEBOUNCE_DELAY}.</li>
 *   <li>This approach minimizes unnecessary network usage and client-side rendering operations.</li>
 * </ul>
 *
 * <p><strong>Execution Flow:</strong></p>
 * <ol>
 *   <li>An analytics update triggers an {@link AnalyticsUpdateEvent}.</li>
 *   <li>The event is handled <em>after</em> the surrounding transaction commits successfully.</li>
 *   <li>The affected user’s dashboard is rebuilt using {@link AnalyticsReadService}.</li>
 *   <li>The updated dashboard is pushed to that user’s private WebSocket destination.</li>
 * </ol>
 *
 * @see AnalyticsUpdateEvent
 * @see AnalyticsReadService
 * @see SimpMessagingTemplate
 * @since 1.1
 */
@Slf4j
@Service
public class AnalyticsNotificationService implements AnalyticsNotificationServiceInterface {

    /**
     * Defines the debounce delay duration (in milliseconds).
     * Any subsequent updates for the same user within this window reset the timer.
     */
    private static final Duration DEBOUNCE_DELAY = Duration.ofMillis(500);

    private final SimpMessagingTemplate messagingTemplate;
    private final AnalyticsReadService analyticsReadService;

    /**
     * A thread-safe scheduler used to delay dashboard updates per user.
     * Uses daemon threads for lightweight background execution.
     */
    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(2, runnable -> {
                Thread thread = new Thread(runnable);
                thread.setDaemon(true);
                thread.setName("analytics-notifier-scheduler");
                return thread;
            });

    /**
     * A concurrent map tracking pending scheduled update tasks for each user.
     * Prevents duplicate notifications within the debounce window.
     */
    private final Map<UUID, ScheduledFuture<?>> scheduledNotifications = new ConcurrentHashMap<>();

    /**
     * Constructs the notification service with required dependencies.
     *
     * @param messagingTemplate     a Spring {@link SimpMessagingTemplate} used to send STOMP messages to clients
     * @param analyticsReadService  service for generating {@link DashboardDTO} objects representing the current state
     */
    public AnalyticsNotificationService(SimpMessagingTemplate messagingTemplate,
                                        AnalyticsReadService analyticsReadService) {
        this.messagingTemplate = messagingTemplate;
        this.analyticsReadService = analyticsReadService;
    }

    /**
     * Handles {@link AnalyticsUpdateEvent}s after the surrounding transaction successfully commits.
     * Implements a debounce strategy to coalesce multiple updates for the same user into a single WebSocket push.
     *
     * @param event the analytics update event containing the affected user identifier
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAnalyticsUpdate(AnalyticsUpdateEvent event) {
        UUID userId = event.getUserId();
        log.debug("Received analytics update event for user {}", userId);

        ScheduledFuture<?> existing = scheduledNotifications.get(userId);
        if (existing != null && !existing.isDone()) {
            existing.cancel(false);
        }

        ScheduledFuture<?> future = scheduler.schedule(
                () -> sendDashboardUpdate(userId),
                DEBOUNCE_DELAY.toMillis(),
                TimeUnit.MILLISECONDS
        );

        scheduledNotifications.put(userId, future);
    }

    /**
     * Builds the latest dashboard for the specified user and sends it through
     * a dedicated WebSocket destination.
     *
     * @param userId the unique identifier of the user receiving the update
     */
    public void sendDashboardUpdate(UUID userId) {
        try {
            DashboardDTO updatedDashboard = analyticsReadService.buildDashboard(userId);
            String destination = "/queue/dashboard";

            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    destination,
                    updatedDashboard
            );

            log.info("Sent debounced dashboard update to user {}", userId);
        } catch (Exception ex) {
            log.error("Failed to send dashboard update for user {}", userId, ex);
        } finally {
            scheduledNotifications.remove(userId);
        }
    }
}


