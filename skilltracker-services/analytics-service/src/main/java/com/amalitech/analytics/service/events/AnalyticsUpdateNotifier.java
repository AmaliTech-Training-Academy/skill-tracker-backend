package com.amalitech.analytics.service.events;

import com.amalitech.analytics.service.dto.DashboardDTO;
import com.amalitech.analytics.service.service.AnalyticsReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;



@Component
@Slf4j
public class AnalyticsUpdateNotifier {

    private final SimpMessagingTemplate messagingTemplate;
    private final AnalyticsReadService analyticsReadService;

    public AnalyticsUpdateNotifier(SimpMessagingTemplate messagingTemplate,
                                   AnalyticsReadService analyticsReadService) {
        this.messagingTemplate = messagingTemplate;
        this.analyticsReadService = analyticsReadService;
    }

    /**
     * Listens for the internal domain event published by AnalyticsService.
     */
    @EventListener
    public void handleAnalyticsUpdate(AnalyticsUpdateEvent event) {
        UUID userId = event.getUserId();
        log.info("Analytics updated for user {}. Preparing STOMP push.", userId);

        // --- Granular Payload Strategy ---
        // Instead of sending a generic "refresh" message, we send the
        // specific data that changed. Here, we'll re-fetch the user's stats,
        // as that's the most likely thing to have changed (e.g., streak).

        // Note: This uses your existing ReadService. This is fine if getUserStats
        // is fast (e.g., hits Redis or a simple aggregate table),
        // which your code implies it is.
        DashboardDTO updatedStats = analyticsReadService.buildDashboard(userId);

        // Define a clear, granular payload
        UpdatePayload payload = new UpdatePayload(
                "USER_STATS_UPDATED",
                updatedStats
        );

        String destination = "/queue/dashboard-updates";
        messagingTemplate.convertAndSendToUser(
                userId.toString(),
                destination,
                payload
        );

        log.info("STOMP message sent to user {} at destination {}", userId, destination);
    }

    // A simple DTO for our STOMP message payload
    record UpdatePayload(String type, DashboardDTO data) {}

    // You might need to re-define or import UserStatsDTO if it's not public
    // record UserStatsDTO(int totalTasks, int currentStreak, int longestStreak, LocalDate lastPractice) {}
}
