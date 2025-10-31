package com.amalitech.user.service.controller;


import com.amalitech.user.service.dto.request.OnboardingRequest;
import com.amalitech.user.service.security.CustomUserDetails;
import com.amalitech.user.service.service.OnboardingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final OnboardingService onboardingService;

    public UserController(OnboardingService onboardingService) {
        this.onboardingService = onboardingService;
    }

    @PostMapping("/complete-onboarding")
    public ResponseEntity<Void> completeOnboarding(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody OnboardingRequest request
    ) {
        onboardingService.completeOnboarding(userDetails.getUser().getId(), request);
        return ResponseEntity.ok().build();
    }
}