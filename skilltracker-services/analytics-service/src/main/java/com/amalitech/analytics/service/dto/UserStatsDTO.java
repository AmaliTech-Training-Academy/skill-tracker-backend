package com.amalitech.analytics.service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

/**
 DTO representing the user's aggregate statistics for API or dashboard purposes.

 @param totalTasksCompleted Total number of tasks the user has completed.
 @param currentStreakInDays Number of consecutive days the user has practiced.
 @param longestStreakInDays Longest streak the user has ever achieved.
 @param lastPracticeDate Date of the last practice.
 */
public record UserStatsDTO(
        Integer totalTasksCompleted,
        Integer currentStreakInDays,
        Integer longestStreakInDays,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate lastPracticeDate
) {}