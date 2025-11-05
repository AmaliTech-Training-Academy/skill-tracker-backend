package com.amalitech.task.service.service;

import com.amalitech.task.service.model.Task;
import com.amalitech.task.service.model.enums.TaskDifficulty;
import com.amalitech.task.service.model.view.SkillView;

import java.util.List;

public interface ContentGeneratorService {

    /**
     * Generates a batch of coding tasks for a given skill and difficulty.
     *
     * @param skill The skill to generate tasks for.
     * @param difficulty The difficulty of the tasks.
     * @param quantity The number of tasks to generate in this batch.
     * @return A list of the persisted Task entities.
     */
    List<Task> generateCodingTask(SkillView skill, TaskDifficulty difficulty, int quantity);

}