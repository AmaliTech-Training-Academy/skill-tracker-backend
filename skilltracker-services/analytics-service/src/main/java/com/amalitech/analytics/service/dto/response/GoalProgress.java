package com.amalitech.analytics.service.dto.response;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GoalProgress {
    private String goalDescription;
    private Double currentScore;
    private Double targetScore;
    private Double percentageComplete; // Calculated by the service
}