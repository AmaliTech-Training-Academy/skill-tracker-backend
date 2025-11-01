package com.amalitech.user.service.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record OnboardingRequest(
        @NotEmpty
        List<SkillSelection> skills
) {}