//package com.amalitech.analytics.service.events;
//
//import com.amalitech.analytics.service.dto.TaskCompletedEvent;
//import com.amalitech.analytics.service.service.AnalyticsService;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.amqp.rabbit.annotation.RabbitListener;
//import org.springframework.stereotype.Component;
//
//@Slf4j
//@Component
//public class AnalyticsEventListener {
//
//    public static final String TASK_COMPLETED_QUEUE = "q.task.completed";
//    private final AnalyticsService analyticsService;
//
//    public AnalyticsEventListener(AnalyticsService analyticsService) {
//        this.analyticsService = analyticsService;
//    }
//
//    @RabbitListener(queues = TASK_COMPLETED_QUEUE)
//    public void onTaskCompleted(TaskCompletedEvent event) {
//        try {
//            analyticsService.processTaskCompletion(event);
//        } catch (Exception e) {
//            // BEST PRACTICE: Catch exception to prevent message retry loop.
//            // Logs the error and lets the message be acknowledged, pushing it to a Dead-Letter Queue (DLQ).
//            log.error("Failed to process event for user {}. Sending to DLQ.", event.userId(), e);
//        }
//    }
//}
