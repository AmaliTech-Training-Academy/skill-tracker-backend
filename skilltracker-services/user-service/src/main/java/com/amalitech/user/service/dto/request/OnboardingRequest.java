package com.amalitech.user.service.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Data Transfer Object for user onboarding requests.
 * Contains the list of skills selected by the user during registration.
 *
 * @param skills the list of skill selections made by the user
 */
public record OnboardingRequest(
        @NotEmpty
        List<SkillSelection> skills
) {}