package com.amalitech.analytics.service.dto;

public record SkillGapDTO(
        String rubric,
        double averageScore,
        String description
) {}