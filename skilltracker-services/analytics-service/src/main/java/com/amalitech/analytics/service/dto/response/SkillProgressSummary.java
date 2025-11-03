package com.amalitech.analytics.service.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class SkillProgressSummary {
    private UUID skillId;
    private String skillName;
    private Double currentScore;
    private List<DataPoint> historicalData;
}