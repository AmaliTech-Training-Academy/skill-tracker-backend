package com.amalitech.task.service.service;

import com.amalitech.task.service.model.Task;
import com.amalitech.task.service.model.enums.TaskDifficulty;
import com.amalitech.task.service.model.view.SkillView;

/**
 * Interface for all AI-driven content generation.
 * This abstracts the specific AI provider (e.g., DeepSeek) and the
 * persistence logic from the job orchestrator.
 */
public interface ContentGeneratorService {
    /**
     * Generates, parses, and saves a new MCQ task.
     *
     * @param skill The skill to associate the task with.
     * @param difficulty The task's difficulty.
     * @param topic A specific topic for the question.
     * @return The newly created and persisted Task.
     */
    Task generateMcqTask(SkillView skill, TaskDifficulty difficulty, String topic);
}
