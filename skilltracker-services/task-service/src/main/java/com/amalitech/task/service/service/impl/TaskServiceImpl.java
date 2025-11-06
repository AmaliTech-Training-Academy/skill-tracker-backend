package com.amalitech.task.service.service.impl;

import com.amalitech.task.service.dto.MCQquestionDTO;
import com.amalitech.task.service.dto.TaskAvailabilityDTO;
import com.amalitech.task.service.dto.TaskDTO;
import com.amalitech.task.service.dto.request.BatchGenerationRequest;
import com.amalitech.task.service.dto.request.GenerateTaskRequest;
import com.amalitech.task.service.dto.response.AdminTaskDetailResponse;
import com.amalitech.task.service.dto.response.AdminTaskSummaryResponse;
import com.amalitech.task.service.dto.request.McqRequestTaskDTO;
import com.amalitech.task.service.dto.response.McqResponseTaskDTO;
import com.amalitech.task.service.dto.request.McqRequestDTO;
import com.amalitech.task.service.dto.request.UserProfileRequestDTO;
import com.amalitech.task.service.dto.response.McqResponseDTO;
import com.amalitech.task.service.events.RabbitMQEventProducer;
import com.amalitech.task.service.exception.ResourceNotFoundException;
import com.amalitech.task.service.mapper.TaskMapper;
import com.amalitech.task.service.model.Task;
import com.amalitech.task.service.model.UserSkillProfile;
import com.amalitech.task.service.model.enums.TaskDifficulty;
import com.amalitech.task.service.model.enums.TaskType;
import com.amalitech.task.service.model.view.SkillView;
import com.amalitech.task.service.repository.TaskRepository;
import com.amalitech.task.service.repository.TaskSubmissionRepository;
import com.amalitech.task.service.repository.UserSkillProfileRepository;
import com.amalitech.task.service.service.SkillService;
import com.amalitech.task.service.service.TaskService;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.Strictness;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.core.io.ClassPathResource;

