package com.amalitech.analytics.service.service;

import com.amalitech.analytics.service.dto.DashboardDTO;
import com.amalitech.analytics.service.events.AnalyticsUpdateEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import java.util.UUID;

@Service
public class AnalyticsNotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final AnalyticsReadService analyticsReadService;

    public AnalyticsNotificationService(SimpMessagingTemplate messagingTemplate,
                                        AnalyticsReadService analyticsReadService) {
        this.messagingTemplate = messagingTemplate;
        this.analyticsReadService = analyticsReadService;
    }

    /**
     * Listens for our internal event *only after* the database
     * transaction has successfully committed.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAnalyticsUpdate(AnalyticsUpdateEvent event) {
        UUID userId = event.getUserId();

        // 1. Build the fresh Dashboard DTO using the service that already knows how.
        DashboardDTO updatedDashboard = analyticsReadService.buildDashboard(userId);

        // 2. Define the user-specific destination
        String destination = "/queue/dashboard";

        // 3. Push the new dashboard state to the specific user
        messagingTemplate.convertAndSendToUser(
                userId.toString(),
                destination,
                updatedDashboard
        );
    }
}
