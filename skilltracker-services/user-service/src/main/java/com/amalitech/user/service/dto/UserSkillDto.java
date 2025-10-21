package com.amalitech.user.service.dto;

import com.amalitech.user.service.model.enums.DifficultyLevel;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public record UserSkillDto(UUID skillId, String skillName, DifficultyLevel difficultyLevel, LocalDateTime selectedAt) {}
