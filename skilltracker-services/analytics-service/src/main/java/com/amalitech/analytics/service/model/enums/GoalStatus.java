package com.amalitech.analytics.service.model.enums;

public enum GoalStatus {
    /**
     * The goal is in progress.
     */
    ACTIVE,
    /**
     * The user has successfully met the goal's target.
     */
    COMPLETED,
    /**
     * The user has manually archived this goal (e.g., no longer relevant).
     */
    ARCHIVED
}