import java.time.Duration;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.amalitech.task.service.mapper.MCQMapper.mapJsonToMcqResponse;

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
    private final UserSkillProfileRepository userSkillProfileRepository;
    private final SkillService skillService;
    private final TaskSubmissionRepository submissionRepository;
    private final RabbitMQEventProducer taskEventProducer;
    private final TaskMapper taskMapper;
    private final StringRedisTemplate redisTemplate;
    private final String model = "gemini-2.5-flash";

    /**
     * Minimum number of tasks required per difficulty level before triggering generation.
     * Configurable via application property: app.task.min-tasks-per-difficulty
     */
    @Value("${app.task.min-tasks-per-difficulty:5}")
    private int minTasksPerDifficulty;

    private static final String FETCH_LOCK_PREFIX = "lock:task-fetch-gen:";
    private static final Duration FETCH_LOCK_TIMEOUT = Duration.ofMinutes(1);

    public TaskServiceImpl(TaskRepository taskRepository,
                           UserSkillProfileRepository userSkillProfileRepository,
                           SkillService skillService,
                           TaskSubmissionRepository submissionRepository,
                           RabbitMQEventProducer taskEventProducer,
                           TaskMapper taskMapper, StringRedisTemplate redisTemplate
    ) {
        this.taskRepository = taskRepository;
        this.userSkillProfileRepository = userSkillProfileRepository;
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
    public List<TaskDTO> getPersonalizedTasks(UUID userId, String skillName, TaskType taskType, int limit) {
        log.info("Fetching personalized tasks for user: {}, skill: {}", userId, skillName);

        UserSkillProfile profile = userSkillProfileRepository
                .findByIdUserIdAndSkillName(userId, skillName)
                .orElseThrow(() -> new ResourceNotFoundException("User skill profile for " + skillName + " not found."));

        UUID skillId = profile.getId().getSkillId();
        TaskDifficulty difficulty = profile.getDifficulty();
        log.debug("Found local profile: skillId={}, difficulty={} for user: {}", skillId, difficulty, userId);

        List<Task> tasks = getOrGenerateTasksForSkillAndDifficulty(
                userId,
                profile.getSkillName(),
                profile.getId().getUserId(),
                difficulty,
                taskType,
                limit
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
    public List<TaskDTO> getTasksForSkillAndDifficulty(String skillName, TaskDifficulty difficulty, TaskType taskType, int limit) {
        log.info("Getting tasks for skill: {}, difficulty: {}, limit: {}", skillName, difficulty, limit);

        SkillView skill = skillService.getSkillByName(skillName);
        UUID skillId = skill.getId();

        List<Task> tasks = getOrGenerateTasksForSkillAndDifficulty(
                null,
                skillName,
                skillId,
                difficulty,
                taskType,
                limit
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
     * --- THIS IS THE ADJUSTED METHOD ---
     *
     * Retrieves tasks or triggers generation if needed.
     * It now uses simple parameters and has a "guard clause" to prevent
     * generation for anonymous (null) users.
     *
     * @param userId The ID of the user (can be null for anonymous requests)
     * @param skillName The name of the skill (e.g., "PYTHON")
     * @param difficulty The difficulty level (e.g., "BEGINNER")
     * @param taskType The type of task (e.g., "CODING")
     * @param limit The number of tasks to fetch
     * @return A list of tasks found in the database.
     */
    private List<Task> getOrGenerateTasksForSkillAndDifficulty(
            UUID userId, String skillName, UUID skillId, TaskDifficulty difficulty, TaskType taskType, int limit) {

        List<Task> cachedTasks = taskRepository.findBySkillIdAndDifficultyAndType(
                skillId,
                difficulty,
                taskType,
                true,
                PageRequest.of(0, limit)
        );

        if (cachedTasks.size() >= limit) {
            log.info("Cache hit: Using {} cached tasks for {}/{}",
                    cachedTasks.size(), skillName, difficulty);
            return cachedTasks;
        }

        if (userId == null) {
            log.warn("Cache miss for anonymous request {}/{}. Not triggering generation. Returning {} tasks.",
                    skillName, difficulty, cachedTasks.size());
            return cachedTasks;
        }

        String lockKey = FETCH_LOCK_PREFIX + skillName + ":" + difficulty + ":" + taskType;
        Boolean lockAcquired = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "generating", FETCH_LOCK_TIMEOUT);

        if (Boolean.FALSE.equals(lockAcquired)) {
            log.warn("Cache miss for user {} on {}/{}. Generation is already in progress. Returning {} tasks.",
                    userId, skillName, difficulty, cachedTasks.size());
            return cachedTasks;
        }

        try {
            log.info("Cache miss for user {} on {}/{}. Acquired lock. Triggering async generation.",
                    userId, skillName, difficulty);

            BatchGenerationRequest request = new BatchGenerationRequest(
                    userId,
                    skillName,
                    difficulty,
                    minTasksPerDifficulty,
                    taskType
            );

            taskEventProducer.requestBatchTaskGeneration(request);

        } catch (Exception e) {
            log.error("Failed to publish task generation request for {}: {}. Releasing lock.",
                    lockKey, e.getMessage(), e);
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
     * @param userId  the unique identifier of the user
     * @param skillId the unique identifier of the skill
     * @return the determined difficulty level
     */
    private TaskDifficulty determineUserDifficulty(UUID userId, UUID skillId) {
        Long correctCount = submissionRepository.countByUserIdAndIsCorrect(userId, true);

        if (correctCount < 5) return TaskDifficulty.BEGINNER;
        if (correctCount < 15) return TaskDifficulty.INTERMEDIATE;
        return TaskDifficulty.ADVANCED;
    }

    @Override
    public McqResponseDTO generateMCQ(McqRequestDTO mcqRequestDTO) throws IOException {
        Client client = new Client();
        ClassPathResource prompt = new ClassPathResource("prompts/mcq/mcq_prompt.json");

        String updatedFields = updateFields(
                Files.readString(prompt.getFile().toPath(), StandardCharsets.UTF_8),
                Map.of(
                        "interest", mcqRequestDTO.getInterest(),
                        "difficulty", mcqRequestDTO.getDifficulty(),
                        "no_of_questions", String.valueOf(mcqRequestDTO.getNo_of_questions()))
                );


        GenerateContentResponse response =
                client.models.generateContent(
                        model,
                        updatedFields,
                        null);

        if (response.text() == null) {
            throw new IOException("No response from Ai API....");
        }
        List<MCQquestionDTO> questions = parseJsonToMcqList(response.text());

//        Task newTask = new Task().builder()
//                .title(taskDTO.getTitle())
//                .description(taskDTO.getDescription())
//                .type(taskDTO.getType())
//                .difficulty(taskDTO.getDifficulty())
//                .build();

//        taskRepository.save(newTask);

        return new McqResponseDTO(questions);
    }

    @Override
    public UserProfileRequestDTO generateLearningPath(UserProfileRequestDTO userProfileRequestDTO) {
        Client client = new Client();
        ClassPathResource prompt = new ClassPathResource("prompts/mcq/learningPath_prompt.json");

//        String updateUserId = updateNumberOfQuestions(
//                Files.readString(prompt.getFile().toPath(), StandardCharsets.UTF_8),
//                no_of_questions);
//
//        GenerateContentResponse response =
//                client.models.generateContent(
//                        model,
//                        jsonResponse,
//                        null);


        return new UserProfileRequestDTO();
    }

    public static String updateFields (String jsonString, Map < String, String > updates){
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(jsonString, JsonObject.class);
        updates.forEach(jsonObject::addProperty);
        return gson.toJson(jsonObject);
    }

    public static String updateField (String jsonString, String field, String value){
        return updateFields(jsonString, Map.of(field, value));
    }

    public static String cleanMarkdownJson (String response){
        String cleaned = response.trim();

        // Remove opening markdown code block
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }

        // Remove closing markdown code block
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }

        return cleaned.trim();
    }

    public static List<MCQquestionDTO> parseJsonToMcqList (String jsonArrayString){
        Gson gson = new GsonBuilder().setStrictness(Strictness.LENIENT).create();

        // Define the type for List<MCQquestionDTO>
        Type listType = new TypeToken<List<MCQquestionDTO>>() {
        }.getType();

        // Parse JSON array directly to List<MCQquestionDTO>
        List<MCQquestionDTO> mcqQuestions = gson.fromJson(jsonArrayString, listType);

        return mcqQuestions;
    }
}