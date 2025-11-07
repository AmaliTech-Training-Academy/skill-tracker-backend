package com.amalitech.analytics.service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

/**
 DTO representing a point in the skill trajectory chart.

 @param snapshotDate The date of the snapshot.
 @param averageXpEarned The average XP score for the skill on that date.
 @param tasksCompletedUpToDate The cumulative number of tasks completed up to that date.
 */
public record TrajectoryPointDTO(
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate snapshotDate,
        Double averageXpEarned,
        Integer tasksCompletedUpToDate
) {}