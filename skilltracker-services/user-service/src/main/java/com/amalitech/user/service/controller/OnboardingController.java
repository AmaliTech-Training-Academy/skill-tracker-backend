package com.amalitech.user.service.controller;

import com.amalitech.common.security.dto.response.ApiResponse;
import com.amalitech.user.service.dto.request.OnboardingRequest;
import com.amalitech.user.service.dto.response.OnboardingResponseDTO;
import com.amalitech.user.service.security.CustomUserDetails;
import com.amalitech.user.service.service.OnboardingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for handling user onboarding processes.
 * This controller provides endpoints for users to complete their initial setup,
 * such as selecting skills and setting proficiency levels.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;

    /**
     * Completes the onboarding process for an authenticated user.
     * This endpoint allows a user to submit their initial skill selections and levels,
     * which marks their onboarding as complete.
     *
     * @param userDetails The authenticated user's details, providing access to their ID.
     * @param request     The onboarding request containing the user's skill selections.
     * @return A ResponseEntity indicating the success of the onboarding completion.
     */
    @PostMapping("/complete-onboarding")
    public ResponseEntity<ApiResponse<OnboardingResponseDTO>> completeOnboarding(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody OnboardingRequest request
    ) {
        OnboardingResponseDTO response = onboardingService.completeOnboarding(
                userDetails.getUser().getId(), request
        );

        return ResponseEntity.ok(ApiResponse.success(
                "Onboarding completed successfully. Task generation is in progress...",
                response,
                null
        ));
    }
}