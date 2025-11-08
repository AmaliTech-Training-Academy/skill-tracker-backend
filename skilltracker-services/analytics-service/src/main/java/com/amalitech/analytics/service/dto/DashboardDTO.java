package com.amalitech.analytics.service.dto;


import java.util.List;


public record DashboardDTO(
        UserStatsDTO userStats,
        List<SkillProgressDTO> skillProgress,
        List<GoalStatusDTO> goalStatus,
        List<SkillGapDTO> skillGaps,
        List<RecommendationDTO> recommendations,
        GlobalRankDTO globalRank
) {}
