package com.amalitech.task.service.service.impl;

import com.amalitech.task.service.model.content.impl.CodingTaskContent;
import com.amalitech.task.service.service.ContentGeneratorService;
import com.amalitech.task.service.model.Task;
import com.amalitech.task.service.model.TaskDefinition;
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
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@Slf4j
public class ContentGeneratorServiceImpl implements ContentGeneratorService {
    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;
    private final TaskRepository taskRepository;
    private final TaskDefinitionRepository taskDefinitionRepository;

    private final PromptTemplate codingPromptTemplate;

    public ContentGeneratorServiceImpl(ChatModel chatModel,
                                       ObjectMapper objectMapper,
                                       TaskRepository taskRepository,
                                       TaskDefinitionRepository taskDefinitionRepository,
                                       PromptTemplate codingPromptTemplate
    ) {
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
        this.taskRepository = taskRepository;
        this.taskDefinitionRepository = taskDefinitionRepository;
        this.codingPromptTemplate = codingPromptTemplate;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public List<Task> generateCodingTask(SkillView skill, TaskDifficulty difficulty, String topic) {
        log.info("Generating Coding tasks via OpenAI for skill: {}, difficulty: {}, topic: {}",
                skill.getName(), difficulty, topic);

        Map<String, Object> promptParameters = Map.of(
                "skill", skill.getName(),
                "difficulty", difficulty.name(),
                "topic", topic
        );

        Prompt prompt = codingPromptTemplate.create(promptParameters);
        String aiResponse = callOpenAI(prompt);

        List<JsonNode> challengeNodes = parseCodingResponseToNodes(aiResponse);

        List<Task> savedTasks = new ArrayList<>();
        for (JsonNode challengeNode : challengeNodes) {
            Task savedTask = createAndSaveCodingTask(skill, difficulty, challengeNode);
            savedTasks.add(savedTask);
            log.info("Successfully created coding task: {} (ID: {})",
                    savedTask.getTitle(), savedTask.getId());
        }

        log.info("Generated {} coding tasks for topic: {}", savedTasks.size(), topic);
        return savedTasks;
    }

    /**
     * Creates and persists a coding task from AI-generated JSON.
     *
     * @param skill the skill view
     * @param difficulty the task difficulty
     * @param challengeNode the JSON node containing challenge data
     * @return the persisted coding task
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

        List<CodingTaskContent.Example> examples = StreamSupport
                .stream(challengeNode.path("testCases").spliterator(), false)
                .filter(node -> !node.path("isHidden").asBoolean(false))
                .map(node -> CodingTaskContent.Example.builder()
                        .input(node.path("input").asText())
                        .output(node.path("expectedOutput").asText())
                        .build())
                .collect(Collectors.toList());
        content.setExamples(examples);

        content.setStarterCode(challengeNode.path("starterCode").asText(null));

        List<CodingTaskContent.TestCase> testCases = StreamSupport
                .stream(challengeNode.path("testCases").spliterator(), false)
                .map(node -> CodingTaskContent.TestCase.builder()
                        .input(node.path("input").asText())
                        .expectedOutput(node.path("expectedOutput").asText())
                        .isHidden(node.path("isHidden").asBoolean(false))
                        .description(node.path("description").asText(null))
                        .build())
                .collect(Collectors.toList());
        content.setTestCases(testCases);

        JsonNode evalCriteriaNode = challengeNode.path("evaluationCriteria");
        if (!evalCriteriaNode.isMissingNode()) {
            CodingTaskContent.EvaluationCriteria evaluationCriteria = CodingTaskContent.EvaluationCriteria.builder()
                    .correctness(jsonArrayToStringList(evalCriteriaNode.path("correctness")))
                    .efficiency(jsonArrayToStringList(evalCriteriaNode.path("efficiency")))
                    .style(jsonArrayToStringList(evalCriteriaNode.path("style")))
                    .build();
            content.setEvaluationCriteria(evaluationCriteria);
        }

        List<String> hints = jsonArrayToStringList(challengeNode.path("hints"));
        content.setHints(hints);

        TaskDefinition definition = getOrCreateTaskDefinition(skill, title);

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
     * Parses coding challenges response into a list of JsonNodes.
     *
     * @param response the JSON response containing multiple challenges
     * @return list of challenge nodes
     * @throws RuntimeException if parsing fails or structure is invalid
     */
    private List<JsonNode> parseCodingResponseToNodes(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode challengesNode = root.path("challenges");

            if (challengesNode.isMissingNode() || !challengesNode.isArray()) {
                throw new RuntimeException("Missing or invalid 'challenges' array in OpenAI response");
            }

            return StreamSupport.stream(challengesNode.spliterator(), false)
                    .collect(Collectors.toList());

        } catch (JsonProcessingException e) {
            log.error("Failed to parse coding challenge JSON: {}", response, e);
            throw new RuntimeException("Failed to parse OpenAI coding response", e);
        }
    }

    /**
     * Calls the OpenAI API via Spring AI ChatModel.
     * Implements retry logic and error handling for API calls.
     *
     * @param prompt the structured prompt to send to OpenAI
     * @return the cleaned JSON response from OpenAI
     * @throws RuntimeException if the API call fails after retries
     */
    private String callOpenAI(Prompt prompt) {
        int maxRetries = 3;
        int retryCount = 0;

        while (true) {
            try {
                log.debug("Calling OpenAI API (attempt {}/{})", retryCount + 1, maxRetries);

                ChatResponse response = chatModel.call(prompt);
                String content = response.getResult().getOutput().getText();

                log.debug("OpenAI response received: {} characters", content.length());

                return cleanJsonResponse(content);

            } catch (Exception e) {
                retryCount++;
                log.error("OpenAI API call failed (attempt {}/{}): {}",
                        retryCount, maxRetries, e.getMessage());

                if (retryCount >= maxRetries) {
                    log.error("Failed to call OpenAI after {} attempts", maxRetries, e);
                    throw new RuntimeException("Failed to generate task content via OpenAI", e);
                }

                try {
                    Thread.sleep(1000L * retryCount);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted during retry", ie);
                }
            }
        }

    }

    /**
     * Gets or creates a task definition with proper version management.
     *
     * @param skill the skill view
     * @param title the task title
     * @return the task definition with incremented version
     */
    private TaskDefinition getOrCreateTaskDefinition(SkillView skill, String title) {
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
        return taskDefinitionRepository.save(definition);
    }

    /**
     * Cleans the AI response by removing markdown code block markers.
     * OpenAI sometimes wraps JSON in markdown code blocks.
     *
     * @param response the raw response from OpenAIyes
     * @return the cleaned JSON string
     */
    private String cleanJsonResponse(String response) {
        response = response.trim();

        if (response.startsWith("```json")) {
            response = response.substring(7);
        } else if (response.startsWith("```")) {
            response = response.substring(3);
        }

        if (response.endsWith("```")) {
            response = response.substring(0, response.length() - 3);
        }

        return response.trim();
    }

    /**
     * Converts a JSON array node to a list of strings.
     *
     * @param arrayNode the JSON array node
     * @return list of strings, or empty list if node is missing/invalid
     */
    private List<String> jsonArrayToStringList(JsonNode arrayNode) {
        if (arrayNode.isMissingNode() || !arrayNode.isArray()) {
            return List.of();
        }
        return StreamSupport.stream(arrayNode.spliterator(), false)
                .map(JsonNode::asText)
                .collect(Collectors.toList());
    }
}