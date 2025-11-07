package com.amalitech.analytics.service.service.interfaces;

import com.amalitech.analytics.service.dto.TaskCompletedEvent;
import com.amalitech.analytics.service.dto.TaskSubmissionRequestDTO;

/**
 * Defines the contract for processing analytics-related updates,
 * including task completions and direct submissions.
 *
 * <p>Implementations should handle persistence, data aggregation,
 * and event publishing in a transactional and idempotent manner.</p>
 *
 * @since 1.0
 */
public interface AnalyticsServiceInterface {

    /**
     * Processes a completed task event, updating skill progress,
     * user statistics, and related trajectory snapshots.
     *
     * @param event the completed task event to process
     */
    void processTaskCompletion(TaskCompletedEvent event);

    /**
     * Processes a task submission received directly from an API request,
     * bypassing the message queue.
     *
     * @param request DTO representing the direct submission request
     */
    void submitTaskDirectly(TaskSubmissionRequestDTO request);
}

