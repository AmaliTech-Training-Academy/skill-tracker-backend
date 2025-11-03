package com.amalitech.user.service.service;

import com.amalitech.user.service.dto.request.OnboardingRequest;

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
     */
    void completeOnboarding(UUID userId, OnboardingRequest request);
}