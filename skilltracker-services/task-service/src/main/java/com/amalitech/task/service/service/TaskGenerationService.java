package com.amalitech.task.service.service;

import com.amalitech.common.event.events.UserOnboardingCompletedEvent;
import com.amalitech.task.service.dto.request.BatchGenerationRequest;
import com.amalitech.task.service.dto.request.GenerateTaskRequest;

/**
 * Service interface for handling all task generation business logic.
 *
 * This contract is implemented by @Async and @Transactional workers
 * and is called by listeners/adapters.
 */
public interface TaskGenerationService {

    /**
     * Asynchronously processes a batch generation request.
     * Includes Redis-based distributed locking.
     */
    void processBatchGeneration(BatchGenerationRequest request);

    /**
     * Asynchronously processes an admin-initiated generation request.
     */
    void processAdminGeneration(GenerateTaskRequest request);

    /**
     * Asynchronously generates the initial set of tasks for a
     * newly onboarded user.
     */
    void generateTasksAfterOnboarding(UserOnboardingCompletedEvent event);
}