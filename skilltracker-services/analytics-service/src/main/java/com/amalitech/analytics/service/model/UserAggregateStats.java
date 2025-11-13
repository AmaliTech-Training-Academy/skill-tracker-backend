package com.amalitech.analytics.service.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "user_aggregate_stats")
@Data
@NoArgsConstructor
/**
 Represents the aggregate statistics for a user, tracking total tasks and streaks.
 */
public class UserAggregateStats {
    @Id
    private UUID userId;
    private Integer totalTasksCompleted = 0;
    private Integer currentStreakInDays = 0;
    private Integer longestStreakInDays = 0;
    private LocalDate lastPracticeDate;
    public UserAggregateStats(UUID userId) {
        this.userId = userId;
    }
    /**
     Updates the user's streak and total tasks based on the practice date.
     Handles first-day, consecutive-day, and streak reset scenarios.

     @param practiceDate The date the user completed a task.
     */
    public void updateStreak(LocalDate practiceDate) {
        if (practiceDate.equals(lastPracticeDate)) {
            return;
        }
        if (lastPracticeDate != null && practiceDate.equals(lastPracticeDate.plusDays(1))) {
            currentStreakInDays++;
        } else {
            currentStreakInDays = 1;
        }
        if (currentStreakInDays > longestStreakInDays) {
            longestStreakInDays = currentStreakInDays;
        }
        lastPracticeDate = practiceDate;
    }

    public void incrementTasksCompleted() {
        this.totalTasksCompleted++;
    }
}
