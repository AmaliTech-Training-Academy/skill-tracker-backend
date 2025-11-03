package com.amalitech.task.service.service;

import com.amalitech.task.service.model.Task;
import com.amalitech.task.service.model.enums.TaskDifficulty;
import com.amalitech.task.service.model.view.SkillView;

import java.util.List;

/**
 * Interface for all AI-driven content generation.
 * This abstracts the specific AI provider (e.g., DeepSeek) and the
 * persistence logic from the job orchestrator.
 */
public interface ContentGeneratorService {
    /**
     * Generates a list of coding tasks based on the provided prompt.
     *
     * @param skill      The skill to generate tasks for.
     * @param difficulty The difficulty level.
     * @return A list of newly created and persisted Task entities.
     */
    List<Task> generateCodingTask(SkillView skill, TaskDifficulty difficulty);
}
