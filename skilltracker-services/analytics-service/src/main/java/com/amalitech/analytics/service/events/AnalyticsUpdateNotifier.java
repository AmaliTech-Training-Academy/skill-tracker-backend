//package com.amalitech.analytics.service.events;
//
//import com.amalitech.analytics.service.dto.DashboardDTO;
//import com.amalitech.analytics.service.service.AnalyticsReadService;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.context.event.EventListener;
//import org.springframework.messaging.simp.SimpMessagingTemplate;
//import org.springframework.scheduling.annotation.Async;
//import org.springframework.stereotype.Component;
//
//import java.util.UUID;
//
///**
// * Asynchronous event listener responsible for notifying WebSocket/STOMP clients
// * when analytics data changes.
// * <p>
// * When an {@link AnalyticsUpdateEvent} is published (typically after analytics
// * data is updated in the database), this listener:
// * <ul>
// *   <li>Asynchronously fetches the latest user analytics data from the {@link AnalyticsReadService}.</li>
// *   <li>Constructs an {@link UpdatePayload} message describing the update.</li>
// *   <li>Pushes the payload to the user’s STOMP destination using {@link SimpMessagingTemplate}.</li>
// * </ul>
// *
// * <p>This ensures that UI dashboards reflect the latest user statistics in real time
// * without blocking the publishing thread.</p>
// *
// * <p><strong>Note:</strong> To enable asynchronous event handling, ensure a configuration
// * class includes the annotation {@code @EnableAsync}.</p>
// *
// * @see AnalyticsUpdateEvent
// * @see org.springframework.messaging.simp.SimpMessagingTemplate
// * @see org.springframework.scheduling.annotation.EnableAsync
// * @since 1.0
// */
//@Component
//@Slf4j
//public class AnalyticsUpdateNotifier {
//
//    private final SimpMessagingTemplate messagingTemplate;
//    private final AnalyticsReadService analyticsReadService;
//
//    /**
//     * Constructs an {@code AnalyticsUpdateNotifier}.
//     *
//     * @param messagingTemplate     the messaging template used for sending STOMP messages
//     * @param analyticsReadService  the service used to retrieve updated analytics data
//     */
//    public AnalyticsUpdateNotifier(SimpMessagingTemplate messagingTemplate,
//                                   AnalyticsReadService analyticsReadService) {
//        this.messagingTemplate = messagingTemplate;
//        this.analyticsReadService = analyticsReadService;
//    }
//
//    /**
//     * Handles {@link AnalyticsUpdateEvent} asynchronously.
//     * <p>
//     * When invoked, this method re-fetches the user's latest analytics data and sends
//     * a STOMP message containing the update to the appropriate user destination.
//     * </p>
//     *
//     * <p>The asynchronous execution ensures that the publishing thread (usually
//     * in the {@code AnalyticsService}) is not blocked by message delivery operations.</p>
//     *
//     * @param event the {@link AnalyticsUpdateEvent} containing the affected user's ID
//     */
//    @Async
//    @EventListener
//    public void handleAnalyticsUpdate(AnalyticsUpdateEvent event) {
//        UUID userId = event.getUserId();
//        log.info("Received analytics update event for user {}. Processing asynchronously.", userId);
//
//        DashboardDTO updatedStats = analyticsReadService.buildDashboard(userId);
//
//        UpdatePayload payload = new UpdatePayload(
//                "USER_STATS_UPDATED",
//                updatedStats
//        );
//
//        String destination = "/queue/dashboard-updates";
//        messagingTemplate.convertAndSendToUser(
//                userId.toString(),
//                destination,
//                payload
//        );
//
//        log.info("STOMP message sent to user {} at destination {}", userId, destination);
//    }
//
//    /**
//     * Immutable payload structure used for WebSocket/STOMP message delivery.
//     * <p>
//     * The {@code type} field indicates the update type, while {@code data}
//     * holds the updated analytics information.
//     * </p>
//     *
//     * @param type a descriptive update type identifier (e.g., {@code USER_STATS_UPDATED})
//     * @param data the updated {@link DashboardDTO} data payload
//     */
//    public record UpdatePayload(String type, DashboardDTO data) {}
//}


