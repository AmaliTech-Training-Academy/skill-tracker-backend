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
     * Generates a multiple-choice question task using AI content generation.
     * This method orchestrates the entire MCQ generation workflow:
     * <ol>
     *   <li>Builds a prompt from the template with skill, difficulty, and topic</li>
     *   <li>Calls the DeepSeek API to generate content</li>
     *   <li>Parses the AI response into structured MCQ content</li>
     *   <li>Creates and persists the task with versioning</li>
     * </ol>
     *
     * @param skill the skill view for which to generate the task
     * @param difficulty the difficulty level of the task (EASY, MEDIUM, HARD)
     * @param topic the specific topic or subject area for the question
     * @return the generated and persisted Task entity
     * @throws RuntimeException if AI generation or task creation fails
     */
    Task generateMcqTask(SkillView skill, TaskDifficulty difficulty, String topic);
}
