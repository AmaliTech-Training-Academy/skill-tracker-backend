package com.amalitech.task.service.service.impl;

import com.amalitech.task.service.dto.TaskAvailabilityDTO;
import com.amalitech.task.service.dto.TaskDTO;
import com.amalitech.task.service.dto.request.BatchGenerationRequest;
import com.amalitech.task.service.dto.request.GenerateTaskRequest;
import com.amalitech.task.service.dto.response.AdminTaskDetailResponse;
import com.amalitech.task.service.dto.response.AdminTaskSummaryResponse;
import com.amalitech.task.service.events.RabbitMQEventProducer;
import com.amalitech.task.service.exception.ResourceNotFoundException;
import com.amalitech.task.service.mapper.TaskMapper;
import com.amalitech.task.service.model.Task;
import com.amalitech.task.service.model.enums.TaskDifficulty;
import com.amalitech.task.service.model.enums.TaskType;
import com.amalitech.task.service.model.view.SkillView;
import com.amalitech.task.service.repository.TaskRepository;
import com.amalitech.task.service.repository.TaskSubmissionRepository;
import com.amalitech.task.service.service.SkillService;
import com.amalitech.task.service.service.TaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of the TaskService interface.
 * Provides business logic for task management, personalization, and availability checking.
 * Handles task retrieval with caching and triggers asynchronous task generation when needed.
 *
 */

