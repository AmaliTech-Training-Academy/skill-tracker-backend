package com.amalitech.user.service.dto.response;

import java.util.UUID;

/**
 * Data Transfer Object for safely exposing Skill information to the frontend.
 * Hides internal data like XP maps and supported task types.
 */
public record SkillResponseDto(
        UUID id,
        String name,
        String category,
        String iconUrl
) {}