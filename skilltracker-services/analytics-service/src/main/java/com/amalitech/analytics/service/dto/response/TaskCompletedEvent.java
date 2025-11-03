package com.amalitech.analytics.service.dto.response;

import lombok.Data;

import java.util.Map;
import java.util.UUID;

/**
 * DTO for the incoming event from Task Service.
 */
@Data
public class TaskCompletedEvent {
    private UUID userId;
    private UUID taskId;
    private UUID skillId;
    private Double score;
    private String taskType; // +++ NEW FIELD: For Enum mapping +++
    private Map<String, Double> rubricsScores; // +++ NEW FIELD: JSON data +++
}