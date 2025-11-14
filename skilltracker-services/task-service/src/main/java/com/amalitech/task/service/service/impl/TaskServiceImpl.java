package com.amalitech.task.service.service.impl;

import com.amalitech.task.service.dto.LearningPathDTO;
import com.amalitech.task.service.dto.MCQquestionDTO;
import com.amalitech.task.service.dto.TaskAvailabilityDTO;
import com.amalitech.task.service.dto.TaskDTO;
import com.amalitech.task.service.dto.request.BatchGenerationRequest;
import com.amalitech.task.service.dto.request.GenerateTaskRequest;
import com.amalitech.task.service.dto.request.McqRequestDTO;
import com.amalitech.task.service.dto.request.UserProfileRequestDTO;
import com.amalitech.task.service.dto.response.*;
import com.amalitech.task.service.events.RabbitMQEventProducer;
import com.amalitech.task.service.exception.AiServiceException;
import com.amalitech.task.service.exception.ResourceNotFoundException;
import com.amalitech.task.service.mapper.TaskMapper;
import com.amalitech.task.service.model.Task;
import com.amalitech.task.service.model.TaskSubmission;
import com.amalitech.task.service.model.UserSkillProfile;
import com.amalitech.task.service.model.content.impl.McqTaskContent;
import com.amalitech.task.service.model.enums.CompletedTaskPeriod;
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
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
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
    private final UserSkillProfileRepository userSkillProfileRepository;
    private final SkillService skillService;
    private final TaskSubmissionRepository submissionRepository;
    private final RabbitMQEventProducer taskEventProducer;
    private final TaskMapper taskMapper;
    private final StringRedisTemplate redisTemplate;
    private static final String model = "gemini-2.5-flash";

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
     *param
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
                        "userId", mcqRequestDTO.getUserId().toString(),
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
            throw new AiServiceException("No response from AI API");
        }
        List<MCQquestionDTO> questions = parseJsonToMcqList(response.text());

        saveQuestions(questions);

        return new McqResponseDTO(questions);
    }

    @Override
    public LearningPathResponseDTO generateLearningPath(UserProfileRequestDTO userProfileRequestDTO) throws IOException {
        Client client = new Client();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        ClassPathResource prompt = new ClassPathResource("prompts/learningPath/learningPath_prompt.json");
        String StringPrompt = Files.readString(prompt.getFile().toPath(), StandardCharsets.UTF_8);
        String updatedFields = updateBlock(StringPrompt, "input", userProfileRequestDTO);

        GenerateContentResponse response =
                client.models.generateContent(
                        model,
                        updatedFields,
                        null);

        if (response.text() == null) {
            throw new IOException("No response from Ai API....");
        }

        String cleanedResponse = cleanModelResponse(response.text());

        LearningPathDTO responseJson = gson.fromJson(cleanedResponse, LearningPathDTO.class);

        return new LearningPathResponseDTO(responseJson);
    }

    public static String updateBlock(String jsonString, String blockKey, Object blockValue) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonObject jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();

        jsonObject.add(blockKey, gson.toJsonTree(blockValue));

        return gson.toJson(jsonObject);
    }

    public static String cleanModelResponse(String modelResponse) {
        if (modelResponse == null || modelResponse.isEmpty()) {
            return modelResponse;
        }
        String cleanedJson = modelResponse.replaceFirst("```(json|text|)", "");
        if (cleanedJson.endsWith("```")) {
            cleanedJson = cleanedJson.substring(0, cleanedJson.length() - 3);
        }
        return cleanedJson.trim();
    }

    public static String updateFields (String jsonString, Map < String, String > updates){
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(jsonString, JsonObject.class);
        updates.forEach(jsonObject::addProperty);
        return gson.toJson(jsonObject);
    }

    public static List<MCQquestionDTO> parseJsonToMcqList (String jsonArrayString){
        Gson gson = new GsonBuilder().setStrictness(Strictness.LENIENT).create();

        Type listType = new TypeToken<List<MCQquestionDTO>>() {
        }.getType();

        return gson.fromJson(jsonArrayString, listType);
    }

    public void saveQuestions(List<MCQquestionDTO> questions) {

        for(MCQquestionDTO question : questions) {
            Task task = new Task().builder()
                    .title(question.getQuestion_title())
                    .description(question.getQuestion_description())
                    .type(TaskType.valueOf(question.getQuestion_type()))
                    .difficulty(TaskDifficulty.valueOf(question.getQuestion_difficulty()))
                    .content(createMCQContent(question))
                    .xpReward(question.getXpReward())
                    .build();

            taskRepository.save(task);
        }
    }

    public McqTaskContent createMCQContent(MCQquestionDTO content) {

        return McqTaskContent.builder()
                .question_number(content.getQuestion_number())
                .question_text(content.getQuestion_text())
                .question_duration(content.getQuestion_duration())
                .options(content.getOptions())
                .hint(content.getHint())
                .correct_answer(content.getCorrect_answer())
                .explanation(content.getExplanation())
                .build();
    }

    /**
     * {@inheritDoc}
     *
     * This is the main orchestrator method. It delegates complex logic to private
     * helper methods for validation, profile filtering, and data fetching.
     */
    @Override
    public UserTasksResponse getUserTasksGroupedByStatus(UUID userId, int pendingPage, int pendingSize,
                                                         int completedPage, int completedSize,
                                                         String skillName, String completedPeriodStr) {

        log.info("Fetching grouped tasks for user {} with filters [skill={}, period={}]",
                userId, skillName, completedPeriodStr);

        List<UserSkillProfile> relevantProfiles;
        try {
            relevantProfiles = getRelevantSkillProfiles(userId, skillName);
        } catch (ResourceNotFoundException e) {
            log.warn("User {} filtered for unknown skill '{}', returning empty", userId, skillName);
            return createEmptyUserTasksResponse(pendingPage, pendingSize, completedPage, completedSize);
        }

        if (relevantProfiles.isEmpty()) {
            log.warn("User {} has no matching skill profiles for filter, returning empty", userId);
            return createEmptyUserTasksResponse(pendingPage, pendingSize, completedPage, completedSize);
        }

        UUID skillIdFilter = (skillName != null) ? relevantProfiles.get(0).getId().getSkillId() : null;

        Pageable pendingPageable = PageRequest.of(pendingPage, pendingSize);
        Pageable completedPageable = PageRequest.of(completedPage, completedSize, Sort.by(Sort.Direction.DESC, "submittedAt"));
        CompletedTaskPeriod periodFilter = CompletedTaskPeriod.fromString(completedPeriodStr);

        Set<UUID> completedTaskIds = submissionRepository.findCompletedTaskIdsByUser(userId);

        Page<TaskDTO> pendingDTOs = getPendingTasks(relevantProfiles, completedTaskIds, pendingPageable);
        Page<TaskDTO> completedDTOs = getCompletedTasks(userId, skillIdFilter, periodFilter, completedTaskIds, completedPageable);

        log.info("Returning {} pending and {} completed tasks for user {}",
                pendingDTOs.getTotalElements(), completedDTOs.getTotalElements(), userId);

        return new UserTasksResponse(pendingDTOs, completedDTOs);
    }

    /**
     * Fetches and filters the user's skill profiles based on an optional skillName.
     *
     * @param userId    The user's ID.
     * @param skillName The optional skill name to filter by.
     * @return A list of relevant UserSkillProfile objects.
     * @throws ResourceNotFoundException if skillName is provided but not found.
     */
    private List<UserSkillProfile> getRelevantSkillProfiles(UUID userId, String skillName) {
        UUID targetSkillId = null;
        if (skillName != null && !skillName.isBlank()) {
            SkillView skill = skillService.getSkillByName(skillName);
            targetSkillId = skill.getId();
        }

        List<UserSkillProfile> allProfiles = userSkillProfileRepository.findById_UserId(userId);
        if (allProfiles.isEmpty()) {
            return Collections.emptyList();
        }

        if (targetSkillId != null) {
            final UUID finalTargetSkillId = targetSkillId;
            return allProfiles.stream()
                    .filter(p -> p.getId().getSkillId().equals(finalTargetSkillId))
                    .collect(Collectors.toList());
        }

        return allProfiles;
    }

    /**
     * Fetches the paginated list of pending tasks for the user.
     *
     * @param relevantProfiles The filtered list of user skill profiles.
     * @param completedTaskIds The set of task IDs that the user has completed.
     * @param pendingPageable  The pagination information for pending tasks.
     * @return A Page of TaskDTOs.
     */
    private Page<TaskDTO> getPendingTasks(List<UserSkillProfile> relevantProfiles, Set<UUID> completedTaskIds, Pageable pendingPageable) {
        Specification<Task> pendingSpec = createPendingTaskSpecification(relevantProfiles, completedTaskIds);

        Page<Task> pendingTasks = taskRepository.findAll(pendingSpec, pendingPageable);
        return pendingTasks.map(taskMapper::toDTO);
    }

    /**
     * Fetches the paginated list of completed tasks for the user.
     *
     * @param userId             The user's ID.
     * @param skillIdFilter      Optional skill ID to filter by.
     * @param periodFilter       The time period filter.
     * @param completedTaskIds   The set of task IDs that the user has completed.
     * @param completedPageable  The pagination information for completed tasks.
     * @return A Page of TaskDTOs.
     */
    private Page<TaskDTO> getCompletedTasks(UUID userId, UUID skillIdFilter, CompletedTaskPeriod periodFilter, Set<UUID> completedTaskIds, Pageable completedPageable) {
        if (completedTaskIds.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), completedPageable, 0);
        }
        
        Specification<TaskSubmission> completedSpec = createCompletedTaskSpecification(userId, skillIdFilter, periodFilter);

        Page<TaskSubmission> completedSubmissions = submissionRepository.findAll(completedSpec, completedPageable);

        return completedSubmissions
                .map(TaskSubmission::getTask)
                .map(taskMapper::toDTO);
    }

    /**
     * Builds the JPA Specification for querying pending tasks.
     */
    private Specification<Task> createPendingTaskSpecification(List<UserSkillProfile> relevantProfiles, Set<UUID> completedTaskIds) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            List<Predicate> skillPredicates = relevantProfiles.stream()
                    .map(profile -> cb.and(
                            cb.equal(root.get("taskDefinition").get("skill").get("id"), profile.getId().getSkillId()),
                            cb.equal(root.get("difficulty"), profile.getDifficulty())
                    ))
                    .toList();
            predicates.add(cb.or(skillPredicates.toArray(new Predicate[0])));

            if (!completedTaskIds.isEmpty()) {
                predicates.add(root.get("id").in(completedTaskIds).not());
            }

            predicates.add(cb.equal(root.get("isPublished"), true));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Builds the JPA Specification for querying completed tasks.
     */
    private Specification<TaskSubmission> createCompletedTaskSpecification(UUID userId, UUID skillIdFilter, CompletedTaskPeriod periodFilter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("userId"), userId));
            predicates.add(cb.equal(root.get("isCorrect"), true));

            if (skillIdFilter != null) {
                Join<TaskSubmission, Task> taskJoin = root.join("task");
                Join<Task, Object> taskDefJoin = taskJoin.join("taskDefinition");
                Join<Object, Object> skillJoin = taskDefJoin.join("skill");
                predicates.add(cb.equal(skillJoin.get("id"), skillIdFilter));
            }

            LocalDateTime startDate = periodFilter.getStartDateTime();
            LocalDateTime endDate = periodFilter.getEndDateTime();

            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("submittedAt"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThan(root.get("submittedAt"), endDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Creates a serializable empty response to avoid JSON errors with Page.empty().
     */
    private UserTasksResponse createEmptyUserTasksResponse(int pendingPage, int pendingSize, int completedPage, int completedSize) {
        Pageable emptyPendingPageable = PageRequest.of(pendingPage, pendingSize);
        Pageable emptyCompletedPageable = PageRequest.of(completedPage, completedSize);

        return new UserTasksResponse(
                new PageImpl<>(Collections.emptyList(), emptyPendingPageable, 0),
                new PageImpl<>(Collections.emptyList(), emptyCompletedPageable, 0)
        );
    }

}