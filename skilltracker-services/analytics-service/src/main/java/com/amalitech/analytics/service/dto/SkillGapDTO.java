package com.amalitech.analytics.service.dto;

public record SkillGapDTO(
        String rubric,
        double score_percentage,
        String description
) {}