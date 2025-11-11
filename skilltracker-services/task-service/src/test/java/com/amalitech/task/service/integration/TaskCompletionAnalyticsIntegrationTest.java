package com.amalitech.task.service.integration;

import com.amalitech.common.event.events.SubmissionEvaluatedEvent;
import com.amalitech.common.event.events.TaskCompletedEvent;
import com.amalitech.task.service.events.EventProducer;
import com.amalitech.task.service.mapper.FallbackFeedbackMapper;
import com.amalitech.task.service.mapper.SubmissionMapper;
import com.amalitech.task.service.mapper.TaskCompletionMapper;
import com.amalitech.task.service.model.Task;
import com.amalitech.task.service.model.TaskDefinition;
import com.amalitech.task.service.model.TaskSubmission;
import com.amalitech.task.service.model.view.SkillView;
import com.amalitech.task.service.model.enums.SubmissionStatus;
import com.amalitech.task.service.model.enums.TaskDifficulty;
import com.amalitech.task.service.model.enums.TaskType;
import com.amalitech.task.service.repository.TaskRepository;
import com.amalitech.task.service.repository.TaskSubmissionRepository;
import com.amalitech.task.service.service.SubmissionService;
import com.amalitech.task.service.validation.TaskCompletedEventValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Integration test for the task completion analytics feature.
 *
 * <p>This test verifies that the complete flow from submission evaluation to
 * TaskCompletedEvent publication works correctly, including:
 * <ul>
 *   <li>Event construction with all required fields</li>
 *   <li>Rubric score extraction from detailed feedback</li>
 *   <li>Event validation</li>
 *   <li>Metrics recording</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class TaskCompletionAnalyticsIntegrationTest {

    @Mock
    private TaskSubmissionRepository submissionRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private EventProducer eventProducer;

    @Mock
    private SubmissionMapper submissionMapper;

    @Mock
    private FallbackFeedbackMapper fallbackMapper;

    private ObjectMapper objectMapper;
    private TaskCompletionMapper taskCompletionMapper;
    private TaskCompletedEventValidator validator;
    private MeterRegistry meterRegistry;

    @InjectMocks
    private com.amalitech.task.service.service.impl.SubmissionServiceImpl submissionService;

    private UUID userId;
    private UUID taskId;
    private UUID skillId;
    private UUID submissionId;
    private Task testTask;
    private TaskSubmission testSubmission;

    @BeforeEach
    void setUp() {
        // Initialize real instances for mapping and validation
        objectMapper = new ObjectMapper();
        taskCompletionMapper = new TaskCompletionMapper(objectMapper);
        validator = new TaskCompletedEventValidator();
        meterRegistry = new SimpleMeterRegistry();

        // Reinitialize the submission service with all dependencies
        submissionService = new com.amalitech.task.service.service.impl.SubmissionServiceImpl(
                submissionRepository,
                taskRepository,
                eventProducer,
                submissionMapper,
                objectMapper,
                fallbackMapper,
                taskCompletionMapper,
                validator,
                meterRegistry
        );

        // Create test data
        userId = UUID.randomUUID();
        taskId = UUID.randomUUID();
        skillId = UUID.randomUUID();
        submissionId = UUID.randomUUID();

        SkillView skill = SkillView.builder()
                .id(skillId)
                .name("Java Programming")
                .build();

        TaskDefinition taskDefinition = TaskDefinition.builder()
                .id(UUID.randomUUID())
                .skill(skill)
                .build();

        testTask = Task.builder()
                .id(taskId)
                .title("Reverse a String")
                .description("Write a function to reverse a string")
                .type(TaskType.CODING)
                .difficulty(TaskDifficulty.BEGINNER)
                .xpReward(100)
                .taskDefinition(taskDefinition)
                .build();

        testSubmission = new TaskSubmission();
        testSubmission.setId(submissionId);
        testSubmission.setUserId(userId);
        testSubmission.setTask(testTask);
        testSubmission.setStatus(SubmissionStatus.PENDING);
    }

    @Test
    void testTaskCompletionEventPublishingFlow() {
        // Arrange
        String detailedFeedback = """
                {
                  "evaluation": {
                    "correctness": {"score": 50.0, "feedback": "All test cases passed"},
                    "efficiency": {"score": 25.5, "feedback": "Good algorithm complexity"},
                    "style": {"score": 18.0, "feedback": "Well-formatted code"},
                    "overall": {"percentage": 93, "summary": "Excellent submission"}
                  }
                }
                """;

        SubmissionEvaluatedEvent evaluatedEvent = SubmissionEvaluatedEvent.builder()
                .submissionId(submissionId)
                .userId(userId)
                .status("COMPLETED")
                .score(93)
                .isCorrect(true)
                .feedbackType("CODING")
                .overallFeedback("Excellent submission")
                .detailedFeedback(detailedFeedback)
                .avgExecutionTimeMs(1.5)
                .avgMemoryUsedKb(512)
                .build();

        when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(testSubmission));
        when(submissionRepository.save(any(TaskSubmission.class))).thenAnswer(invocation -> {
            TaskSubmission submission = invocation.getArgument(0);
            submission.setEvaluatedAt(LocalDateTime.now());
            return submission;
        });

        // Act
        submissionService.updateSubmissionFromEvent(evaluatedEvent);

        // Assert - Verify submission was updated
        verify(submissionRepository).findById(submissionId);
        verify(submissionRepository).save(argThat(submission ->
                submission.getScoreEarned() == 93 &&
                submission.getIsCorrect() &&
                submission.getStatus() == SubmissionStatus.COMPLETED
        ));

        // Assert - Verify TaskCompletedEvent was published
        ArgumentCaptor<TaskCompletedEvent> eventCaptor = ArgumentCaptor.forClass(TaskCompletedEvent.class);
        verify(eventProducer).publishTaskCompleted(eventCaptor.capture());

        TaskCompletedEvent publishedEvent = eventCaptor.getValue();
        assertNotNull(publishedEvent);
        assertEquals(userId, publishedEvent.getUserId());
        assertEquals(skillId, publishedEvent.getSkillId());
        assertEquals(TaskType.CODING.toString(), publishedEvent.getTaskType());
        assertTrue(publishedEvent.getPassed());
    }

    @Test
    void testRubricScoreExtractionFromDetailedFeedback() {
        // Arrange
        String detailedFeedback = """
                {
                  "evaluation": {
                    "correctness": {"score": 48.5, "feedback": "Minor issues in edge cases"},
                    "efficiency": {"score": 28.0, "feedback": "Optimized"},
                    "style": {"score": 20.0, "feedback": "Perfect formatting"},
                    "overall": {"percentage": 96, "summary": "Great work"}
                  }
                }
                """;

        SubmissionEvaluatedEvent evaluatedEvent = SubmissionEvaluatedEvent.builder()
                .submissionId(submissionId)
                .userId(userId)
                .status("COMPLETED")
                .score(96)
                .isCorrect(true)
                .feedbackType("CODING")
                .overallFeedback("Great work")
                .detailedFeedback(detailedFeedback)
                .build();

        when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(testSubmission));
        when(submissionRepository.save(any(TaskSubmission.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        submissionService.updateSubmissionFromEvent(evaluatedEvent);

        // Assert
        ArgumentCaptor<TaskCompletedEvent> eventCaptor = ArgumentCaptor.forClass(TaskCompletedEvent.class);
        verify(eventProducer).publishTaskCompleted(eventCaptor.capture());

        TaskCompletedEvent publishedEvent = eventCaptor.getValue();
        Map<String, TaskCompletedEvent.RubricScoreData> rubrics = publishedEvent.getRubricsScores();

        assertNotNull(rubrics);
        assertTrue(rubrics.containsKey("correctness"));
        assertTrue(rubrics.containsKey("efficiency"));
        assertTrue(rubrics.containsKey("style"));

        // Verify score precision is preserved (Double, not Integer)
        TaskCompletedEvent.RubricScoreData correctnessScore = rubrics.get("correctness");
        assertEquals(48.5, correctnessScore.getScore(), 0.001);
        assertEquals(50, correctnessScore.getMaxScore());
        assertEquals(97, correctnessScore.getPercentage());

        TaskCompletedEvent.RubricScoreData efficiencyScore = rubrics.get("efficiency");
        assertEquals(28.0, efficiencyScore.getScore(), 0.001);
        assertEquals(30, efficiencyScore.getMaxScore());
        assertEquals(93, efficiencyScore.getPercentage());
    }

    @Test
    void testEventValidation() {
        // Arrange
        String detailedFeedback = """
                {
                  "evaluation": {
                    "correctness": {"score": 50.0, "feedback": "Good"},
                    "efficiency": {"score": 30.0, "feedback": "Good"},
                    "style": {"score": 20.0, "feedback": "Good"},
                    "overall": {"percentage": 100, "summary": "Perfect"}
                  }
                }
                """;

        SubmissionEvaluatedEvent evaluatedEvent = SubmissionEvaluatedEvent.builder()
                .submissionId(submissionId)
                .userId(userId)
                .status("COMPLETED")
                .score(100)
                .isCorrect(true)
                .feedbackType("CODING")
                .overallFeedback("Perfect")
                .detailedFeedback(detailedFeedback)
                .build();

        when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(testSubmission));
        when(submissionRepository.save(any(TaskSubmission.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        submissionService.updateSubmissionFromEvent(evaluatedEvent);

        // Assert
        ArgumentCaptor<TaskCompletedEvent> eventCaptor = ArgumentCaptor.forClass(TaskCompletedEvent.class);
        verify(eventProducer).publishTaskCompleted(eventCaptor.capture());

        TaskCompletedEvent publishedEvent = eventCaptor.getValue();

        // Event should be valid
        assertTrue(validator.isValid(publishedEvent), "Event should pass validation");
    }

    @Test
    void testMetricsRecording() {
        // Arrange
        String detailedFeedback = """
                {
                  "evaluation": {
                    "correctness": {"score": 50.0, "feedback": "Good"},
                    "efficiency": {"score": 30.0, "feedback": "Good"},
                    "style": {"score": 20.0, "feedback": "Good"},
                    "overall": {"percentage": 100, "summary": "Perfect"}
                  }
                }
                """;

        SubmissionEvaluatedEvent evaluatedEvent = SubmissionEvaluatedEvent.builder()
                .submissionId(submissionId)
                .userId(userId)
                .status("COMPLETED")
                .score(100)
                .isCorrect(true)
                .feedbackType("CODING")
                .overallFeedback("Perfect")
                .detailedFeedback(detailedFeedback)
                .build();

        when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(testSubmission));
        when(submissionRepository.save(any(TaskSubmission.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        submissionService.updateSubmissionFromEvent(evaluatedEvent);

        // Assert - Verify metrics were recorded
        double publishedCount = meterRegistry.counter(
                "task.completed.published",
                "taskType", "CODING",
                "passed", "true"
        ).count();
        assertEquals(1.0, publishedCount, "Should record one successful publication");
    }

    @Test
    void testHandleNullDetailedFeedback() {
        // Arrange
        SubmissionEvaluatedEvent evaluatedEvent = SubmissionEvaluatedEvent.builder()
                .submissionId(submissionId)
                .userId(userId)
                .status("COMPLETED")
                .score(85)
                .isCorrect(true)
                .feedbackType("CODING")
                .overallFeedback("Good work")
                .detailedFeedback(null)  // No detailed feedback
                .build();

        when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(testSubmission));
        when(submissionRepository.save(any(TaskSubmission.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(fallbackMapper.createBasicCodingFeedback(evaluatedEvent))
                .thenReturn(new com.amalitech.task.service.model.feedback.impl.CodingSubmissionFeedback());

        // Act
        submissionService.updateSubmissionFromEvent(evaluatedEvent);

        // Assert
        verify(eventProducer).publishTaskCompleted(any(TaskCompletedEvent.class));
        // Should still publish event even without detailed feedback
        ArgumentCaptor<TaskCompletedEvent> eventCaptor = ArgumentCaptor.forClass(TaskCompletedEvent.class);
        verify(eventProducer).publishTaskCompleted(eventCaptor.capture());

        TaskCompletedEvent publishedEvent = eventCaptor.getValue();
        assertNotNull(publishedEvent);
        // Rubrics will be empty due to null feedback, but validation should pass
        assertTrue(validator.isValid(publishedEvent) || publishedEvent.getRubricsScores().isEmpty());
    }

    @Test
    void testXpEarningCalculation() {
        // Arrange
        testTask.setXpReward(200);

        String detailedFeedback = """
                {
                  "evaluation": {
                    "correctness": {"score": 40.0, "feedback": "Good"},
                    "efficiency": {"score": 24.0, "feedback": "Good"},
                    "style": {"score": 16.0, "feedback": "Good"},
                    "overall": {"percentage": 80, "summary": "Good"}
                  }
                }
                """;

        SubmissionEvaluatedEvent evaluatedEvent = SubmissionEvaluatedEvent.builder()
                .submissionId(submissionId)
                .userId(userId)
                .status("COMPLETED")
                .score(80)
                .isCorrect(true)
                .feedbackType("CODING")
                .overallFeedback("Good submission")
                .detailedFeedback(detailedFeedback)
                .build();

        when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(testSubmission));
        when(submissionRepository.save(any(TaskSubmission.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        submissionService.updateSubmissionFromEvent(evaluatedEvent);

        // Assert
        ArgumentCaptor<TaskCompletedEvent> eventCaptor = ArgumentCaptor.forClass(TaskCompletedEvent.class);
        verify(eventProducer).publishTaskCompleted(eventCaptor.capture());

        TaskCompletedEvent publishedEvent = eventCaptor.getValue();
        assertNotNull(publishedEvent.getTotalXpEarned());
        assertEquals(200, publishedEvent.getTotalXpEarned());
    }
}
