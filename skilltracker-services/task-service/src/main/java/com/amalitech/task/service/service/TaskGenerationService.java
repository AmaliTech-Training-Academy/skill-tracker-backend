package com.amalitech.task.service.service;

import com.amalitech.task.service.config.RabbitMQConfig;
import com.amalitech.task.service.dto.request.BatchGenerationRequest;
import com.amalitech.task.service.dto.request.GenerateTaskRequest;
import com.amalitech.task.service.model.Task;
import com.amalitech.task.service.model.TaskDefinition;
import com.amalitech.task.service.model.content.TaskContent;
import com.amalitech.task.service.model.content.impl.McqTaskContent;
import com.amalitech.task.service.model.enums.TaskDifficulty;
import com.amalitech.task.service.model.enums.TaskType;
import com.amalitech.task.service.model.view.SkillView;
import com.amalitech.task.service.repository.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.model.ChatModel;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class TaskGenerationService {

    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;
    private final TaskRepository taskRepository;
    private final TaskDefinitionRepository taskDefinitionRepository;
    private final SkillViewRepository skillViewRepository;
    private final StringRedisTemplate redisTemplate;

    private static final String LOCK_PREFIX = "lock:task-gen:";
    private static final Duration LOCK_TIMEOUT = Duration.ofMinutes(5);

    public TaskGenerationService(
            ChatModel chatModel,
            ObjectMapper objectMapper,
            TaskRepository taskRepository,
            TaskDefinitionRepository taskDefinitionRepository,
            SkillViewRepository skillViewRepository, StringRedisTemplate redisTemplate
    ) {
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
        this. taskRepository =  taskRepository;
        this.taskDefinitionRepository = taskDefinitionRepository;
        this.skillViewRepository = skillViewRepository;
        this.redisTemplate = redisTemplate;
    }

    /**
     * LISTENER 1: For USER "batch" requests (Our incremental MCQ-only flow)
     * This one USES THE REDIS LOCK to prevent thundering herds.
     */
    @RabbitListener(queues = RabbitMQConfig.BATCH_GENERATION_QUEUE)
    @Transactional
    public void handleBatchGenerationRequest(BatchGenerationRequest request) {
        log.info("Received BATCH request: {}", request);

        String lockKey = LOCK_PREFIX + request.getSkillName() + ":" + request.getDifficulty();
        Boolean lockAcquired = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "in-progress", LOCK_TIMEOUT);

        if (Boolean.FALSE.equals(lockAcquired)) {
            log.warn("Batch job for {} is already in progress. Skipping.", lockKey);
            return;
        }

        try {
            log.info("Acquired lock {}. Generating {} MCQ tasks...", lockKey, request.getRequiredCount());
            SkillView skill = skillViewRepository.findByName(request.getSkillName())
                    .orElseThrow(() -> new RuntimeException("Skill not found: " + request.getSkillName()));

            for (int i = 0; i < request.getRequiredCount(); i++) {
                String topic = String.format("A question about %s fundamentals", request.getSkillName());
                generateMcqTask(skill, request.getDifficulty(), topic);
            }

            log.info("Batch generation complete for {}", lockKey);

        } catch (Exception e) {
            log.error("Failed to generate BATCH tasks for {}: {}", lockKey, e.getMessage(), e);
        } finally {
            redisTemplate.delete(lockKey);
            log.info("Released lock {}.", lockKey);
        }
    }

    /**
     * LISTENER 2: For ADMIN "special order" requests
     * This one DOES NOT NEED A LOCK, as it's a specific, manual action.
     * This is where we will build out all task types.
     */
    @RabbitListener(queues = RabbitMQConfig.ADMIN_GENERATION_QUEUE)
    @Transactional
    public void handleAdminGenerationRequest(GenerateTaskRequest request) {
        log.info("Received ADMIN request: {}", request);

        try {
            SkillView skill = skillViewRepository.findByName(request.skillName())
                    .orElseThrow(() -> new RuntimeException("Skill not found: " + request.skillName()));

            switch (request.taskType()) {
                case MULTIPLE_CHOICE:
                    log.info("Generating ADMIN MCQ...");
                    generateMcqTask(skill, request.difficulty(), request.topic());
                    break;

                case CODING:
                    log.info("Generating ADMIN CODING task...");
                    // This is where we'll add the coding generation logic next
                    // generateCodingTask(skill, request.difficulty(), request.topic(), request.languageName());
                    log.warn("Coding task generation is not yet implemented.");
                    break;

                case ESSAY:
                    log.info("Generating ADMIN ESSAY task...");
                    // This is where we'll add the essay generation logic
                    // generateEssayTask(skill, request.difficulty(), request.topic());
                    log.warn("Essay task generation is not yet implemented.");
                    break;
            }
            log.info("Admin task generation complete for {}", request.topic());

        } catch (Exception e) {
            log.error("Failed to generate ADMIN task {}: {}", request, e.getMessage(), e);
            // We don't release a lock, but we could send a "failed"
            // notification back to the admin via WebSocket or email.
        }
    }

    /**
     * This is the core logic, now private and called by both listeners.
     */
    @Transactional
    private void generateMcqTask(SkillView skill, TaskDifficulty difficulty, String topic) {
        log.info("Generating MCQ task for skill: {}, difficulty: {}, topic: {}",
                skill.getName(), difficulty, topic);

        String prompt = buildMcqPrompt(skill.getName(), difficulty, topic);
        String aiResponse = callDeepSeek(prompt);

        McqTaskContent content = parseMcqResponse(aiResponse);

        createAndSaveTask(skill, TaskType.MULTIPLE_CHOICE, difficulty, content, topic);
    }

    // ========== PROMPT BUILDER (MCQ ONLY) ==========

    private String buildMcqPrompt(String skillName, TaskDifficulty difficulty, String topic) {
        return String.format("""
            You are an expert educator creating a multiple-choice question.
            
            Skill: %s
            Difficulty: %s
            Topic: %s
            
            Generate a high-quality MCQ in JSON format with this exact structure:
            {
              "title": "Short descriptive title (max 60 chars)",
              "description": "Brief description of what this tests (1-2 sentences)",
              "question": "The actual question text",
              "options": ["option1", "option2", "option3", "option4"],
              "correctOption": 0,
              "explanation": "Detailed explanation of why the answer is correct",
              "xpReward": 10,
              "estimatedDuration": 2
            }
            
            Requirements:
            - Exactly 4 options
            - correctOption is the index (0-3) of the correct answer
            - Question should test understanding, not just memorization
            - Explanation should be educational and clear
            - For EASY: Basic concepts
            - For MEDIUM: Application of concepts
            - For HARD: Advanced scenarios or edge cases
            
            Return ONLY valid JSON, no markdown, no explanation.
            """, skillName, difficulty, topic);
    }

    private String callDeepSeek(String promptText) {
        try {
            log.debug("Calling DeepSeek API with prompt length: {}", promptText.length());

            var systemMessage = new SystemMessage(
                    "You are an expert educational content creator. Always respond with valid JSON only. No markdown, no explanations."
            );
            var userMessage = new UserMessage(promptText);

            var prompt = new Prompt(
                    List.of(systemMessage, userMessage),
                    DeepSeekChatOptions.builder()
                            .model(DeepSeekApi.ChatModel.DEEPSEEK_CHAT.getValue())
                            .temperature(0.3)
                            .build()
            );

            ChatResponse response = chatModel.call(prompt);

            String content = response.getResult().getOutput().getText();

            log.debug("DeepSeek response: {}", content);

            return cleanJsonResponse(content);

        } catch (Exception e) {
            log.error("Error calling DeepSeek API: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate task content", e);
        }
    }

    private String cleanJsonResponse(String response) {
        response = response.trim();
        if (response.startsWith("```json")) {
            response = response.substring(7);
        }
        if (response.startsWith("```")) {
            response = response.substring(3);
        }
        if (response.endsWith("```")) {
            response = response.substring(0, response.length() - 3);
        }
        return response.trim();
    }

    // ========== PARSER (MCQ ONLY) ==========

    private McqTaskContent parseMcqResponse(String response) {
        try {
            JsonNode json = objectMapper.readTree(response);

            List<String> options = new ArrayList<>();
            json.get("options").forEach(node -> options.add(node.asText()));

            return McqTaskContent.builder()
                    .question(json.get("question").asText())
                    .options(options)
                    .correctOption(json.get("correctOption").asInt())
                    .explanation(json.get("explanation").asText())
                    .build();

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse MCQ content", e);
        }
    }

    private Task createAndSaveTask(SkillView skill, TaskType type, TaskDifficulty difficulty,
                                   TaskContent content, String topic) {
        try {
            JsonNode json = objectMapper.valueToTree(content);

            String title = json.has("title") ? json.get("title").asText() : topic;
            String description = json.has("description") ?
                    json.get("description").asText() : "AI-generated task";
            int xpReward = json.has("xpReward") ? json.get("xpReward").asInt() : 10;
            int duration = json.has("estimatedDuration") ?
                    json.get("estimatedDuration").asInt() : 10;

            if (!json.has("title")) {
                title = topic + " #" + UUID.randomUUID().toString().substring(0, 4);
            }

            String finalTitle = title;
            TaskDefinition definition = taskDefinitionRepository
                    .findBySkillIdAndTitle(skill.getId(), title)
                    .orElseGet(() -> {
                        TaskDefinition def = new TaskDefinition();
                        def.setSkill(skill);
                        def.setTitle(finalTitle);
                        def.setLatestVersion(0);
                        return taskDefinitionRepository.save(def);
                    });

            definition.setLatestVersion(definition.getLatestVersion() + 1);
            taskDefinitionRepository.save(definition);

            Task task = Task.builder()
                    .taskDefinition(definition)
                    .version(definition.getLatestVersion())
                    .title(title)
                    .description(description)
                    .type(type)
                    .difficulty(difficulty)
                    .content(content)
                    .xpReward(xpReward)
                    .estimatedDurationInMinutes(duration)
                    .isPublished(true)
                    .build();

            return taskRepository.save(task);

        } catch (Exception e) {
            log.error("Error creating task: {}", e.getMessage());
            throw new RuntimeException("Failed to create task", e);
        }
    }
}