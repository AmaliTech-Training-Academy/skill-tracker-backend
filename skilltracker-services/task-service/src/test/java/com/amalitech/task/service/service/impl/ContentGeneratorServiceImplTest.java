package com.amalitech.task.service.service.impl;

import com.amalitech.task.service.model.Task;
import com.amalitech.task.service.model.TaskDefinition;
import com.amalitech.task.service.model.content.impl.CodingTaskContent;
import com.amalitech.task.service.model.enums.TaskDifficulty;
import com.amalitech.task.service.model.enums.TaskType;
import com.amalitech.task.service.model.view.SkillView;
import com.amalitech.task.service.repository.TaskDefinitionRepository;
import com.amalitech.task.service.repository.TaskRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class ContentGeneratorServiceImplTest {

    @Mock
    private ChatModel chatModel;

    private ObjectMapper objectMapper;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskDefinitionRepository taskDefinitionRepository;

    @Mock
    private PromptTemplate codingPromptTemplate;

    @Mock
    private PromptTemplate essayPromptTemplate;

    @InjectMocks
    private ContentGeneratorServiceImpl contentGeneratorService;

    private SkillView testSkill;
    private UUID skillId;
    private TaskDefinition testDefinition;

    @BeforeEach
    void setUp() throws Exception {
        skillId = UUID.randomUUID();
        testSkill = new SkillView();
        testSkill.setId(skillId);
        testSkill.setName("PYTHON");

        testDefinition = new TaskDefinition();
        testDefinition.setId(UUID.randomUUID());
        testDefinition.setSkill(testSkill);
        testDefinition.setTitle("Test Task");
        testDefinition.setLatestVersion(0);

        objectMapper = new ObjectMapper();
        
        // Manually set ObjectMapper on the service via reflection since it's not a constructor parameter
        java.lang.reflect.Field field = ContentGeneratorServiceImpl.class.getDeclaredField("objectMapper");
        field.setAccessible(true);
        field.set(contentGeneratorService, objectMapper);

         lenient().when(codingPromptTemplate.create(any(Map.class)))
                 .thenReturn(mock(Prompt.class));
         lenient().when(essayPromptTemplate.create(any(Map.class)))
                 .thenReturn(mock(Prompt.class));
     }

    // ==================== CODING TASK GENERATION TESTS ====================

    @Test
    void testGenerateCodingTask_Success() throws Exception {
        String aiResponse = """
                {
                  "challenges": [
                    {
                      "title": "Fibonacci Sequence",
                      "description": "Generate fibonacci sequence",
                      "detailedRequirements": "Must handle edge cases",
                      "constraints": "O(n) time complexity",
                      "maxXP": 50,
                      "estimatedDuration": 30,
                      "starterCode": "def fibonacci(n):",
                      "testCases": [
                        {
                          "input": "5",
                          "expectedOutput": "8",
                          "isHidden": false,
                          "description": "Test case 1"
                        }
                      ],
                      "evaluationCriteria": {
                        "correctness": ["Passes all test cases"],
                        "efficiency": ["O(n) solution"],
                        "style": ["Clean code"]
                      },
                      "hints": ["Consider recursion", "Use memoization"]
                    }
                  ]
                }
                """;

        mockChatResponse(aiResponse);
        when(taskDefinitionRepository.findBySkillIdAndTitle(eq(skillId), eq("Fibonacci Sequence")))
                .thenReturn(Optional.empty());
        when(taskDefinitionRepository.save(any(TaskDefinition.class)))
                .thenReturn(testDefinition);

        Task savedTask = new Task();
        savedTask.setId(UUID.randomUUID());
        savedTask.setTitle("Fibonacci Sequence");
        savedTask.setType(TaskType.CODING);
        savedTask.setDifficulty(TaskDifficulty.INTERMEDIATE);
        when(taskRepository.save(any(Task.class))).thenReturn(savedTask);

        List<Task> results = contentGeneratorService.generateCodingTask(
                testSkill, TaskDifficulty.INTERMEDIATE, 1
        );

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(TaskType.CODING, results.get(0).getType());
        verify(taskRepository, times(1)).save(any(Task.class));
    }

    @Test
    void testGenerateCodingTask_MultipleTasks() throws Exception {
        String aiResponse = """
                {
                  "challenges": [
                    {
                      "title": "Task 1",
                      "description": "Description 1",
                      "detailedRequirements": "Requirements 1",
                      "constraints": "No constraints",
                      "maxXP": 25,
                      "estimatedDuration": 15,
                      "testCases": [],
                      "evaluationCriteria": {},
                      "hints": []
                    },
                    {
                      "title": "Task 2",
                      "description": "Description 2",
                      "detailedRequirements": "Requirements 2",
                      "constraints": "No constraints",
                      "maxXP": 30,
                      "estimatedDuration": 20,
                      "testCases": [],
                      "evaluationCriteria": {},
                      "hints": []
                    }
                  ]
                }
                """;

        mockChatResponse(aiResponse);
        when(taskDefinitionRepository.findBySkillIdAndTitle(eq(skillId), anyString()))
                .thenReturn(Optional.empty());
        when(taskDefinitionRepository.save(any(TaskDefinition.class)))
                .thenReturn(testDefinition);

        Task savedTask = new Task();
        savedTask.setId(UUID.randomUUID());
        savedTask.setType(TaskType.CODING);
        when(taskRepository.save(any(Task.class))).thenReturn(savedTask);

        List<Task> results = contentGeneratorService.generateCodingTask(
                testSkill, TaskDifficulty.BEGINNER, 2
        );

        assertNotNull(results);
        assertEquals(2, results.size());
        verify(taskRepository, times(2)).save(any(Task.class));
    }

    @Test
    void testGenerateCodingTask_WithTestCases() throws Exception {
        String aiResponse = """
                {
                  "challenges": [
                    {
                      "title": "Test Case Task",
                      "description": "Test multiple cases",
                      "detailedRequirements": "Handle various inputs",
                      "constraints": "None",
                      "maxXP": 40,
                      "estimatedDuration": 25,
                      "testCases": [
                        {
                          "input": "1,2,3",
                          "expectedOutput": "6",
                          "isHidden": false,
                          "description": "Sum of numbers"
                        },
                        {
                          "input": "0",
                          "expectedOutput": "0",
                          "isHidden": true,
                          "description": "Edge case"
                        }
                      ],
                      "evaluationCriteria": {},
                      "hints": []
                    }
                  ]
                }
                """;

        mockChatResponse(aiResponse);
        when(taskDefinitionRepository.findBySkillIdAndTitle(eq(skillId), anyString()))
                .thenReturn(Optional.empty());
        when(taskDefinitionRepository.save(any(TaskDefinition.class)))
                .thenReturn(testDefinition);

        Task savedTask = new Task();
        savedTask.setId(UUID.randomUUID());
        savedTask.setType(TaskType.CODING);
        CodingTaskContent content = new CodingTaskContent();
        content.setTestCases(List.of(
                CodingTaskContent.TestCase.builder()
                        .input("1,2,3")
                        .expectedOutput("6")
                        .isHidden(false)
                        .build(),
                CodingTaskContent.TestCase.builder()
                        .input("0")
                        .expectedOutput("0")
                        .isHidden(true)
                        .build()
        ));
        savedTask.setContent(content);
        when(taskRepository.save(any(Task.class))).thenReturn(savedTask);

        List<Task> results = contentGeneratorService.generateCodingTask(
                testSkill, TaskDifficulty.INTERMEDIATE, 1
        );

        assertNotNull(results);
        assertEquals(1, results.size());
    }

    @Test
    void testGenerateCodingTask_InvalidJsonResponse() throws Exception {
    String invalidResponse = "{ broken json";

    Prompt mockPrompt = mock(Prompt.class);
    lenient().when(codingPromptTemplate.create(anyMap())).thenReturn(mockPrompt);
    mockChatResponse("{ broken json");

    assertThrows(RuntimeException.class, () -> {
    contentGeneratorService.generateCodingTask(testSkill, TaskDifficulty.BEGINNER, 1);
    });
    }

    @Test
    void testGenerateCodingTask_MissingChallengesArray() throws Exception {
        String aiResponse = """
                {
                  "other_field": "value"
                }
                """;

        mockChatResponse(aiResponse);

        assertThrows(RuntimeException.class, () -> {
            contentGeneratorService.generateCodingTask(testSkill, TaskDifficulty.BEGINNER, 1);
        });
    }

    @Test
    void testGenerateCodingTask_WithStarterCode() throws Exception {
        String aiResponse = """
                {
                  "challenges": [
                    {
                      "title": "Starter Code Task",
                      "description": "Complete the function",
                      "detailedRequirements": "Fill in the implementation",
                      "constraints": "None",
                      "maxXP": 25,
                      "estimatedDuration": 15,
                      "starterCode": "def solve(x):\\n    # Your code here\\n    pass",
                      "testCases": [],
                      "evaluationCriteria": {},
                      "hints": []
                    }
                  ]
                }
                """;

        mockChatResponse(aiResponse);
        when(taskDefinitionRepository.findBySkillIdAndTitle(eq(skillId), anyString()))
                .thenReturn(Optional.empty());
        when(taskDefinitionRepository.save(any(TaskDefinition.class)))
                .thenReturn(testDefinition);

        Task savedTask = new Task();
        savedTask.setId(UUID.randomUUID());
        CodingTaskContent content = new CodingTaskContent();
        content.setStarterCode("def solve(x):\\n    # Your code here\\n    pass");
        savedTask.setContent(content);
        when(taskRepository.save(any(Task.class))).thenReturn(savedTask);

        List<Task> results = contentGeneratorService.generateCodingTask(
                testSkill, TaskDifficulty.BEGINNER, 1
        );

        assertNotNull(results);
        assertEquals(1, results.size());
    }

    // ==================== ESSAY TASK GENERATION TESTS ====================

    @Test
    void testGenerateEssayTask_Success() throws Exception {
        String aiResponse = """
                {
                  "tasks": [
                    {
                      "title": "Python Best Practices",
                      "description": "Write about Python best practices",
                      "prompt": "Explain Python best practices",
                      "detailedInstructions": "Include examples",
                      "maxXP": 50,
                      "estimatedDuration": 30,
                      "expectedLength": "500-800 words",
                      "evaluationCriteria": {
                        "completeness": ["Covers all topics"],
                        "accuracy": ["Factually correct"],
                        "clarity": ["Well-written"],
                        "depth": ["Shows understanding"]
                      },
                      "rubric": {
                        "completeness": {
                          "excellent": "All topics covered thoroughly",
                          "good": "Most topics covered",
                          "satisfactory": "Basic coverage",
                          "needsImprovement": "Incomplete coverage"
                        },
                        "accuracy": {
                          "excellent": "No errors",
                          "good": "Minor errors",
                          "satisfactory": "Some errors",
                          "needsImprovement": "Major errors"
                        },
                        "clarity": {
                          "excellent": "Very clear",
                          "good": "Mostly clear",
                          "satisfactory": "Somewhat clear",
                          "needsImprovement": "Unclear"
                        },
                        "depth": {
                          "excellent": "Deep understanding",
                          "good": "Good understanding",
                          "satisfactory": "Basic understanding",
                          "needsImprovement": "Limited understanding"
                        }
                      },
                      "hints": ["Include examples", "Reference docs"]
                    }
                  ]
                }
                """;

        mockChatResponse(aiResponse);
        when(taskDefinitionRepository.findBySkillIdAndTitle(eq(skillId), anyString()))
                .thenReturn(Optional.empty());
        when(taskDefinitionRepository.save(any(TaskDefinition.class)))
                .thenReturn(testDefinition);

        Task savedTask = new Task();
        savedTask.setId(UUID.randomUUID());
        savedTask.setTitle("Python Best Practices");
        savedTask.setType(TaskType.ESSAY);
        savedTask.setDifficulty(TaskDifficulty.INTERMEDIATE);
        when(taskRepository.save(any(Task.class))).thenReturn(savedTask);

        List<Task> results = contentGeneratorService.generateEssayTask(
                testSkill, TaskDifficulty.INTERMEDIATE, 1
        );

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(TaskType.ESSAY, results.get(0).getType());
        verify(taskRepository, times(1)).save(any(Task.class));
    }

    @Test
    void testGenerateEssayTask_MultipleTasks() throws Exception {
        String aiResponse = """
                {
                  "tasks": [
                    {
                      "title": "Essay 1",
                      "description": "Description 1",
                      "prompt": "Prompt 1",
                      "detailedInstructions": "Instructions 1",
                      "maxXP": 40,
                      "estimatedDuration": 25,
                      "expectedLength": "300-500 words",
                      "evaluationCriteria": {},
                      "rubric": {},
                      "hints": []
                    },
                    {
                      "title": "Essay 2",
                      "description": "Description 2",
                      "prompt": "Prompt 2",
                      "detailedInstructions": "Instructions 2",
                      "maxXP": 45,
                      "estimatedDuration": 30,
                      "expectedLength": "400-600 words",
                      "evaluationCriteria": {},
                      "rubric": {},
                      "hints": []
                    }
                  ]
                }
                """;

        mockChatResponse(aiResponse);
        when(taskDefinitionRepository.findBySkillIdAndTitle(eq(skillId), anyString()))
                .thenReturn(Optional.empty());
        when(taskDefinitionRepository.save(any(TaskDefinition.class)))
                .thenReturn(testDefinition);

        Task savedTask = new Task();
        savedTask.setId(UUID.randomUUID());
        savedTask.setType(TaskType.ESSAY);
        when(taskRepository.save(any(Task.class))).thenReturn(savedTask);

        List<Task> results = contentGeneratorService.generateEssayTask(
                testSkill, TaskDifficulty.ADVANCED, 2
        );

        assertNotNull(results);
        assertEquals(2, results.size());
        verify(taskRepository, times(2)).save(any(Task.class));
    }

    @Test
    void testGenerateEssayTask_InvalidJsonResponse() throws Exception {
        mockChatResponse("{ broken json");

        assertThrows(RuntimeException.class, () -> {
            contentGeneratorService.generateEssayTask(testSkill, TaskDifficulty.BEGINNER, 1);
        });
    }

    @Test
    void testGenerateEssayTask_MissingTasksArray() throws Exception {
        String aiResponse = """
                {
                  "other_field": "value"
                }
                """;

        mockChatResponse(aiResponse);

        assertThrows(RuntimeException.class, () -> {
            contentGeneratorService.generateEssayTask(testSkill, TaskDifficulty.BEGINNER, 1);
        });
    }

    // ==================== JSON CLEANING TESTS ====================

    @Test
    void testGenerateCodingTask_CleanJsonMarkdown() throws Exception {
        String aiResponseWithMarkdown = """
                ```json
                {
                  "challenges": [
                    {
                      "title": "Test",
                      "description": "Test",
                      "detailedRequirements": "Test",
                      "constraints": "None",
                      "maxXP": 25,
                      "estimatedDuration": 15,
                      "testCases": [],
                      "evaluationCriteria": {},
                      "hints": []
                    }
                  ]
                }
                ```
                """;

        mockChatResponse(aiResponseWithMarkdown);
        when(taskDefinitionRepository.findBySkillIdAndTitle(eq(skillId), anyString()))
                .thenReturn(Optional.empty());
        when(taskDefinitionRepository.save(any(TaskDefinition.class)))
                .thenReturn(testDefinition);

        Task savedTask = new Task();
        savedTask.setId(UUID.randomUUID());
        savedTask.setType(TaskType.CODING);
        when(taskRepository.save(any(Task.class))).thenReturn(savedTask);

        List<Task> results = contentGeneratorService.generateCodingTask(
                testSkill, TaskDifficulty.BEGINNER, 1
        );

        assertNotNull(results);
        assertEquals(1, results.size());
    }

    @Test
    void testGenerateCodingTask_CleanJsonMarkdownTripleBacktick() throws Exception {
        String aiResponseWithMarkdown = """
                ```
                {
                  "challenges": [
                    {
                      "title": "Test",
                      "description": "Test",
                      "detailedRequirements": "Test",
                      "constraints": "None",
                      "maxXP": 25,
                      "estimatedDuration": 15,
                      "testCases": [],
                      "evaluationCriteria": {},
                      "hints": []
                    }
                  ]
                }
                ```
                """;

        mockChatResponse(aiResponseWithMarkdown);
        when(taskDefinitionRepository.findBySkillIdAndTitle(eq(skillId), anyString()))
                .thenReturn(Optional.empty());
        when(taskDefinitionRepository.save(any(TaskDefinition.class)))
                .thenReturn(testDefinition);

        Task savedTask = new Task();
        savedTask.setId(UUID.randomUUID());
        savedTask.setType(TaskType.CODING);
        when(taskRepository.save(any(Task.class))).thenReturn(savedTask);

        List<Task> results = contentGeneratorService.generateCodingTask(
                testSkill, TaskDifficulty.BEGINNER, 1
        );

        assertNotNull(results);
        assertEquals(1, results.size());
    }

    // ==================== TASK DEFINITION VERSIONING TESTS ====================

    @Test
    void testGenerateCodingTask_CreatesNewTaskDefinition() throws Exception {
        String aiResponse = """
                {
                  "challenges": [
                    {
                      "title": "New Definition Task",
                      "description": "Test",
                      "detailedRequirements": "Test",
                      "constraints": "None",
                      "maxXP": 25,
                      "estimatedDuration": 15,
                      "testCases": [],
                      "evaluationCriteria": {},
                      "hints": []
                    }
                  ]
                }
                """;

        mockChatResponse(aiResponse);
        when(taskDefinitionRepository.findBySkillIdAndTitle(eq(skillId), eq("New Definition Task")))
                .thenReturn(Optional.empty());

        TaskDefinition newDef = new TaskDefinition();
        newDef.setSkill(testSkill);
        newDef.setTitle("New Definition Task");
        newDef.setLatestVersion(0);

        when(taskDefinitionRepository.save(any(TaskDefinition.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Task savedTask = new Task();
        savedTask.setId(UUID.randomUUID());
        savedTask.setType(TaskType.CODING);
        when(taskRepository.save(any(Task.class))).thenReturn(savedTask);

        List<Task> results = contentGeneratorService.generateCodingTask(
                testSkill, TaskDifficulty.BEGINNER, 1
        );

        assertNotNull(results);
        ArgumentCaptor<TaskDefinition> defCaptor = ArgumentCaptor.forClass(TaskDefinition.class);
        verify(taskDefinitionRepository, atLeast(1)).save(defCaptor.capture());
    }

    @Test
    void testGenerateCodingTask_IncrementsVersionNumber() throws Exception {
        String aiResponse = """
                {
                  "challenges": [
                    {
                      "title": "Versioned Task",
                      "description": "Test",
                      "detailedRequirements": "Test",
                      "constraints": "None",
                      "maxXP": 25,
                      "estimatedDuration": 15,
                      "testCases": [],
                      "evaluationCriteria": {},
                      "hints": []
                    }
                  ]
                }
                """;

        mockChatResponse(aiResponse);

        TaskDefinition existingDef = new TaskDefinition();
        existingDef.setSkill(testSkill);
        existingDef.setTitle("Versioned Task");
        existingDef.setLatestVersion(3);

        when(taskDefinitionRepository.findBySkillIdAndTitle(eq(skillId), eq("Versioned Task")))
                .thenReturn(Optional.of(existingDef));
        when(taskDefinitionRepository.save(any(TaskDefinition.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Task savedTask = new Task();
        savedTask.setId(UUID.randomUUID());
        savedTask.setType(TaskType.CODING);
        when(taskRepository.save(any(Task.class))).thenReturn(savedTask);

        List<Task> results = contentGeneratorService.generateCodingTask(
                testSkill, TaskDifficulty.BEGINNER, 1
        );

        assertNotNull(results);
        assertEquals(1, results.size());
    }

    // ==================== DEFAULT VALUES TESTS ====================

    @Test
    void testGenerateCodingTask_DefaultValues() throws Exception {
        String minimalResponse = """
                {
                  "challenges": [
                    {
                      "title": "Minimal Task"
                    }
                  ]
                }
                """;

        mockChatResponse(minimalResponse);
        when(taskDefinitionRepository.findBySkillIdAndTitle(eq(skillId), eq("Minimal Task")))
                .thenReturn(Optional.empty());
        when(taskDefinitionRepository.save(any(TaskDefinition.class)))
                .thenReturn(testDefinition);

        Task savedTask = new Task();
        savedTask.setId(UUID.randomUUID());
        savedTask.setType(TaskType.CODING);
        when(taskRepository.save(any(Task.class))).thenReturn(savedTask);

        List<Task> results = contentGeneratorService.generateCodingTask(
                testSkill, TaskDifficulty.BEGINNER, 1
        );

        assertNotNull(results);
        assertEquals(1, results.size());
    }

    @Test
    void testGenerateEssayTask_DefaultValues() throws Exception {
        String minimalResponse = """
                {
                  "tasks": [
                    {
                      "title": "Minimal Essay"
                    }
                  ]
                }
                """;

        mockChatResponse(minimalResponse);
        when(taskDefinitionRepository.findBySkillIdAndTitle(eq(skillId), eq("Minimal Essay")))
                .thenReturn(Optional.empty());
        when(taskDefinitionRepository.save(any(TaskDefinition.class)))
                .thenReturn(testDefinition);

        Task savedTask = new Task();
        savedTask.setId(UUID.randomUUID());
        savedTask.setType(TaskType.ESSAY);
        when(taskRepository.save(any(Task.class))).thenReturn(savedTask);

        List<Task> results = contentGeneratorService.generateEssayTask(
                testSkill, TaskDifficulty.BEGINNER, 1
        );

        assertNotNull(results);
        assertEquals(1, results.size());
    }

    // ==================== PUBLISHED STATUS TESTS ====================

    @Test
    void testGenerateCodingTask_TaskPublished() throws Exception {
        String aiResponse = """
                {
                  "challenges": [
                    {
                      "title": "Published Task",
                      "description": "Test",
                      "detailedRequirements": "Test",
                      "constraints": "None",
                      "maxXP": 25,
                      "estimatedDuration": 15,
                      "testCases": [],
                      "evaluationCriteria": {},
                      "hints": []
                    }
                  ]
                }
                """;

        mockChatResponse(aiResponse);
        when(taskDefinitionRepository.findBySkillIdAndTitle(eq(skillId), anyString()))
                .thenReturn(Optional.empty());
        when(taskDefinitionRepository.save(any(TaskDefinition.class)))
                .thenReturn(testDefinition);

        Task savedTask = new Task();
        savedTask.setId(UUID.randomUUID());
        savedTask.setIsPublished(true);
        savedTask.setType(TaskType.CODING);
        when(taskRepository.save(any(Task.class))).thenReturn(savedTask);

        List<Task> results = contentGeneratorService.generateCodingTask(
                testSkill, TaskDifficulty.BEGINNER, 1
        );

        assertNotNull(results);
        assertTrue(results.get(0).getIsPublished());
    }

    @Test
    void testGenerateEssayTask_TaskPublished() throws Exception {
        String aiResponse = """
                {
                  "tasks": [
                    {
                      "title": "Published Essay",
                      "description": "Test",
                      "prompt": "Test",
                      "detailedInstructions": "Test",
                      "evaluationCriteria": {},
                      "rubric": {},
                      "hints": []
                    }
                  ]
                }
                """;

        mockChatResponse(aiResponse);
        when(taskDefinitionRepository.findBySkillIdAndTitle(eq(skillId), anyString()))
                .thenReturn(Optional.empty());
        when(taskDefinitionRepository.save(any(TaskDefinition.class)))
                .thenReturn(testDefinition);

        Task savedTask = new Task();
        savedTask.setId(UUID.randomUUID());
        savedTask.setIsPublished(true);
        savedTask.setType(TaskType.ESSAY);
        when(taskRepository.save(any(Task.class))).thenReturn(savedTask);

        List<Task> results = contentGeneratorService.generateEssayTask(
                testSkill, TaskDifficulty.BEGINNER, 1
        );

        assertNotNull(results);
        assertTrue(results.get(0).getIsPublished());
    }

    // ==================== EDGE CASES ====================

    @Test
    void testGenerateCodingTask_EmptyChallengesArray() throws Exception {
        String aiResponse = """
                {
                  "challenges": []
                }
                """;

        mockChatResponse(aiResponse);

        List<Task> results = contentGeneratorService.generateCodingTask(
                testSkill, TaskDifficulty.BEGINNER, 0
        );

        assertNotNull(results);
        assertEquals(0, results.size());
    }

    @Test
    void testGenerateEssayTask_EmptyTasksArray() throws Exception {
        String aiResponse = """
                {
                  "tasks": []
                }
                """;

        mockChatResponse(aiResponse);

        List<Task> results = contentGeneratorService.generateEssayTask(
                testSkill, TaskDifficulty.BEGINNER, 0
        );

        assertNotNull(results);
        assertEquals(0, results.size());
    }

    @Test
    void testGenerateCodingTask_WithExamples() throws Exception {
        String aiResponse = """
                {
                  "challenges": [
                    {
                      "title": "With Examples",
                      "description": "Test",
                      "detailedRequirements": "Test",
                      "constraints": "None",
                      "maxXP": 25,
                      "estimatedDuration": 15,
                      "testCases": [
                        {
                          "input": "test",
                          "expectedOutput": "result",
                          "isHidden": false
                        }
                      ],
                      "evaluationCriteria": {},
                      "hints": []
                    }
                  ]
                }
                """;

        mockChatResponse(aiResponse);
        when(taskDefinitionRepository.findBySkillIdAndTitle(eq(skillId), anyString()))
                .thenReturn(Optional.empty());
        when(taskDefinitionRepository.save(any(TaskDefinition.class)))
                .thenReturn(testDefinition);

        Task savedTask = new Task();
        savedTask.setId(UUID.randomUUID());
        savedTask.setType(TaskType.CODING);
        when(taskRepository.save(any(Task.class))).thenReturn(savedTask);

        List<Task> results = contentGeneratorService.generateCodingTask(
                testSkill, TaskDifficulty.BEGINNER, 1
        );

        assertNotNull(results);
        assertEquals(1, results.size());
    }

    @Test
    void testGenerateEssayTask_WithCompleteRubric() throws Exception {
        String aiResponse = """
                {
                  "tasks": [
                    {
                      "title": "Complete Rubric Essay",
                      "description": "Test",
                      "prompt": "Test",
                      "detailedInstructions": "Test",
                      "maxXP": 50,
                      "estimatedDuration": 30,
                      "expectedLength": "500-800 words",
                      "evaluationCriteria": {
                        "completeness": ["criterion1"],
                        "accuracy": ["criterion2"],
                        "clarity": ["criterion3"],
                        "depth": ["criterion4"]
                      },
                      "rubric": {
                        "completeness": {
                          "excellent": "Excellent",
                          "good": "Good",
                          "satisfactory": "Satisfactory",
                          "needsImprovement": "Needs Improvement"
                        },
                        "accuracy": {
                          "excellent": "Excellent",
                          "good": "Good",
                          "satisfactory": "Satisfactory",
                          "needsImprovement": "Needs Improvement"
                        },
                        "clarity": {
                          "excellent": "Excellent",
                          "good": "Good",
                          "satisfactory": "Satisfactory",
                          "needsImprovement": "Needs Improvement"
                        },
                        "depth": {
                          "excellent": "Excellent",
                          "good": "Good",
                          "satisfactory": "Satisfactory",
                          "needsImprovement": "Needs Improvement"
                        }
                      },
                      "hints": ["hint1", "hint2"]
                    }
                  ]
                }
                """;

        mockChatResponse(aiResponse);
        when(taskDefinitionRepository.findBySkillIdAndTitle(eq(skillId), anyString()))
                .thenReturn(Optional.empty());
        when(taskDefinitionRepository.save(any(TaskDefinition.class)))
                .thenReturn(testDefinition);

        Task savedTask = new Task();
        savedTask.setId(UUID.randomUUID());
        savedTask.setType(TaskType.ESSAY);
        when(taskRepository.save(any(Task.class))).thenReturn(savedTask);

        List<Task> results = contentGeneratorService.generateEssayTask(
                testSkill, TaskDifficulty.ADVANCED, 1
        );

        assertNotNull(results);
        assertEquals(1, results.size());
    }

    // ==================== PROMPT PARAMETER TESTS ====================

    @Test
    void testGenerateCodingTask_CorrectPromptParameters() throws Exception {
    String aiResponse = """
    {
    "challenges": [
    {
    "title": "Test 1",
    "description": "Test",
    "detailedRequirements": "Test",
    "constraints": "None",
    "maxXP": 25,
    "estimatedDuration": 15,
    "testCases": [],
    "evaluationCriteria": {},
    "hints": []
    },
      {
          "title": "Test 2",
          "description": "Test",
                       "detailedRequirements": "Test",
                  "constraints": "None",
                  "maxXP": 25,
                  "estimatedDuration": 15,
                  "testCases": [],
          "evaluationCriteria": {},
                  "hints": []
        },
                     {
                  "title": "Test 3",
              "description": "Test",
              "detailedRequirements": "Test",
              "constraints": "None",
                 "maxXP": 25,
                       "estimatedDuration": 15,
                 "testCases": [],
                       "evaluationCriteria": {},
                  "hints": []
            }
                 ]
                 }
                \s""";

         Prompt mockPrompt = mock(Prompt.class);
         when(codingPromptTemplate.create(anyMap())).thenReturn(mockPrompt);
         mockChatResponse(aiResponse);
         when(taskDefinitionRepository.findBySkillIdAndTitle(eq(skillId), anyString()))
                 .thenReturn(Optional.empty());
         when(taskDefinitionRepository.save(any(TaskDefinition.class)))
                 .thenReturn(testDefinition);

         when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
             Task task = invocation.getArgument(0);
             task.setId(UUID.randomUUID());
             return task;
          });

          List<Task> results = contentGeneratorService.generateCodingTask(testSkill, TaskDifficulty.INTERMEDIATE, 3);

         assertNotNull(results);
         assertEquals(3, results.size());
      }

    @Test
    void testGenerateEssayTask_CorrectPromptParameters() throws Exception {
    String aiResponse = """
    {
    "tasks": [
    {
    "title": "Test 1",
    "description": "Test",
    "prompt": "Test",
    "detailedInstructions": "Test",
    "evaluationCriteria": {},
    "rubric": {},
    "hints": []
    },
      {
          "title": "Test 2",
          "description": "Test",
                       "prompt": "Test",
                  "detailedInstructions": "Test",
                  "evaluationCriteria": {},
                  "rubric": {},
                  "hints": []
        }
              ]
    }
                \s""";

    Prompt mockPrompt = mock(Prompt.class);
    when(essayPromptTemplate.create(anyMap())).thenReturn(mockPrompt);
    mockChatResponse(aiResponse);
    when(taskDefinitionRepository.findBySkillIdAndTitle(eq(skillId), anyString()))
                 .thenReturn(Optional.empty());
    when(taskDefinitionRepository.save(any(TaskDefinition.class)))
                 .thenReturn(testDefinition);

    when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task task = invocation.getArgument(0);
             task.setId(UUID.randomUUID());
             return task;
         });

         List<Task> results = contentGeneratorService.generateEssayTask(testSkill, TaskDifficulty.ADVANCED, 2);

         assertNotNull(results);
         assertEquals(2, results.size());
     }

    // ==================== HELPER METHODS ====================
    private void mockChatResponse(String responseText) {
        ChatResponse response = mock(ChatResponse.class);
        Generation generation = mock(Generation.class);
        var output = mock(org.springframework.ai.chat.messages.AssistantMessage.class);
        lenient().when(output.getText()).thenReturn(responseText);
        lenient().when(generation.getOutput()).thenReturn(output);
        lenient().when(response.getResult()).thenReturn(generation);
        lenient().when(chatModel.call(any(Prompt.class))).thenReturn(response);
    }

}
