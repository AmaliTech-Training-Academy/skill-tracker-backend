package com.amalitech.task.service.service.impl;

import com.amalitech.task.service.exception.AiResponseParsingException;
import com.amalitech.task.service.exception.AiServiceException;
import com.amalitech.task.service.exception.InvalidAiResponseException;
import com.amalitech.task.service.model.Task;
import com.amalitech.task.service.model.TaskDefinition;
import com.amalitech.task.service.model.content.impl.CodingTaskContent;
import com.amalitech.task.service.model.content.impl.EssayTaskContent;
import com.amalitech.task.service.model.content.impl.McqTaskContent;
import com.amalitech.task.service.model.enums.TaskDifficulty;
import com.amalitech.task.service.model.enums.TaskType;
import com.amalitech.task.service.model.view.SkillView;
import com.amalitech.task.service.repository.TaskDefinitionRepository;
import com.amalitech.task.service.repository.TaskRepository;
import com.amalitech.task.service.service.ContentGeneratorService;
import com.amalitech.task.service.validation.McqContentValidator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
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
    private final PromptTemplate essayPromptTemplate;
    private final PromptTemplate mcqPromptTemplate;
    private final McqContentValidator mcqContentValidator;

    public ContentGeneratorServiceImpl(@Qualifier("flagshipChatModel") ChatModel chatModel,
                                       ObjectMapper objectMapper,
                                       TaskRepository taskRepository,
                                       TaskDefinitionRepository taskDefinitionRepository,
                                       PromptTemplate codingPromptTemplate,
                                       PromptTemplate essayPromptTemplate,
                                       PromptTemplate mcqPromptTemplate,
                                       McqContentValidator mcqContentValidator
    ) {
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
        this.taskRepository = taskRepository;
        this.taskDefinitionRepository = taskDefinitionRepository;
        this.codingPromptTemplate = codingPromptTemplate;
        this.essayPromptTemplate = essayPromptTemplate;
        this.mcqPromptTemplate = mcqPromptTemplate;
        this.mcqContentValidator = mcqContentValidator;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    @CacheEvict(cacheNames = "tasks-public-cache", allEntries = true)
    public List<Task> generateCodingTask(SkillView skill, TaskDifficulty difficulty, int quantity) {
        log.info("Generating {} Coding tasks via OpenAI for skill: {}, difficulty: {}",
                quantity, skill.getName(), difficulty);

        Map<String, Object> promptParameters = Map.of(
                "skill", skill.getName(),
                "difficulty", difficulty.name(),
                "quantity", quantity
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

        log.info("Generated {} coding tasks for topic", savedTasks.size());
        return savedTasks;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    @CacheEvict(cacheNames = "tasks-public-cache", allEntries = true)
    public List<Task> generateEssayTask(SkillView skill, TaskDifficulty difficulty, int quantity) {
        log.info("Generating {} Essay tasks via OpenAI for skill: {}, difficulty: {}",
                quantity, skill.getName(), difficulty);

        Map<String, Object> promptParameters = Map.of(
                "skill", skill.getName(),
                "difficulty", difficulty.name(),
                "quantity", quantity
        );

        Prompt prompt = essayPromptTemplate.create(promptParameters);

        String aiResponse = callOpenAI(prompt);

        List<JsonNode> taskNodes = parseEssayResponseToNodes(aiResponse);

        List<Task> savedTasks = new ArrayList<>();
        for (JsonNode taskNode : taskNodes) {
            Task savedTask = createAndSaveEssayTask(skill, difficulty, taskNode);
            savedTasks.add(savedTask);
            log.info("Successfully created essay task: {} (ID: {})",
                    savedTask.getTitle(), savedTask.getId());
        }

        log.info("Generated {} essay tasks for skill: {}", savedTasks.size(), skill.getName());
        return savedTasks;
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "tasks-public-cache", allEntries = true)
    public List<Task> generateMCQTask(SkillView skill, TaskDifficulty difficulty, int quantity) throws IOException {
        log.info("Generating {} MCQ questions via OpenAI for skill: {}, difficulty: {}",
                quantity, skill.getName(), difficulty);

        Prompt mcqPrompt = mcqPromptTemplate.create(Map.of(
                "skill", skill.getName(),
                "difficulty", difficulty.toString(),
                "quantity", String.valueOf(quantity)
        ));

        String response = callOpenAI(mcqPrompt);

        List<JsonNode> questionNodes = parseMcqResponseToNodes(response);

        Task savedTask = createAndSaveMCQTask(skill, difficulty, questionNodes);

        log.info("Generated MCQ task with {} questions (ID: {})", 
                questionNodes.size(), savedTask.getId());

        return List.of(savedTask);
    }

    private Task createAndSaveMCQTask(SkillView skill, TaskDifficulty difficulty, List<JsonNode> questionNodes) {
        String taskTitle = skill.getName() + " - MCQ Quiz";
        String description = "Multiple choice assessment for " + skill.getName();
        
        // Calculate total duration
        // Fixed: MCQ tasks are worth 50 XP total, divided equally among all questions
        int totalDuration = 0;
        for (JsonNode node : questionNodes) {
            totalDuration += node.path("question_duration").asInt(3);
        }

        TaskDefinition definition = getOrCreateTaskDefinition(skill, taskTitle);

        Task task = Task.builder()
                .taskDefinition(definition)
                .version(definition.getLatestVersion())
                .title(taskTitle)
                .description(description)
                .type(TaskType.MULTIPLE_CHOICE)
                .difficulty(difficulty)
                .content(createMCQContent(questionNodes))
                .estimatedDurationInMinutes(totalDuration)
                .xpReward(50)
                .isPublished(true)
                .updatedAt(LocalDateTime.now())
                .build();

        return taskRepository.save(task);

    }

    public McqTaskContent createMCQContent(List<JsonNode> questionNodes) {
        List<McqTaskContent.Question> questions = new ArrayList<>();
        
        // Calculate XP per question: 50 XP total divided equally among all questions
        int xpPerQuestion = Math.max(1, 50 / questionNodes.size());

        for (int i = 0; i < questionNodes.size(); i++) {
            JsonNode questionNode = questionNodes.get(i);
            List<String> options = jsonArrayToStringList(questionNode.path("options"));

            // AI returns correct_answer as string value, convert to index
            String correctAnswerString = questionNode.path("correct_answer").asText();
            int correctAnswerIndex = convertCorrectAnswerToIndex(correctAnswerString, options, String.valueOf(i + 1));

            McqTaskContent.Question question = McqTaskContent.Question.builder()
                    .question_number(String.valueOf(i + 1))
                    .question_title(questionNode.path("question_title").asText())
                    .question_description(questionNode.path("question_description").asText())
                    .question_text(questionNode.path("question_text").asText())
                    .question_duration(questionNode.path("question_duration").asInt(3))
                    .question_difficulty(questionNode.path("question_difficulty").asText())
                    .options(options)
                    .hint(questionNode.path("hint").asText())
                    .correct_answer(correctAnswerIndex)
                    .explanation(questionNode.path("explanation").asText())
                    .xpReward(xpPerQuestion)
                    .build();

            questions.add(question);
        }

        McqTaskContent content = McqTaskContent.builder()
                .questions(questions)
                .build();

        // Validate the generated content
        mcqContentValidator.validateMcqContent(content);

        return content;
    }

    /**
     * Converts the correct_answer string (as provided by AI) to an index based on its position in options.
     *
     * @param correctAnswerString The correct answer string from AI response
     * @param options The list of available options
     * @param questionNumber The question number for error reporting
     * @return The 0-based index of the correct answer in the options list
     * @throws IllegalArgumentException if the correct answer is not found in options
     */
    private int convertCorrectAnswerToIndex(String correctAnswerString, List<String> options, String questionNumber) {
        if (correctAnswerString == null || correctAnswerString.isBlank()) {
            throw new IllegalArgumentException("Correct answer is null or empty for question " + questionNumber);
        }

        int index = options.indexOf(correctAnswerString);

        if (index == -1) {
            log.error("AI-generated correct_answer '{}' not found in options for question {}. Options: {}",
                    correctAnswerString, questionNumber, options);
            throw new IllegalArgumentException(
                    "Generated correct_answer not found in options for question: " + questionNumber
            );
        }

        return index;
    }

    private List<JsonNode> parseMcqResponseToNodes(String response) {
        try {
            String fixedResponse = fixJsonFormatting(response);
            
            JsonNode root = objectMapper.readTree(fixedResponse);
            JsonNode challengesNode = root.path("expected_output");

            if (challengesNode.isMissingNode() || !challengesNode.isArray()) {
                throw new InvalidAiResponseException("Missing or invalid 'expected_output' array in OpenAI response");
            }

            return StreamSupport.stream(challengesNode.spliterator(), false)
                    .collect(Collectors.toList());

        } catch (InvalidAiResponseException e) {
            throw e;
        } catch (JsonProcessingException e) {
            log.error("Failed to parse MCQ tasks JSON: {}", response, e);
            throw new AiResponseParsingException("Failed to parse OpenAI MCQ response", e);
        }
    }

    /**
     * Fixes common JSON formatting issues from AI responses.
     * Handles literal newlines in string values by escaping them as \n.
     * 
     * @param response the JSON response
     * @return the fixed JSON response
     */
    private String fixJsonFormatting(String response) {
        // JSON requires proper escaping of special characters
        // If AI includes literal newlines in strings, they need to be escaped
        StringBuilder fixed = new StringBuilder();
        boolean inString = false;
        boolean escaped = false;
        
        for (int i = 0; i < response.length(); i++) {
            char c = response.charAt(i);
            
            if (escaped) {
                fixed.append('\\').append(c);
                escaped = false;
                continue;
            }
            
            if (c == '\\' && inString) {
                fixed.append(c);
                escaped = true;
                continue;
            }
            
            if (c == '"') {
                inString = !inString;
                fixed.append(c);
                continue;
            }
            
            // If we're in a string and encounter a literal newline, escape it
            if (inString && c == '\n') {
                fixed.append("\\n");
                continue;
            }
            
            // If we're in a string and encounter a literal carriage return, escape it
            if (inString && c == '\r') {
                fixed.append("\\r");
                continue;
            }
            
            fixed.append(c);
        }
        
        return fixed.toString();
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
        content.setSubmissionHarness(challengeNode.path("submissionHarness").asText(null));

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
                throw new InvalidAiResponseException("Missing or invalid 'challenges' array in OpenAI response");
            }

            return StreamSupport.stream(challengesNode.spliterator(), false)
                    .collect(Collectors.toList());

        } catch (InvalidAiResponseException e) {
            throw e;
        } catch (JsonProcessingException e) {
            log.error("Failed to parse coding challenge JSON: {}", response, e);
            throw new AiResponseParsingException("Failed to parse OpenAI coding response", e);
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
        try {
            log.debug("Calling Spring AI ChatModel...");

            ChatResponse response = chatModel.call(prompt);
            String content = response.getResult().getOutput().getText();

            log.debug("OpenAI response received: {} characters", content.length());
            return cleanJsonResponse(content);

        } catch (Exception e) {
            log.error("Failed to call OpenAI after all retries.", e);
            throw new AiServiceException("Failed to generate task content via OpenAI after all retries", e);
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

    /**
     * Parses essay tasks response into a list of JsonNodes.
     *
     * @param response the JSON response containing multiple essay tasks
     * @return list of task nodes
     * @throws RuntimeException if parsing fails or structure is invalid
     */
    private List<JsonNode> parseEssayResponseToNodes(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode tasksNode = root.path("tasks");

            if (tasksNode.isMissingNode() || !tasksNode.isArray()) {
                throw new InvalidAiResponseException("Missing or invalid 'tasks' array in OpenAI response");
            }

            return StreamSupport.stream(tasksNode.spliterator(), false)
                    .collect(Collectors.toList());

        } catch (InvalidAiResponseException e) {
            throw e;
        } catch (JsonProcessingException e) {
            log.error("Failed to parse essay tasks JSON: {}", response, e);
            throw new AiResponseParsingException("Failed to parse OpenAI essay response", e);
        }
    }

    /**
     * Creates and persists an essay task from AI-generated JSON.
     *
     * @param skill the skill view
     * @param difficulty the task difficulty
     * @param taskNode the JSON node containing task data
     * @return the persisted essay task
     */
    private Task createAndSaveEssayTask(SkillView skill, TaskDifficulty difficulty, JsonNode taskNode) {
        String title = taskNode.path("title").asText("AI-Generated Essay Task");
        String description = taskNode.path("description").asText("AI-generated description.");
        int xpReward = taskNode.path("maxXP").asInt(50);
        int duration = taskNode.path("estimatedDuration").asInt(20);

        EssayTaskContent content = buildEssayTaskContent(taskNode);

        TaskDefinition definition = getOrCreateTaskDefinition(skill, title);

        Task task = Task.builder()
                .taskDefinition(definition)
                .version(definition.getLatestVersion())
                .title(title)
                .description(description)
                .type(TaskType.ESSAY)
                .difficulty(difficulty)
                .content(content)
                .xpReward(xpReward)
                .estimatedDurationInMinutes(duration)
                .isPublished(true)
                .build();

        return taskRepository.save(task);
    }

    /**
     * Builds EssayTaskContent from the AI-generated JSON node.
     *
     * @param taskNode the JSON node containing essay task data
     * @return the constructed EssayTaskContent
     */
    private EssayTaskContent buildEssayTaskContent(JsonNode taskNode) {
        JsonNode evalCriteriaNode = taskNode.path("evaluationCriteria");
        JsonNode rubricNode = taskNode.path("rubric");

        EssayTaskContent.EvaluationCriteria evaluationCriteria = EssayTaskContent.EvaluationCriteria.builder()
                .completeness(jsonArrayToStringList(evalCriteriaNode.path("completeness")))
                .accuracy(jsonArrayToStringList(evalCriteriaNode.path("accuracy")))
                .clarity(jsonArrayToStringList(evalCriteriaNode.path("clarity")))
                .depth(jsonArrayToStringList(evalCriteriaNode.path("depth")))
                .build();

        EssayTaskContent.Rubric rubric = EssayTaskContent.Rubric.builder()
                .completeness(buildPerformanceLevels(rubricNode.path("completeness")))
                .accuracy(buildPerformanceLevels(rubricNode.path("accuracy")))
                .clarity(buildPerformanceLevels(rubricNode.path("clarity")))
                .depth(buildPerformanceLevels(rubricNode.path("depth")))
                .build();

        List<String> hints = jsonArrayToStringList(taskNode.path("hints"));

        return EssayTaskContent.builder()
                .prompt(taskNode.path("prompt").asText())
                .detailedInstructions(taskNode.path("detailedInstructions").asText())
                .evaluationCriteria(evaluationCriteria)
                .rubric(rubric)
                .hints(hints)
                .expectedLength(taskNode.path("expectedLength").asText("200-400 words"))
                .build();
    }

    /**
     * Builds PerformanceLevels from a rubric category node.
     *
     * @param categoryNode the JSON node for a rubric category
     * @return the constructed PerformanceLevels
     */
    private EssayTaskContent.Rubric.PerformanceLevels buildPerformanceLevels(JsonNode categoryNode) {
        return EssayTaskContent.Rubric.PerformanceLevels.builder()
                .excellent(categoryNode.path("excellent").asText())
                .good(categoryNode.path("good").asText())
                .satisfactory(categoryNode.path("satisfactory").asText())
                .needsImprovement(categoryNode.path("needsImprovement").asText())
                .build();
    }
}