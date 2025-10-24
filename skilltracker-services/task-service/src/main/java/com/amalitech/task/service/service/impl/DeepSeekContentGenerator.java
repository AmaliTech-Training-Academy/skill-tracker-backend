package com.amalitech.task.service.service.impl;

import com.amalitech.task.service.service.ContentGeneratorService;
import com.amalitech.task.service.model.Task;
import com.amalitech.task.service.model.TaskDefinition;
import com.amalitech.task.service.model.content.TaskContent;
import com.amalitech.task.service.model.content.impl.McqTaskContent;
import com.amalitech.task.service.model.enums.TaskDifficulty;
import com.amalitech.task.service.model.enums.TaskType;
import com.amalitech.task.service.model.view.SkillView;
import com.amalitech.task.service.repository.TaskDefinitionRepository;
import com.amalitech.task.service.repository.TaskRepository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class DeepSeekContentGenerator implements ContentGeneratorService {
    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;
    private final TaskRepository taskRepository;
    private final TaskDefinitionRepository taskDefinitionRepository;

    private final PromptTemplate mcqPromptTemplate;

    public DeepSeekContentGenerator(ChatModel chatModel,
                                    ObjectMapper objectMapper,
                                    TaskRepository taskRepository,
                                    TaskDefinitionRepository taskDefinitionRepository,
                                    PromptTemplate mcqPromptTemplate
    ) {
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
        this.taskRepository = taskRepository;
        this.taskDefinitionRepository = taskDefinitionRepository;
        this.mcqPromptTemplate = mcqPromptTemplate;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public Task generateMcqTask(SkillView skill, TaskDifficulty difficulty, String topic) {
        log.info("Generating MCQ task for skill: {}, difficulty: {}, topic: {}",
                skill.getName(), difficulty, topic);

        Map<String, Object> promptParameters = Map.of(
                "skill", skill.getName(),
                "difficulty", difficulty.name(),
                "topic", topic
        );

        Prompt prompt = mcqPromptTemplate.create(promptParameters);
        String aiResponse = callDeepSeek(prompt);
        McqTaskContent content = parseMcqResponse(aiResponse);

        return createAndSaveTask(skill, TaskType.MULTIPLE_CHOICE, difficulty, content, topic);
    }

    /**
     * Calls the DeepSeek AI model with the provided prompt.
     * This method sends a structured request to the AI model with system and user messages,
     * ensuring the response is in valid JSON format without markdown formatting.
     *
     * @param prompt the prompt text to send to the AI model
     * @return the cleaned JSON response from the AI model
     * @throws RuntimeException if the API call fails or encounters an error
     */
    private String callDeepSeek(Prompt prompt) {
        try {
            log.debug("Calling DeepSeek API with prompt...");

            ChatResponse response = chatModel.call(prompt);

            String content = response.getResult().getOutput().getText();
            log.debug("DeepSeek response: {}", content);
            return cleanJsonResponse(content);

        } catch (Exception e) {
            log.error("Error calling DeepSeek API: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate task content", e);
        }
    }


    /**
     * Cleans the AI response by removing markdown code block markers.
     * Handles responses that may be wrapped in markdown JSON code blocks
     * (e.g., ```json ... ``` or ``` ... ```).
     *
     * @param response the raw response from the AI model
     * @return the cleaned JSON string without markdown markers
     */
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


    /**
     * Parses the AI response JSON into an MCQ content object.
     * Expected JSON structure:
     * <pre>
     * {
     *   "question": "The question text",
     *   "options": ["Option 1", "Option 2", "Option 3", "Option 4"],
     *   "correctOption": 0,
     *   "explanation": "Explanation of the correct answer"
     * }
     * </pre>
     *
     * @param response the JSON response string from the AI model
     * @return the parsed McqTaskContent object
     * @throws RuntimeException if JSON parsing fails or required fields are missing
     */
    private McqTaskContent parseMcqResponse(String response) {
        try {
            JsonNode json = objectMapper.readTree(response);

            JsonNode optionsNode = json.get("options");
            if (optionsNode == null || !optionsNode.isArray() || optionsNode.isEmpty()) {
                throw new RuntimeException("Missing or invalid 'options' field in MCQ response");
            }
            JsonNode questionNode = json.get("question");
            if (questionNode == null || questionNode.asText().isEmpty()) {
                throw new RuntimeException("Missing or invalid 'question' field in MCQ response");
            }
            JsonNode correctOptionNode = json.get("correctOption");
            if (correctOptionNode == null || !correctOptionNode.isInt()) {
                throw new RuntimeException("Missing or invalid 'correctOption' field in MCQ response");
            }
            JsonNode explanationNode = json.get("explanation");
            if (explanationNode == null || explanationNode.asText().isEmpty()) {
                throw new RuntimeException("Missing or invalid 'explanation' field in MCQ response");
            }

            List<String> options = new ArrayList<>();
            optionsNode.forEach(node -> options.add(node.asText()));

            return McqTaskContent.builder()
                    .question(questionNode.asText())
                    .options(options)
                    .correctOption(correctOptionNode.asInt())
                    .explanation(explanationNode.asText())
                    .build();

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse MCQ content", e);
        }
    }

    /**
     * Creates and persists a task with its associated task definition.
     * This method handles:
     * <ul>
     *   <li>Extracting metadata from the content (title, description, xp, duration)</li>
     *   <li>Creating or retrieving the task definition with version management</li>
     *   <li>Building the complete task entity with all required fields</li>
     *   <li>Persisting the task to the database</li>
     * </ul>
     *
     * <p>If the task definition doesn't exist, it creates a new one. Otherwise,
     * it increments the version number for the existing definition.</p>
     *
     * @param skill the skill view associated with the task
     * @param type the task type (e.g., MULTIPLE_CHOICE)
     * @param difficulty the difficulty level of the task
     * @param content the generated task content
     * @param topic the topic used as fallback for title generation
     * @return the persisted Task entity
     * @throws RuntimeException if task creation or persistence fails
     */
    private Task createAndSaveTask(SkillView skill, TaskType type, TaskDifficulty difficulty,
                                   TaskContent content, String topic) {
        try {
            JsonNode json = objectMapper.valueToTree(content);

            String title;
            if (json.has("title")) {
                title = json.get("title").asText();
            } else {
                title = topic + " #" + UUID.randomUUID().toString().substring(0, 4);
            }
            String description = json.has("description") ?
                    json.get("description").asText() : "AI-generated task";
            int xpReward = json.has("xpReward") ? json.get("xpReward").asInt() : 10;
            int duration = json.has("estimatedDuration") ?
                    json.get("estimatedDuration").asInt() : 10;

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