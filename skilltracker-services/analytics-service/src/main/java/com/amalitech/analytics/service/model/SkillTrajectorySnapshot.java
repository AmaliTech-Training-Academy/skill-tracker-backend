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
 Represents a daily snapshot of a user's skill trajectory.
 */
public class SkillTrajectorySnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private java.util.UUID id;

    private UUID userId;
    private UUID skillId;

    private LocalDate snapshotDate;
    private Double averageXpEarned;
    private Integer tasksCompletedUpToDate;


    /**
     Creates a snapshot from a user's current progress for a given date.

     @param progress The user's skill progress.
     @param date The snapshot date.
     @return A new SkillTrajectorySnapshot instance.
     */
    public static SkillTrajectorySnapshot fromProgress(UserSkillProgress progress, LocalDate date) {
        SkillTrajectorySnapshot snapshot = new SkillTrajectorySnapshot();
        snapshot.setUserId(progress.getUserId());
        snapshot.setSkillId(progress.getSkillId());
        snapshot.setSnapshotDate(date);
        snapshot.setAverageXpEarned(progress.getAverageXpEarned());
        snapshot.setTasksCompletedUpToDate(progress.getTasksCompleted());
        return snapshot;
    }
}
