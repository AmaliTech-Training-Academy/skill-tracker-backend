package com.amalitech.analytics.service.model;


import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "skill_trajectory_snapshots",
        indexes = {@Index(columnList = "userId, skillId, snapshotDate", unique = true)})
@Data
@NoArgsConstructor
/**
 Represents a daily aggregation (delta) of a user's skill activity.
 */
public class SkillTrajectorySnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private java.util.UUID id;
    private UUID userId;
    private UUID skillId;
    private LocalDate snapshotDate;

    private Integer xpEarnedToday = 0;
    private Integer tasksCompletedToday = 0;
    private Double averageXpEarned = 0.0;

    /**
     * Updates the daily record based on a new task completion.
     * This method assumes the entity has already been found or newly created.
     * @param xpEarned The XP from the latest task submission.
     */
    public void recordTaskCompletion(int xpEarned) {
        this.tasksCompletedToday++;
        this.xpEarnedToday += xpEarned;
        this.recalculateAverageXp();
    }


    private void recalculateAverageXp(){
        if (this.tasksCompletedToday > 0) {
            this.averageXpEarned = (double) this.xpEarnedToday / this.tasksCompletedToday;
        } else {
            this.averageXpEarned = 0.0;
        }
    }


    public static SkillTrajectorySnapshot createNew(UUID userId, UUID skillId, LocalDate date, int xpEarned) {
        SkillTrajectorySnapshot snapshot = new SkillTrajectorySnapshot();
        snapshot.setUserId(userId);
        snapshot.setSkillId(skillId);
        snapshot.setSnapshotDate(date);
        snapshot.recordTaskCompletion(xpEarned);
        return snapshot;
    }
}
