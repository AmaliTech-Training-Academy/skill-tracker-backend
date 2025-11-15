package com.amalitech.analytics.service.dto;

import java.io.Serializable;
import java.util.UUID;

public record UserRubricStatsId(UUID userId, String rubric) implements Serializable {}
