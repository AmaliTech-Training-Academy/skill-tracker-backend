package com.amalitech.analytics.service.events;

import org.springframework.context.ApplicationEvent;
import java.util.UUID;

/**
 * Fired when a UserGoal is marked as COMPLETED.
 * Can be consumed by a NotificationService to alert the user.
 */
public class GoalCompletedEvent extends ApplicationEvent {
    private final UUID userId;
    private final UUID goalId;
    private final String goalDescription;

    public GoalCompletedEvent(Object source, UUID userId, UUID goalId, String goalDescription) {
        super(source);
        this.userId = userId;
        this.goalId = goalId;
        this.goalDescription = goalDescription;
    }

    public UUID getUserId() { return userId; }
    public UUID getGoalId() { return goalId; }
    public String getGoalDescription() { return goalDescription; }
}