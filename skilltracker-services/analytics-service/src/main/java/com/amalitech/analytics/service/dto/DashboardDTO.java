package com.amalitech.analytics.service.dto;


import java.util.List;

// The top-level response object for the main dashboard API call
public record DashboardDTO(
        UserStatsDTO userStats,
        List<SkillProgressDTO> skillProgress,
        List<GoalStatusDTO> goalStatus, // Stubbed for completeness
        List<SkillGapDTO> skillGaps,    // Stubbed for completeness
        GlobalRankDTO globalRank        // Stubbed for completeness
) {}
