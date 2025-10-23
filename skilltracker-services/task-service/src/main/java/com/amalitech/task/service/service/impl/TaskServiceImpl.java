package com.amalitech.task.service.service.impl;

import com.amalitech.task.service.dto.TaskAvailabilityDTO;
import com.amalitech.task.service.dto.TaskDTO;
import com.amalitech.task.service.dto.request.BatchGenerationRequest;
import com.amalitech.task.service.events.TaskEventProducer;
import com.amalitech.task.service.exception.ResourceNotFoundException;
import com.amalitech.task.service.mapper.TaskMapper;
import com.amalitech.task.service.model.Task;
import com.amalitech.task.service.model.enums.TaskDifficulty;
import com.amalitech.task.service.model.view.SkillView;
import com.amalitech.task.service.repository.*;
import com.amalitech.task.service.service.TaskService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional(readOnly = true)
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final SkillViewRepository skillViewRepository;
    private final TaskSubmissionRepository submissionRepository;
    private final TaskEventProducer taskEventProducer;
    private final TaskMapper taskMapper;

    @Value("${app.task.min-tasks-per-difficulty:5}")
    private int minTasksPerDifficulty;

    public TaskServiceImpl(TaskRepository taskRepository,
                           SkillViewRepository skillViewRepository,
                           TaskSubmissionRepository submissionRepository,
                           TaskEventProducer taskEventProducer,
                           TaskMapper taskMapper
    ) {
        this.taskRepository = taskRepository;
        this.skillViewRepository = skillViewRepository;
        this.submissionRepository = submissionRepository;
        this.taskEventProducer = taskEventProducer;
        this.taskMapper = taskMapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<TaskDTO> getPersonalizedTasks(UUID userId, String skillName, int limit) {
        log.info("Fetching personalized tasks for user: {}, skill: {}", userId, skillName);

        SkillView skill = getSkillByName(skillName);
        TaskDifficulty difficulty = determineUserDifficulty(userId, skill.getId());
        log.debug("Determined difficulty: {} for user: {}", difficulty, userId);

        List<Task> tasks = getOrGenerateTasksForSkillAndDifficulty(
                skill, difficulty, limit
        );

        return tasks.stream()
                .map(taskMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<TaskDTO> getTasksForSkillAndDifficulty(String skillName, TaskDifficulty difficulty, int limit) {
        log.info("Getting tasks for skill: {}, difficulty: {}, limit: {}", skillName, difficulty, limit);

        SkillView skill = getSkillByName(skillName);

        List<Task> tasks = getOrGenerateTasksForSkillAndDifficulty(
                skill, difficulty, limit
        );

        return tasks.stream()
                .map(taskMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public TaskDTO getTaskById(UUID taskId) {
        log.info("Fetching task by ID: {}", taskId);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskId));

        return taskMapper.toDTO(task);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TaskAvailabilityDTO checkTaskAvailability(String skillName, TaskDifficulty difficulty) {
        SkillView skill = skillViewRepository.findByName(skillName)
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found: " + skillName));

        long availableCount = taskRepository.countBySkillAndDifficulty(
                skill.getId(), difficulty, true
        );

        return TaskAvailabilityDTO.builder()
                .skillName(skillName)
                .difficulty(difficulty)
                .availableTaskCount((int) availableCount)
                .needsGeneration(availableCount < minTasksPerDifficulty)
                .build();
    }

    private List<Task> getOrGenerateTasksForSkillAndDifficulty(
            SkillView skill, TaskDifficulty difficulty, int limit) {

        List<Task> cachedTasks = taskRepository.findBySkillAndDifficulty(
                skill.getId(),
                difficulty,
                true,
                PageRequest.of(0, limit)
        );

        if (cachedTasks.size() >= limit) {
            log.info("Cache hit: Using {} cached tasks for {}/{}",
                    cachedTasks.size(), skill.getName(), difficulty);
            return cachedTasks;
        }

        log.info("Cache miss for {}/{}. Have {}, need {}. Triggering async generation.",
                skill.getName(), difficulty, cachedTasks.size(), limit);

        BatchGenerationRequest request = new BatchGenerationRequest(
                skill.getName(),
                difficulty,
                minTasksPerDifficulty
        );

        taskEventProducer.requestBatchTaskGeneration(request);

        return cachedTasks;
    }

    private SkillView getSkillByName(String skillName) {
        return skillViewRepository.findByName(skillName)
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found: " + skillName));
    }

    private TaskDifficulty determineUserDifficulty(UUID userId, UUID skillId) {
        Long correctCount = submissionRepository.countByUserIdAndIsCorrect(userId, true);

        if (correctCount < 5) return TaskDifficulty.EASY;
        if (correctCount < 15) return TaskDifficulty.MEDIUM;
        return TaskDifficulty.HARD;
    }
}