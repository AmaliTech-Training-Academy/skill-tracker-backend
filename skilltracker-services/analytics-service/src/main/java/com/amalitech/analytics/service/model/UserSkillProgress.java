package com.amalitech.analytics.service.model;

import com.amalitech.analytics.service.dto.TaskCompletedEvent;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_skill_progress",
        indexes = {@Index(columnList = "userId, skillId", unique = true)})
@Data
@NoArgsConstructor
/**
 Represents the skill progress of a user.
 */
public class UserSkillProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private UUID skillId;

    private Integer tasksCompleted = 0;
    private Integer tasksSubmitted = 0;
    private Integer tasksFailed = 0;
    private Integer totalXpEarned = 0;
    private Double averageXpEarned = 0.0;
    private Double proficiency = 0.0;
    private Instant lastPracticedAt;

    /**
     Updates the user's skill progress by adding earned XP, incrementing tasks completed,
     recalculating the average XP, and setting the last practiced timestamp.

     @param event XP earned from the completed task.
     */
    public void updateProgress(TaskCompletedEvent event, Double proficiency) {
        this.tasksSubmitted++;

        if (event.passed()) {
            this.tasksCompleted++;
        }

        if (!event.passed()) {
            this.tasksFailed++;
        }

        this.totalXpEarned += event.totalXpEarned();
        this.proficiency = proficiency;

        if (this.tasksCompleted > 0) {
            this.averageXpEarned = (double) this.totalXpEarned / this.tasksCompleted;
        } else {
            this.averageXpEarned = 0.0;
        }

        this.lastPracticedAt = Instant.now();
    }
}