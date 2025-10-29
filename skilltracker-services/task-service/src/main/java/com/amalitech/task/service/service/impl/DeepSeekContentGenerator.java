package com.amalitech.task.service.service.impl;

import com.amalitech.task.service.model.content.impl.CodingTaskContent;
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
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@Slf4j
public class DeepSeekContentGenerator implements ContentGeneratorService {
    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;
    private final TaskRepository taskRepository;
    private final TaskDefinitionRepository taskDefinitionRepository;

    private final PromptTemplate mcqPromptTemplate;
    private final PromptTemplate codingPromptTemplate;

    public DeepSeekContentGenerator(ChatModel chatModel,
                                    ObjectMapper objectMapper,
                                    TaskRepository taskRepository,
                                    TaskDefinitionRepository taskDefinitionRepository,
                                    PromptTemplate mcqPromptTemplate, PromptTemplate codingPromptTemplate
    ) {
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
        this.taskRepository = taskRepository;
        this.taskDefinitionRepository = taskDefinitionRepository;
        this.mcqPromptTemplate = mcqPromptTemplate;
        this.codingPromptTemplate = codingPromptTemplate;
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

        McqTaskContent content = parseMcqResponseToPojo(aiResponse);

        return createAndSaveTask(skill, TaskType.MULTIPLE_CHOICE, difficulty, content, topic);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public List<Task> generateCodingTask(SkillView skill, TaskDifficulty difficulty, String topic) {
        log.info("Generating Coding tasks for skill: {}, difficulty: {}, topic: {}",
                skill.getName(), difficulty, topic);

        Map<String, Object> promptParameters = Map.of(
                "skill", skill.getName(),
                "difficulty", difficulty.name(),
                "topic", topic
        );

        Prompt prompt = codingPromptTemplate.create(promptParameters);
        String aiResponse = callDeepSeek(prompt);

        List<JsonNode> challengeNodes = parseCodingResponseToNodes(aiResponse);

        List<Task> savedTasks = new ArrayList<>();
        for (JsonNode challengeNode : challengeNodes) {
            Task savedTask = createAndSaveCodingTask(skill, difficulty, challengeNode);
            savedTasks.add(savedTask);
        }
        return savedTasks;
    }

    /**
     * creates a Coding Task from the raw AI JSON.
     * This bridges the gap between the prompt JSON and the Task/TaskContent entities.
     */
    private Task createAndSaveCodingTask(SkillView skill, TaskDifficulty difficulty, JsonNode challengeNode) {
        String title = challengeNode.path("title").asText("AI-Generated Coding Task");
        String description = challengeNode.path("description").asText("AI-generated description.");
        int xpReward = challengeNode.path("maxXP").asInt(25);
        int duration = challengeNode.path("estimatedDuration").asInt(15);

        CodingTaskContent content = new CodingTaskContent();
        content.setPrompt(
                challengeNode.path("description").asText() +
                        "\n\n" +
                        challengeNode.path("detailedRequirements").asText()
        );
        content.setConstraints(challengeNode.path("constraints").asText("No specific constraints."));

        List<CodingTaskContent.Example> examples = StreamSupport.stream(challengeNode.path("testCases").spliterator(), false)
                .filter(node -> !node.path("isHidden").asBoolean(false))
                .map(node -> CodingTaskContent.Example.builder()
                        .input(node.path("input").asText())
                        .output(node.path("expectedOutput").asText())
                        .build())
                .collect(Collectors.toList());
        content.setExamples(examples);

        TaskDefinition definition = taskDefinitionRepository
                .findBySkillIdAndTitle(skill.getId(), title)
                .orElseGet(() -> {
                    TaskDefinition def = new TaskDefinition();
                    def.setSkill(skill);
                    def.setTitle(title);
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
                .type(TaskType.CODING)
                .difficulty(difficulty)
                .content(content)
                .xpReward(xpReward)
                .estimatedDurationInMinutes(duration)
                .isPublished(true)
                .build();

        return taskRepository.save(task);
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

    private McqTaskContent parseMcqResponseToPojo(String response) {
        try {
            JsonNode json = objectMapper.readTree(response);

            return objectMapper.treeToValue(json, McqTaskContent.class);

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse MCQ content", e);
        } catch (Exception e) {
            log.error("Invalid MCQ JSON structure: {}", response, e);
            throw new RuntimeException("Invalid fields in MCQ response", e);
        }
    }

    /**
     * Parses the AI response JSON into a list of JsonNodes, one for each challenge.
     */
    private List<JsonNode> parseCodingResponseToNodes(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode challengesNode = root.path("challenges");

            if (challengesNode.isMissingNode() || !challengesNode.isArray()) {
                throw new RuntimeException("Missing or invalid 'challenges' array in AI response");
            }

            return StreamSupport.stream(challengesNode.spliterator(), false)
                    .collect(Collectors.toList());

        } catch (JsonProcessingException e) {
            log.error("Failed to parse coding challenge JSON: {}", response, e);
            throw new RuntimeException("Failed to parse AI coding response", e);
        }
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

}