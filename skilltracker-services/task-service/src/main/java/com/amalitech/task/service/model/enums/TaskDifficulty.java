package com.amalitech.task.service.model.enums;

/**
 * Enumeration defining the standard difficulty levels assigned to tasks within the SkillBoost platform.
 * <p>
 * This enum is a critical component for the AI Task Generation agent and the adaptive learning
 * engine, ensuring that tasks are generated, served, and tracked according to a consistent
 * and predictable scale of complexity.
 * <ul>
 * <li>{@code EASY}: Tasks requiring fundamental knowledge or basic application of concepts.</li>
 * <li>{@code MEDIUM}: Tasks requiring intermediate problem-solving, synthesis of multiple concepts, or common technical challenges.</li>
 * <li>{@code HARD}: Tasks requiring deep expertise, complex algorithmic thinking, or advanced architectural design decisions.</li>
 * </ul>
 */
public enum TaskDifficulty {
    EASY,
    MEDIUM,
    HARD
}