@Service
@Slf4j
@Transactional(readOnly = true)
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final SkillService skillService;
    private final TaskSubmissionRepository submissionRepository;
    private final RabbitMQEventProducer taskEventProducer;
    private final TaskMapper taskMapper;
    private final StringRedisTemplate redisTemplate;

    /**
     * Minimum number of tasks required per difficulty level before triggering generation.
     * Configurable via application property: app.task.min-tasks-per-difficulty
     */
    @Value("${app.task.min-tasks-per-difficulty:5}")
    private int minTasksPerDifficulty;

    private static final String FETCH_LOCK_PREFIX = "lock:task-fetch-gen:";
    private static final Duration FETCH_LOCK_TIMEOUT = Duration.ofMinutes(1);

    public TaskServiceImpl(TaskRepository taskRepository,
                           SkillService skillService,
                           TaskSubmissionRepository submissionRepository,
                           RabbitMQEventProducer taskEventProducer,
                           TaskMapper taskMapper, StringRedisTemplate redisTemplate
    ) {
        this.taskRepository = taskRepository;
        this.skillService = skillService;
        this.submissionRepository = submissionRepository;
        this.taskEventProducer = taskEventProducer;
        this.taskMapper = taskMapper;
        this.redisTemplate = redisTemplate;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<TaskDTO> getPersonalizedTasks(UUID userId, String skillName, int limit) {
        log.info("Fetching personalized tasks for user: {}, skill: {}", userId, skillName);

        SkillView skill = skillService.getSkillByName(skillName);
        TaskDifficulty difficulty = determineUserDifficulty(userId, skill.getId());
        log.debug("Determined difficulty: {} for user: {}", difficulty, userId);

        List<Task> tasks = getOrGenerateTasksForSkillAndDifficulty(
                userId, skill, difficulty, TaskType.CODING, limit
        );

        return tasks.stream()
                .map(taskMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Cacheable(cacheNames = "tasks-public-cache", key = "#skillName + '_' + #difficulty + '_' + #limit")
    public List<TaskDTO> getTasksForSkillAndDifficulty(String skillName, TaskDifficulty difficulty, int limit) {
        log.info("Getting tasks for skill: {}, difficulty: {}, limit: {}", skillName, difficulty, limit);

        SkillView skill = skillService.getSkillByName(skillName);

        List<Task> tasks = getOrGenerateTasksForSkillAndDifficulty(
                null, skill, difficulty, TaskType.CODING, limit
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
    @Cacheable(cacheNames = "tasks-public-cache", key = "#taskId")
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
    @Transactional(readOnly = true)
    public Page<AdminTaskSummaryResponse> getAllTasksForAdmin(Pageable pageable) {
        log.debug("Fetching paginated tasks for admin, page: {}, size: {}",
                pageable.getPageNumber(), pageable.getPageSize());

        Page<Task> taskPage = taskRepository.findAll(pageable);

        return taskPage.map(taskMapper::toAdminSummaryDTO);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public AdminTaskDetailResponse getTaskForAdmin(UUID taskId) {
        log.debug("Fetching full task details for admin, ID: {}", taskId);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));

        return taskMapper.toAdminDetailDTO(task);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TaskAvailabilityDTO checkTaskAvailability(String skillName, TaskDifficulty difficulty) {
        SkillView skill = skillService.getSkillByName(skillName);

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

    /**
     * Retrieves cached tasks or triggers generation if insufficient tasks are available.
     * This method implements a cache-first strategy, checking for existing tasks before
     * requesting new task generation asynchronously.
     *
     * @param skill the skill view containing skill information
     * @param difficulty the difficulty level of tasks to retrieve
     * @param limit the maximum number of tasks to retrieve
     * @return a list of tasks from the cache (may be less than the requested limit)
     */
    private List<Task> getOrGenerateTasksForSkillAndDifficulty(
            UUID userId, SkillView skill, TaskDifficulty difficulty, TaskType taskType, int limit) {

        List<Task> cachedTasks = taskRepository.findBySkillIdAndDifficultyAndType(
                skill.getId(),
                difficulty,
                taskType,
                true,
                PageRequest.of(0, limit)
        );

        if (cachedTasks.size() >= limit) {
            log.info("Cache hit: Using {} cached tasks for {}/{}",
                    cachedTasks.size(), skill.getName(), difficulty);
            return cachedTasks;
        }

        if (userId == null) {
            log.warn("Cache miss for anonymous request {}/{}. Not triggering generation.",
                    skill.getName(), difficulty);
            return cachedTasks;
        }

        String lockKey = FETCH_LOCK_PREFIX + skill.getName() + ":" + difficulty;

        Boolean lockAcquired = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "generating", FETCH_LOCK_TIMEOUT);

        if (Boolean.FALSE.equals(lockAcquired)) {
            log.warn("Cache miss for {}/{}. Generation is already in progress. Returning {} tasks.",
                    skill.getName(), difficulty, cachedTasks.size());
            return cachedTasks;
        }

        try {
            log.info("Cache miss for {}/{}. Acquired lock. Triggering async generation.",
                    skill.getName(), difficulty);

            BatchGenerationRequest request = new BatchGenerationRequest(
                    userId,
                    skill.getName(),
                    difficulty,
                    minTasksPerDifficulty,
                    taskType
            );

            taskEventProducer.requestBatchTaskGeneration(request);

        } catch (Exception e) {
            log.error("Failed to publish task generation request for {}: {}. Lock will remain for {}s.",
                    lockKey, e.getMessage(), FETCH_LOCK_TIMEOUT.toSeconds(), e);
            redisTemplate.delete(lockKey);
        }

        return cachedTasks;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void requestSpecificTaskGeneration(GenerateTaskRequest requestBody, UUID adminUserId) {
        log.info("Admin {} requesting specific task generation for skill: {}", adminUserId, requestBody.skillName());

        GenerateTaskRequest messagePayload = new GenerateTaskRequest(
                adminUserId,
                requestBody.taskType(),
                requestBody.skillName(),
                requestBody.difficulty(),
                requestBody.topic(),
                requestBody.languageName()
        );

        taskEventProducer.requestSpecificTaskGeneration(messagePayload);

        log.info("Specific task generation request for admin {} published to RabbitMQ.", adminUserId);
    }

    /**
     * Determines the appropriate difficulty level for a user based on their submission history.
     * The difficulty is calculated based on the number of correct submissions:
     * <ul>
     *   <li>Less than 5 correct: EASY</li>
     *   <li>5-14 correct: MEDIUM</li>
     *   <li>15 or more correct: HARD</li>
     * </ul>
     *
     * @param userId the unique identifier of the user
     * @param skillId the unique identifier of the skill
     * @return the determined difficulty level
     */
    private TaskDifficulty determineUserDifficulty(UUID userId, UUID skillId) {
        Long correctCount = submissionRepository.countByUserIdAndIsCorrect(userId, true);

        if (correctCount < 5) return TaskDifficulty.BEGINNER;
        if (correctCount < 15) return TaskDifficulty.INTERMEDIATE;
        return TaskDifficulty.ADVANCED;
    }
}