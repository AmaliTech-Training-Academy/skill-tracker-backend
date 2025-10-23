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
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class DeepSeekContentGenerator implements ContentGeneratorService {
    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;
    private final TaskRepository taskRepository;
    private final TaskDefinitionRepository taskDefinitionRepository;

    @Value("classpath:prompts/mcq_prompt.txt")
    private String mcqPromptTemplate;

    public DeepSeekContentGenerator(ChatModel chatModel,
                                    ObjectMapper objectMapper,
                                    TaskRepository taskRepository,
                                    TaskDefinitionRepository taskDefinitionRepository
    ) {
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
        this.taskRepository = taskRepository;
        this.taskDefinitionRepository = taskDefinitionRepository;
    }

    @Override
    @Transactional
    public Task generateMcqTask(SkillView skill, TaskDifficulty difficulty, String topic) {
        log.info("Generating MCQ task for skill: {}, difficulty: {}, topic: {}",
                skill.getName(), difficulty, topic);

        String prompt = buildMcqPrompt(skill.getName(), difficulty, topic);
        String aiResponse = callDeepSeek(prompt);

        McqTaskContent content = parseMcqResponse(aiResponse);

        return createAndSaveTask(skill, TaskType.MULTIPLE_CHOICE, difficulty, content, topic);
    }

    private String buildMcqPrompt(String skillName, TaskDifficulty difficulty, String topic) {
        return String.format(mcqPromptTemplate, skillName, difficulty, topic);
    }

    private String callDeepSeek(String promptText) {
        try {
            log.debug("Calling DeepSeek API with prompt length: {}", promptText.length());

            var systemMessage = new SystemMessage(
                    "You are an expert educational content creator. Always respond with valid JSON only. No markdown, no explanations."
            );
            var userMessage = new UserMessage(promptText);

            var prompt = new Prompt(List.of(systemMessage, userMessage));
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

