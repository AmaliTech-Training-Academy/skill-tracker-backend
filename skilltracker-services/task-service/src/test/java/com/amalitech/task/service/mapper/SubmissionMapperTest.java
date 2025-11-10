package com.amalitech.task.service.mapper;

import com.amalitech.common.event.events.SubmissionCreatedEvent;
import com.amalitech.task.service.dto.TaskSubmissionDTO;
import com.amalitech.task.service.model.Task;
import com.amalitech.task.service.model.TaskDefinition;
import com.amalitech.task.service.model.TaskSubmission;
import com.amalitech.task.service.model.content.impl.CodingTaskContent;
import com.amalitech.task.service.model.content.impl.EssayTaskContent;
import com.amalitech.task.service.model.enums.SubmissionStatus;
import com.amalitech.task.service.model.enums.TaskDifficulty;
import com.amalitech.task.service.model.enums.TaskType;
import com.amalitech.task.service.model.submission.impl.CodingSubmissionAnswer;
import com.amalitech.task.service.model.submission.impl.EssaySubmissionAnswer;
import com.amalitech.task.service.model.view.SkillView;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SubmissionMapperTest {

    private SubmissionMapper submissionMapper;
    private ObjectMapper objectMapper;

    private UUID submissionId;
    private UUID userId;
    private UUID taskId;
    private UUID skillId;
    private Task testTask;
    private TaskSubmission testSubmission;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        submissionMapper = new SubmissionMapper(objectMapper);

        submissionId = UUID.randomUUID();
        userId = UUID.randomUUID();
        taskId = UUID.randomUUID();
        skillId = UUID.randomUUID();

        testTask = Task.builder()
                .id(taskId)
                .title("Test Task")
                .description("Test Description")
                .type(TaskType.CODING)
                .difficulty(TaskDifficulty.INTERMEDIATE)
                .build();

        testSubmission = new TaskSubmission();
        testSubmission.setId(submissionId);
        testSubmission.setUserId(userId);
        testSubmission.setTask(testTask);
        testSubmission.setStatus(SubmissionStatus.COMPLETED);
        testSubmission.setScoreEarned(85);
        testSubmission.setIsCorrect(true);
        testSubmission.setSubmittedAt(LocalDateTime.now());
        testSubmission.setEvaluatedAt(LocalDateTime.now());
    }

    @Test
    void testToDTO_Success() {
        TaskSubmissionDTO result = submissionMapper.toDTO(testSubmission);

        assertNotNull(result);
        assertEquals(submissionId, result.getId());
        assertEquals(userId, result.getUserId());
        assertEquals(SubmissionStatus.COMPLETED, result.getStatus());
        assertEquals(85, result.getScoreEarned());
        assertTrue(result.getIsCorrect());
        assertEquals(taskId, result.getTaskId());
    }

    @Test
    void testToDTO_WithoutTask() {
        testSubmission.setTask(null);

        TaskSubmissionDTO result = submissionMapper.toDTO(testSubmission);

        assertNotNull(result);
        assertEquals(submissionId, result.getId());
        assertNull(result.getTaskId());
    }

    @Test
    void testToDTO_Null() {
        TaskSubmissionDTO result = submissionMapper.toDTO(null);

        assertNull(result);
    }

    @Test
    void testToDTO_WithFeedback() {
        var feedback = new com.amalitech.task.service.model.feedback.impl.CodingSubmissionFeedback();
        testSubmission.setFeedback(feedback);

        TaskSubmissionDTO result = submissionMapper.toDTO(testSubmission);

        assertNotNull(result);
        assertNotNull(result.getFeedback());
        assertSame(feedback, result.getFeedback());
    }

    @Test
    void testToCreatedEvent_CodingSubmission_Success() {
        CodingSubmissionAnswer answer = new CodingSubmissionAnswer("int add(int a, int b) { return a + b; }", 1);
        testSubmission.setAnswer(answer);

        CodingTaskContent content = new CodingTaskContent();
        content.setExamples(List.of(
                new CodingTaskContent.Example("1 2", "3"),
                new CodingTaskContent.Example("5 3", "8")
        ));
        testTask.setContent(content);

        SkillView skill = new SkillView();
        skill.setId(skillId);
        skill.setName("Java");

        TaskDefinition definition = new TaskDefinition();
        definition.setSkill(skill);
        testTask.setTaskDefinition(definition);

        SubmissionCreatedEvent result = submissionMapper.toCreatedEvent(testSubmission);

        assertNotNull(result);
        assertEquals(submissionId, result.getSubmissionId());
        assertEquals(userId, result.getUserId());
        assertEquals(taskId, result.getTaskId());
        assertEquals("CODING", result.getTaskType());
        assertEquals("int add(int a, int b) { return a + b; }", result.getContentToEvaluate());
        assertEquals(1, result.getLanguageId());
        assertEquals(2, result.getTestCases().size());
        assertEquals("Java", result.getSkillName());
        assertEquals("INTERMEDIATE", result.getDifficulty());
        assertEquals("Test Task", result.getTaskTitle());
        assertEquals("Test Description", result.getTaskDescription());
    }

    @Test
    void testToCreatedEvent_EssaySubmission_Success() {
        testTask.setType(TaskType.ESSAY);
        EssaySubmissionAnswer answer = new EssaySubmissionAnswer("This is my essay response");
        testSubmission.setAnswer(answer);

        EssayTaskContent.EvaluationCriteria criteria = EssayTaskContent.EvaluationCriteria.builder()
                .completeness(List.of("Complete", "Thorough", "Comprehensive"))
                .accuracy(List.of("Accurate", "Correct", "Precise"))
                .clarity(List.of("Clear", "Understandable", "Explicit"))
                .depth(List.of("Deep", "Detailed", "In-depth"))
                .build();

        EssayTaskContent.Rubric.PerformanceLevels levels = EssayTaskContent.Rubric.PerformanceLevels.builder()
                .excellent("Excellent")
                .good("Good")
                .satisfactory("Satisfactory")
                .needsImprovement("Needs Improvement")
                .build();

        EssayTaskContent.Rubric rubric = EssayTaskContent.Rubric.builder()
                .completeness(levels)
                .accuracy(levels)
                .clarity(levels)
                .depth(levels)
                .build();

        EssayTaskContent content = new EssayTaskContent();
        content.setDetailedInstructions("Write a comprehensive essay");
        content.setEvaluationCriteria(criteria);
        content.setRubric(rubric);
        testTask.setContent(content);

        SkillView skill = new SkillView();
        skill.setName("Writing");
        TaskDefinition definition = new TaskDefinition();
        definition.setSkill(skill);
        testTask.setTaskDefinition(definition);

        SubmissionCreatedEvent result = submissionMapper.toCreatedEvent(testSubmission);

        assertNotNull(result);
        assertEquals(submissionId, result.getSubmissionId());
        assertEquals("ESSAY", result.getTaskType());
        assertEquals("This is my essay response", result.getContentToEvaluate());
        assertEquals("Write a comprehensive essay", result.getDetailedInstructions());
        assertNotNull(result.getEvaluationCriteria());
        assertNotNull(result.getRubric());
    }

    @Test
    void testToCreatedEvent_EssaySubmission_SerializationError() {
        testTask.setType(TaskType.ESSAY);
        EssaySubmissionAnswer answer = new EssaySubmissionAnswer("Essay text");
        testSubmission.setAnswer(answer);

        EssayTaskContent.EvaluationCriteria criteria = EssayTaskContent.EvaluationCriteria.builder()
                .completeness(List.of("C1", "C2", "C3"))
                .accuracy(List.of("A1", "A2", "A3"))
                .clarity(List.of("Cl1", "Cl2", "Cl3"))
                .depth(List.of("D1", "D2", "D3"))
                .build();

        EssayTaskContent content = new EssayTaskContent();
        content.setDetailedInstructions("Instructions");
        content.setEvaluationCriteria(criteria);
        testTask.setContent(content);

        SkillView skill = new SkillView();
        skill.setName("Writing");
        TaskDefinition definition = new TaskDefinition();
        definition.setSkill(skill);
        testTask.setTaskDefinition(definition);

        // Should not throw exception, just log a warning
        SubmissionCreatedEvent result = submissionMapper.toCreatedEvent(testSubmission);

        assertNotNull(result);
        assertEquals("ESSAY", result.getTaskType());
    }

    @Test
    void testToCreatedEvent_WithoutSkill() {
        CodingSubmissionAnswer answer = new CodingSubmissionAnswer("code", 1);
        testSubmission.setAnswer(answer);

        CodingTaskContent content = new CodingTaskContent();
        content.setExamples(List.of());
        testTask.setContent(content);

        testTask.setTaskDefinition(null);

        SubmissionCreatedEvent result = submissionMapper.toCreatedEvent(testSubmission);

        assertNotNull(result);
        assertNull(result.getSkillName());
    }

    @Test
    void testToCreatedEvent_WithoutTaskDefinition() {
        CodingSubmissionAnswer answer = new CodingSubmissionAnswer("code", 1);
        testSubmission.setAnswer(answer);

        CodingTaskContent content = new CodingTaskContent();
        content.setExamples(List.of());
        testTask.setContent(content);

        SubmissionCreatedEvent result = submissionMapper.toCreatedEvent(testSubmission);

        assertNotNull(result);
        assertNull(result.getSkillName());
    }

    @Test
    void testToCreatedEvent_CodingSubmission_NoTestCases() {
        CodingSubmissionAnswer answer = new CodingSubmissionAnswer("code", 1);
        testSubmission.setAnswer(answer);

        CodingTaskContent content = new CodingTaskContent();
        content.setExamples(List.of());
        testTask.setContent(content);

        SubmissionCreatedEvent result = submissionMapper.toCreatedEvent(testSubmission);

        assertNotNull(result);
        assertTrue(result.getTestCases().isEmpty());
    }

    @Test
    void testToCreatedEvent_CodingSubmission_MultipleTestCases() {
        CodingSubmissionAnswer answer = new CodingSubmissionAnswer("code", 1);
        testSubmission.setAnswer(answer);

        CodingTaskContent content = new CodingTaskContent();
        content.setExamples(List.of(
                new CodingTaskContent.Example("input1", "output1"),
                new CodingTaskContent.Example("input2", "output2"),
                new CodingTaskContent.Example("input3", "output3")
        ));
        testTask.setContent(content);

        SubmissionCreatedEvent result = submissionMapper.toCreatedEvent(testSubmission);

        assertNotNull(result);
        assertEquals(3, result.getTestCases().size());
        assertEquals("input1", result.getTestCases().get(0).getInput());
        assertEquals("output1", result.getTestCases().get(0).getExpectedOutput());
    }
}
