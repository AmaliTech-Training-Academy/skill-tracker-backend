package com.amalitech.user.service.model.enums;

import lombok.Getter;

/**
 * Defines the standard levels of complexity or proficiency associated with a skill or task.
 */
@Getter
public enum DifficultyLevel {
    BEGINNER(1),
    INTERMEDIATE(2),
    ADVANCED(3);

    private final int level;

    DifficultyLevel(int level) {
        this.level = level;
    }

}
