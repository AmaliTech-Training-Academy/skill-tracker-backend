package com.amalitech.task.service.service;

import com.amalitech.task.service.dto.StarterCodeDTO;
import com.amalitech.task.service.dto.TaskDTO;
import com.amalitech.task.service.dto.TaskAvailabilityDTO;
import com.amalitech.task.service.dto.TestCaseDTO;
import com.amalitech.task.service.dto.request.BatchGenerationRequest;
import com.amalitech.task.service.events.TaskEventProducer;
import com.amalitech.task.service.exception.ResourceNotFoundException;
import com.amalitech.task.service.model.Task;
import com.amalitech.task.service.model.TaskStarterCode;
import com.amalitech.task.service.model.TaskTestCase;
import com.amalitech.task.service.model.enums.TaskDifficulty;
import com.amalitech.task.service.model.view.SkillView;
import com.amalitech.task.service.repository.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TaskService {

    private final TaskRepository taskRepository;
    private final SkillViewRepository skillViewRepository;
    private final TaskSubmissionRepository submissionRepository;
    private final TaskTestCaseRepository testCaseRepository;
    private final TaskStarterCodeRepository starterCodeRepository;
    private final TaskEventProducer taskEventProducer;

    @Value("${app.task.min-tasks-per-difficulty:5}")
    private int minTasksPerDifficulty;

    /**
     * Get personalized tasks. This flow is now asynchronous.
     */
    public List<TaskDTO> getPersonalizedTasks(UUID userId, String skillName, int limit) {
        log.info("Fetching personalized tasks for user: {}, skill: {}", userId, skillName);

        SkillView skill = getSkillByName(skillName);
        TaskDifficulty difficulty = determineUserDifficulty(userId, skill.getId());
        log.debug("Determined difficulty: {} for user: {}", difficulty, userId);

        // Get or generate tasks. This method is now async.
        List<Task> tasks = getOrGenerateTasksForSkillAndDifficulty(
                skill, difficulty, limit
        );

        return tasks.stream()
                .map(task -> toDTO(task, skill.getName()))
                .collect(Collectors.toList());
    }

    /**
     * Get tasks for onboarding/initial selection.
     * Checks cache, and if cache miss, triggers async generation.
     */
    public List<TaskDTO> getTasksForSkillAndDifficulty(String skillName, TaskDifficulty difficulty, int limit) {
        log.info("Getting tasks for skill: {}, difficulty: {}, limit: {}", skillName, difficulty, limit);

        SkillView skill = getSkillByName(skillName);

        List<Task> tasks = getOrGenerateTasksForSkillAndDifficulty(
                skill, difficulty, limit
        );

        return tasks.stream()
                .map(task -> toDTO(task, skill.getName()))
                .collect(Collectors.toList());
    }

    private List<Task> getOrGenerateTasksForSkillAndDifficulty(
            SkillView skill, TaskDifficulty difficulty, int limit) {

        // 1. Check the cache
        List<Task> cachedTasks = taskRepository.findBySkillAndDifficulty(
                skill.getId(),
                difficulty,
                true,
                PageRequest.of(0, limit) // Use pagination
        );

        if (cachedTasks.size() >= limit) {
            log.info("Cache hit: Using {} cached tasks for {}/{}",
                    cachedTasks.size(), skill.getName(), difficulty);
            return cachedTasks; // Return full list immediately
        }

        // 2. Cache Miss: We don't have enough tasks.
        log.info("Cache miss for {}/{}. Have {}, need {}. Triggering async generation.",
                skill.getName(), difficulty, cachedTasks.size(), limit);

        // 3. Trigger asynchronous generation (Fire and Forget)
        BatchGenerationRequest request = BatchGenerationRequest.builder()
                .skillName(skill.getName())
                .difficulty(difficulty)
                // We'll generate a standard batch, e.g., 5.
                .requiredCount(minTasksPerDifficulty)
                .build();

        taskEventProducer.requestBatchTaskGeneration(request);

        // 4. Return what we have *right now* (even if it's an empty list)
        // The frontend will show these tasks (or a "generating" message)
        // and poll to get the new ones later.
        return cachedTasks;
    }

    private SkillView getSkillByName(String skillName) {
        return skillViewRepository.findByName(skillName)
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found: " + skillName));
    }

//    public TaskDTO getTaskById(UUID taskId) {
//        log.info("Fetching task by ID: {}", taskId);
//
//        Task task = taskRepository.findById(taskId)
//                .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskId));
//
//        // Get skill info from the (mandatory) TaskDefinition
//        SkillView skill = task.getTaskDefinition().getSkill();
//
//        TaskDTO dto = toDTO(task, skill.getName());
//
//        // Add starter codes
//        List<TaskStarterCode> starterCodes = starterCodeRepository.findByTaskId(taskId);
//        dto.setStarterCodes(starterCodes.stream()
//                .map(sc -> StarterCodeDTO.builder()
//                        .languageId(sc.getProgrammingLanguage().getId())
//                        .languageName(sc.getProgrammingLanguage().getName())
//                        .code(sc.getCode())
//                        .build())
//                .collect(Collectors.toList()));
//
//        // Add visible test cases
//        List<TaskTestCase> visibleTestCases = testCaseRepository
//                .findVisibleTestCasesByTaskId(taskId);
//
//        dto.setVisibleTestCases(visibleTestCases.stream()
//                .map(tc -> TestCaseDTO.builder()
//                        .input(tc.getInput())
//                        .expectedOutput(tc.getExpectedOutput())
//                        .weight(tc.getWeight())
//                        .build())
//                .collect(Collectors.toList()));
//
//        return dto;
//    }
//
//    public TaskAvailabilityDTO checkTaskAvailability(String skillName, TaskDifficulty difficulty) {
//        SkillView skill = skillViewRepository.findByName(skillName)
//                .orElseThrow(() -> new ResourceNotFoundException("Skill not found: " + skillName));
//
//        long availableCount = taskRepository.countBySkillAndDifficulty(
//                skill.getId(), difficulty, true
//        );
//
//        return TaskAvailabilityDTO.builder()
//                .skillName(skillName)
//                .difficulty(difficulty)
//                .availableTaskCount((int) availableCount)
//                .needsGeneration(availableCount < minTasksPerDifficulty)
//                .build();
//    }

    private TaskDifficulty determineUserDifficulty(UUID userId, UUID skillId) {
        // A simple heuristic for now.
        Long correctCount = submissionRepository.countByUserIdAndIsCorrect(userId, true);

        if (correctCount < 5) return TaskDifficulty.EASY;
        if (correctCount < 15) return TaskDifficulty.MEDIUM;
        return TaskDifficulty.HARD;
    }

    private TaskDTO toDTO(Task task, String skillName) {
        return TaskDTO.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .type(task.getType())
                .difficulty(task.getDifficulty())
                .content(task.getContent())
                .xpReward(task.getXpReward())
                .estimatedDuration(task.getEstimatedDurationInMinutes())
                .skillName(skillName)
                .version(task.getVersion())
                .build();
    }
}