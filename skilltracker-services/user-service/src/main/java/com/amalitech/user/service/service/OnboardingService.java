package com.amalitech.user.service.service;

import com.amalitech.user.service.dto.request.OnboardingRequest;
import com.amalitech.user.service.dto.response.OnboardingResponseDTO;

import java.util.UUID;

/**
 * Service interface for handling user onboarding logic.
 * Defines the contract for completing the user's initial setup.
 */
public interface OnboardingService {

    /**
     * Completes the onboarding process for a user by associating them
     * with their selected skills and updating their account state.
     *
     * @param userId The UUID of the authenticated user.
     * @param request The DTO containing the list of selected skills and levels.
     *
     * @return The updated User entity with the new state.
     */
    OnboardingResponseDTO completeOnboarding(UUID userId, OnboardingRequest request);

    /**
     * Re-triggers the task generation process for a user whose
     * initial task generation failed.
     *
     * @param userId The ID of the user to retry for.
     * @throws IllegalStateException if the user's task status is not FAILED.
     */
    void retryTaskGeneration(UUID userId);
}