package com.amalitech.user.service.dto.response;

import com.amalitech.user.service.model.enums.GuidedTourStatus;
import com.amalitech.user.service.model.enums.PremiumTier;
import com.amalitech.user.service.model.enums.TaskGenerationStatus;
import com.amalitech.user.service.model.enums.UserState;

import java.util.UUID;

/**
 * This DTO is returned to the client after a successful onboarding.
 * It provides the client with the user's new state, premium tier,
 * tour status, and the initial status of their task generation.
 *
 */
public record OnboardingResponseDTO(
        UUID id,
        String email,
        UserState state,
        TaskGenerationStatus taskGenerationStatus,
        GuidedTourStatus tourStatus,
        PremiumTier premiumTier
) {
}
