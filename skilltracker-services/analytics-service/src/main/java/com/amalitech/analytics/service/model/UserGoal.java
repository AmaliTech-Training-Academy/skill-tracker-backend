package com.amalitech.analytics.service.model;

import com.amalitech.analytics.service.model.enums.GoalStatus;
import com.amalitech.analytics.service.model.enums.GoalType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Represents a user-defined, stateful goal for a specific skill.
 * Tracks initial, current, and target values for accurate progress calculation.
 */
@Entity
@Table(name = "user_goals", indexes = {
        @Index(name = "idx_usergoal_user_status", columnList = "userId, status")
})
@Getter
@Setter
@NoArgsConstructor
public class UserGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private UUID skillId;

    /**
     * Denormalized skill name for easier display in goal lists.
     */
    @Column(nullable = false)
    private String skillName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GoalType goalType;

    /**
     * The target value for the goal (e.g., target XP, target task count).
     */
    @Column(nullable = false)
    private Integer targetValue;

    /**
     * The value of the metric when the goal was created.
     */
    @Column(nullable = false)
    private Integer initialValue;

    /**
     * The current value of the metric, updated by AnalyticsService.
     */
    @Column(nullable = false)
    private Integer currentValue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GoalStatus status = GoalStatus.ACTIVE;

    /**
     * Optional target date for goal completion.
     */
    private LocalDate deadline;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    private Instant completedAt;

    public UserGoal(UUID userId, UUID skillId, String skillName, GoalType goalType,
                    Integer targetValue, Integer initialValue, LocalDate deadline, Instant createdAt) {
        this.userId = userId;
        this.skillId = skillId;
        this.skillName = skillName;
        this.goalType = goalType;
        this.targetValue = targetValue;
        this.initialValue = initialValue;
        this.currentValue = initialValue;
        this.deadline = deadline;
        this.createdAt = createdAt;
    }

    /**
     * Checks if the goal has been completed based on the current value.
     * If completed, updates status and timestamp.
     *
     * @return true if the goal was *just* completed, false otherwise.
     */
    public boolean checkAndMarkCompleted() {
        if (status == GoalStatus.ACTIVE && currentValue >= targetValue) {
            this.status = GoalStatus.COMPLETED;
            this.completedAt = Instant.now();
            return true;
        }
        return false;
    }
}