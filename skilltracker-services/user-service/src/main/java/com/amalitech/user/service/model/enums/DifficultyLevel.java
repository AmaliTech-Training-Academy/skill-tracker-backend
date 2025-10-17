package com.amalitech.user.service.model.enums;

/**
 * Defines the standard levels of complexity or proficiency associated with a skill or task.
 */
public enum DifficultyLevel {
    BEGINNER(1),
    INTERMEDIATE(2),
    ADVANCED(3);

    private final int level;

    DifficultyLevel(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }
}
