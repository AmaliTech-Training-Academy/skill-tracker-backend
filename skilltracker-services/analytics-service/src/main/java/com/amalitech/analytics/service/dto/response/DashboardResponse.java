package com.amalitech.analytics.service.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;


@Data
@Builder
public class DashboardResponse {
    private List<SkillProgressSummary> skillSummaries;
    private List<GoalProgress> goalProgress;
    private List<Recommendation> recommendations;
}