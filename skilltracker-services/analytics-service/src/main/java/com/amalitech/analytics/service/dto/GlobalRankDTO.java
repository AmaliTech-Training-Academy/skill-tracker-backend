package com.amalitech.analytics.service.dto;

import java.util.UUID;

public record GlobalRankDTO(UUID skillId, String skillName, Double userProficiency, Double p50Proficiency, Double p90Proficiency) {}