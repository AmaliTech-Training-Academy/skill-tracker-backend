package com.amalitech.task.service.service.impl;

import com.amalitech.common.event.events.SubmissionCreatedEvent;
import com.amalitech.common.event.events.SubmissionEvaluatedEvent;
import com.amalitech.task.service.dto.TaskSubmissionDTO;
import com.amalitech.task.service.dto.request.SubmitAnswerRequest;
import com.amalitech.task.service.exception.ResourceNotFoundException;
import com.amalitech.task.service.mapper.FallbackFeedbackMapper;
import com.amalitech.task.service.mapper.SubmissionMapper;
import com.amalitech.task.service.model.Task;
import com.amalitech.task.service.model.TaskSubmission;
import com.amalitech.task.service.model.enums.SubmissionStatus;
import com.amalitech.task.service.model.enums.TaskDifficulty;
import com.amalitech.task.service.model.enums.TaskType;
import com.amalitech.task.service.model.content.impl.McqTaskContent;
import com.amalitech.task.service.model.feedback.impl.CodingSubmissionFeedback;
import com.amalitech.task.service.model.feedback.impl.McqSubmissionFeedback;
import com.amalitech.task.service.model.submission.impl.CodingSubmissionAnswer;
import com.amalitech.task.service.model.submission.impl.McqSubmissionAnswer;
import com.amalitech.task.service.repository.TaskRepository;
import com.amalitech.task.service.repository.TaskSubmissionRepository;
import com.amalitech.task.service.events.EventProducer;
import com.amalitech.task.service.mapper.TaskCompletionMapper;
import com.amalitech.task.service.validation.TaskCompletedEventValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceImplTest {

    @Mock
    private TaskSubmissionRepository submissionRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private EventProducer eventProducer;

    @Mock
    private SubmissionMapper submissionMapper;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private FallbackFeedbackMapper fallbackMapper;

    @Mock
    private TaskCompletionMapper taskCompletionMapper;

    @Mock
    private TaskCompletedEventValidator validator;

    @InjectMocks
    private SubmissionServiceImpl submissionService;

    private UUID userId;
    private UUID taskId;
    private UUID submissionId;
    private Task testTask;
    private TaskSubmission testSubmission;
    private TaskSubmissionDTO testSubmissionDTO;
    private SubmitAnswerRequest submitAnswerRequest;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        taskId = UUID.randomUUID();
        submissionId = UUID.randomUUID();

        testTask = Task.builder()
                .id(taskId)
                .title("Test Task")
                .description("Test Description")
                .type(TaskType.CODING)
                .difficulty(TaskDifficulty.BEGINNER)
                .build();

        testSubmission = new TaskSubmission();
        testSubmission.setId(submissionId);
        testSubmission.setUserId(userId);
        testSubmission.setTask(testTask);
        testSubmission.setStatus(SubmissionStatus.PENDING);

        testSubmissionDTO = new TaskSubmissionDTO();
        testSubmissionDTO.setId(submissionId);
        testSubmissionDTO.setStatus(SubmissionStatus.PENDING);

        CodingSubmissionAnswer answer = new CodingSubmissionAnswer("test answer", 71);
        submitAnswerRequest = new SubmitAnswerRequest(taskId, answer);
    }

    @Test
    void testCreateSubmission_Success() {
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(testTask));
        when(submissionRepository.save(any(TaskSubmission.class))).thenReturn(testSubmission);
        when(submissionMapper.toCreatedEvent(testSubmission)).thenReturn(
                SubmissionCreatedEvent.builder()
                        .submissionId(submissionId)
                        .userId(userId)
                        .taskId(taskId)
                        .taskType("CODING")
                        .contentToEvaluate("test answer")
                        .build()
        );
        when(submissionMapper.toDTO(testSubmission)).thenReturn(testSubmissionDTO);

        TaskSubmissionDTO result = submissionService.createSubmission(submitAnswerRequest, userId);

        assertNotNull(result);
        assertEquals(submissionId, result.getId());
        verify(taskRepository, times(1)).findById(taskId);
        verify(submissionRepository, times(1)).save(any(TaskSubmission.class));
        verify(eventProducer, times(1)).publishSubmissionCreated(any(SubmissionCreatedEvent.class));
    }

    @Test
    void testCreateSubmission_TaskNotFound() {
        when(taskRepository.findById(taskId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            submissionService.createSubmission(submitAnswerRequest, userId);
        });

        verify(taskRepository, times(1)).findById(taskId);
        verify(submissionRepository, never()).save(any());
        verify(eventProducer, never()).publishSubmissionCreated(any());
    }

    @Test
    void testGetSubmissionById_Success() {
        when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(testSubmission));
        when(submissionMapper.toDTO(testSubmission)).thenReturn(testSubmissionDTO);

        TaskSubmissionDTO result = submissionService.getSubmissionById(submissionId);

        assertNotNull(result);
        assertEquals(submissionId, result.getId());
        verify(submissionRepository, times(1)).findById(submissionId);
    }

    @Test
    void testGetSubmissionById_NotFound() {
        when(submissionRepository.findById(submissionId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            submissionService.getSubmissionById(submissionId);
        });

        verify(submissionRepository, times(1)).findById(submissionId);
    }

    @Test
    void testUpdateSubmissionFromEvent_SuccessWithDetailedFeedback() throws Exception {
        CodingSubmissionFeedback.Evaluation evaluation = CodingSubmissionFeedback.Evaluation.builder()
                .correctness(CodingSubmissionFeedback.CorrectnessEvaluation.builder()
                        .score(85)
                        .maxScore(100)
                        .percentage(85)
                        .build())
                .build();
        CodingSubmissionFeedback feedback = CodingSubmissionFeedback.builder()
                .evaluation(evaluation)
                .build();

        String feedbackJson = "{\"evaluation\": {\"correctness\": {\"score\": 85, \"maxScore\": 100}}}";

        SubmissionEvaluatedEvent event = SubmissionEvaluatedEvent.builder()
                .submissionId(submissionId)
                .userId(userId)
                .status("COMPLETED")
                .score(85)
                .isCorrect(true)
                .detailedFeedback(feedbackJson)
                .feedbackType("CODING")
                .build();

        when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(testSubmission));
        when(objectMapper.readValue(eq(feedbackJson), eq(com.amalitech.task.service.model.feedback.SubmissionFeedback.class)))
                .thenReturn(feedback);
        when(submissionRepository.save(any(TaskSubmission.class))).thenReturn(testSubmission);

        submissionService.updateSubmissionFromEvent(event);

        verify(submissionRepository, times(1)).findById(submissionId);
        verify(submissionRepository, times(1)).save(any(TaskSubmission.class));
        verify(objectMapper, times(1)).readValue(eq(feedbackJson), eq(com.amalitech.task.service.model.feedback.SubmissionFeedback.class));
    }

    @Test
    void testUpdateSubmissionFromEvent_WithFallbackFeedback() {
        SubmissionEvaluatedEvent event = SubmissionEvaluatedEvent.builder()
                .submissionId(submissionId)
                .userId(userId)
                .score(70)
                .isCorrect(true)
                .status("COMPLETED")
                .detailedFeedback("")
                .feedbackType("CODING")
                .build();

        CodingSubmissionFeedback fallbackFeedback = new CodingSubmissionFeedback();

        when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(testSubmission));
        when(fallbackMapper.createBasicCodingFeedback(event)).thenReturn(fallbackFeedback);
        when(submissionRepository.save(any(TaskSubmission.class))).thenReturn(testSubmission);

        submissionService.updateSubmissionFromEvent(event);

        verify(submissionRepository, times(1)).findById(submissionId);
        verify(fallbackMapper, times(1)).createBasicCodingFeedback(event);
        verify(submissionRepository, times(1)).save(any(TaskSubmission.class));
    }

    @Test
    void testUpdateSubmissionFromEvent_InvalidStatus() {
        SubmissionEvaluatedEvent event = SubmissionEvaluatedEvent.builder()
                .submissionId(submissionId)
                .userId(userId)
                .score(80)
                .isCorrect(true)
                .status("INVALID_STATUS")
                .detailedFeedback(null)
                .feedbackType("CODING")
                .build();

        when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(testSubmission));
        when(fallbackMapper.createBasicCodingFeedback(event)).thenReturn(new CodingSubmissionFeedback());
        when(submissionRepository.save(any(TaskSubmission.class))).thenReturn(testSubmission);

        submissionService.updateSubmissionFromEvent(event);

        assertEquals(SubmissionStatus.COMPLETED, testSubmission.getStatus());
        verify(submissionRepository, times(1)).save(any(TaskSubmission.class));
    }

    @Test
    void testUpdateSubmissionFromEvent_SubmissionNotFound() {
        SubmissionEvaluatedEvent event = SubmissionEvaluatedEvent.builder()
                .submissionId(submissionId)
                .userId(userId)
                .score(80)
                .isCorrect(true)
                .status("COMPLETED")
                .detailedFeedback(null)
                .feedbackType("CODING")
                .build();

        when(submissionRepository.findById(submissionId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            submissionService.updateSubmissionFromEvent(event);
        });

        verify(submissionRepository, times(1)).findById(submissionId);
    }

    @Test
    void testUpdateSubmissionFromEvent_EssayFeedback() {
        SubmissionEvaluatedEvent event = SubmissionEvaluatedEvent.builder()
                .submissionId(submissionId)
                .userId(userId)
                .score(75)
                .isCorrect(true)
                .status("COMPLETED")
                .detailedFeedback("")
                .feedbackType("ESSAY")
                .build();

        when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(testSubmission));
        when(fallbackMapper.createBasicEssayFeedback(event))
                .thenReturn(new com.amalitech.task.service.model.feedback.impl.EssaySubmissionFeedback());
        when(submissionRepository.save(any(TaskSubmission.class))).thenReturn(testSubmission);

        submissionService.updateSubmissionFromEvent(event);

        verify(submissionRepository, times(1)).findById(submissionId);
        verify(fallbackMapper, times(1)).createBasicEssayFeedback(event);
    }

    @Test
    void testCreateSubmission_EventPublished() {
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(testTask));
        when(submissionRepository.save(any(TaskSubmission.class))).thenReturn(testSubmission);
        when(submissionMapper.toCreatedEvent(testSubmission)).thenReturn(SubmissionCreatedEvent.builder()
                .submissionId(submissionId)
                .userId(userId)
                .taskId(taskId)
                .taskType("CODING")
                .contentToEvaluate("test answer")
                .build());
        when(submissionMapper.toDTO(testSubmission)).thenReturn(testSubmissionDTO);

        submissionService.createSubmission(submitAnswerRequest, userId);

        verify(eventProducer, times(1)).publishSubmissionCreated(any(SubmissionCreatedEvent.class));
    }

    // ===================================================================
    // --- MCQ Synchronous Evaluation Tests ---
    // ===================================================================

    @Test
    void testCreateMcqSubmission_AllCorrect() {
        // Setup MCQ task with 2 questions
        Task mcqTask = Task.builder()
                .id(taskId)
                .title("MCQ Test")
                .description("Test MCQ")
                .type(TaskType.MULTIPLE_CHOICE)
                .difficulty(TaskDifficulty.BEGINNER)
                .xpReward(50)
                .build();

        McqTaskContent content = new McqTaskContent(Arrays.asList(
                McqTaskContent.Question.builder()
                        .question_number("1")
                        .question_text("What is 2+2?")
                        .options(Arrays.asList("3", "4", "5"))
                        .correct_answer(1)
                        .explanation("4 is correct")
                        .build(),
                McqTaskContent.Question.builder()
                        .question_number("2")
                        .question_text("What is 3+3?")
                        .options(Arrays.asList("5", "6", "7"))
                        .correct_answer(1)
                        .explanation("6 is correct")
                        .build()
        ));
        mcqTask.setContent(content);

        McqSubmissionAnswer answer = new McqSubmissionAnswer(Arrays.asList(
                new McqSubmissionAnswer.QuestionAnswer("1", 1),
                new McqSubmissionAnswer.QuestionAnswer("2", 1)
        ));

        SubmitAnswerRequest request = new SubmitAnswerRequest(taskId, answer);

        TaskSubmission[] capturedSubmission = new TaskSubmission[1];
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(mcqTask));
        when(submissionRepository.save(any(TaskSubmission.class))).thenAnswer(invocation -> {
            capturedSubmission[0] = invocation.getArgument(0);
            capturedSubmission[0].setId(submissionId);
            return capturedSubmission[0];
        });
        when(submissionMapper.toDTO(any(TaskSubmission.class))).thenReturn(new TaskSubmissionDTO());

        TaskSubmissionDTO result = submissionService.createSubmission(request, userId);

        assertNotNull(result);
        // Verify status is COMPLETED (sync evaluation)
        assertEquals(SubmissionStatus.COMPLETED, capturedSubmission[0].getStatus());
        // Verify score was calculated (2/2 correct)
        assertEquals(100, capturedSubmission[0].getScoreEarned());
        // Verify isCorrect is true
        assertTrue(capturedSubmission[0].getIsCorrect());
        verify(submissionRepository, times(1)).save(any(TaskSubmission.class));
    }

    @Test
    void testCreateMcqSubmission_MixedAnswers() {
        // Setup MCQ task
        Task mcqTask = Task.builder()
                .id(taskId)
                .title("MCQ Test")
                .description("Test MCQ")
                .type(TaskType.MULTIPLE_CHOICE)
                .difficulty(TaskDifficulty.INTERMEDIATE)
                .xpReward(50)
                .build();

        McqTaskContent content = new McqTaskContent(Arrays.asList(
                McqTaskContent.Question.builder()
                        .question_number("1")
                        .question_text("What is 2+2?")
                        .options(Arrays.asList("3", "4", "5"))
                        .correct_answer(1)
                        .explanation("4 is correct")
                        .build(),
                McqTaskContent.Question.builder()
                        .question_number("2")
                        .question_text("What is 3+3?")
                        .options(Arrays.asList("5", "6", "7"))
                        .correct_answer(1)
                        .explanation("6 is correct")
                        .build()
        ));
        mcqTask.setContent(content);

        // One correct, one incorrect
        McqSubmissionAnswer answer = new McqSubmissionAnswer(Arrays.asList(
                new McqSubmissionAnswer.QuestionAnswer("1", 1),  // Correct
                new McqSubmissionAnswer.QuestionAnswer("2", 0)   // Wrong
        ));

        SubmitAnswerRequest request = new SubmitAnswerRequest(taskId, answer);

        TaskSubmission[] capturedSubmission = new TaskSubmission[1];
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(mcqTask));
        when(submissionRepository.save(any(TaskSubmission.class))).thenAnswer(invocation -> {
            capturedSubmission[0] = invocation.getArgument(0);
            capturedSubmission[0].setId(submissionId);
            return capturedSubmission[0];
        });
        when(submissionMapper.toDTO(any(TaskSubmission.class))).thenReturn(new TaskSubmissionDTO());

        TaskSubmissionDTO result = submissionService.createSubmission(request, userId);

        assertNotNull(result);
        // Verify status is COMPLETED
        assertEquals(SubmissionStatus.COMPLETED, capturedSubmission[0].getStatus());
        // Verify score is 50 (1/2 correct)
        assertEquals(50, capturedSubmission[0].getScoreEarned());
        // Verify isCorrect is false (not all questions correct)
        assertFalse(capturedSubmission[0].getIsCorrect());
    }

    @Test
    void testCreateMcqSubmission_AllWrong() {
        // Setup MCQ task
        Task mcqTask = Task.builder()
                .id(taskId)
                .title("MCQ Test")
                .description("Test MCQ")
                .type(TaskType.MULTIPLE_CHOICE)
                .difficulty(TaskDifficulty.ADVANCED)
                .xpReward(50)
                .build();

        McqTaskContent content = new McqTaskContent(Arrays.asList(
                McqTaskContent.Question.builder()
                        .question_number("1")
                        .question_text("What is 2+2?")
                        .options(Arrays.asList("3", "4", "5"))
                        .correct_answer(1)
                        .explanation("4 is correct")
                        .build(),
                McqTaskContent.Question.builder()
                        .question_number("2")
                        .question_text("What is 3+3?")
                        .options(Arrays.asList("5", "6", "7"))
                        .correct_answer(1)
                        .explanation("6 is correct")
                        .build()
        ));
        mcqTask.setContent(content);

        // All wrong
        McqSubmissionAnswer answer = new McqSubmissionAnswer(Arrays.asList(
                new McqSubmissionAnswer.QuestionAnswer("1", 0),  // Wrong
                new McqSubmissionAnswer.QuestionAnswer("2", 2)   // Wrong
        ));

        SubmitAnswerRequest request = new SubmitAnswerRequest(taskId, answer);

        TaskSubmission[] capturedSubmission = new TaskSubmission[1];
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(mcqTask));
        when(submissionRepository.save(any(TaskSubmission.class))).thenAnswer(invocation -> {
            capturedSubmission[0] = invocation.getArgument(0);
            capturedSubmission[0].setId(submissionId);
            return capturedSubmission[0];
        });
        when(submissionMapper.toDTO(any(TaskSubmission.class))).thenReturn(new TaskSubmissionDTO());

        TaskSubmissionDTO result = submissionService.createSubmission(request, userId);

        assertNotNull(result);
        // Verify status is COMPLETED
        assertEquals(SubmissionStatus.COMPLETED, capturedSubmission[0].getStatus());
        // Verify score is 0 (0/2 correct)
        assertEquals(0, capturedSubmission[0].getScoreEarned());
        // Verify isCorrect is false
        assertFalse(capturedSubmission[0].getIsCorrect());
    }

    @Test
    void testCreateMcqSubmission_SyncEvaluation_StatusIsCompleted() {
        // Verify MCQ submissions get COMPLETED status immediately (sync evaluation)
        Task mcqTask = Task.builder()
                .id(taskId)
                .title("MCQ Test")
                .type(TaskType.MULTIPLE_CHOICE)
                .difficulty(TaskDifficulty.BEGINNER)
                .build();

        McqTaskContent content = new McqTaskContent(Arrays.asList(
                McqTaskContent.Question.builder()
                        .question_number("1")
                        .question_text("Test?")
                        .options(Arrays.asList("Yes", "No"))
                        .correct_answer(0)
                        .explanation("Yes")
                        .build()
        ));
        mcqTask.setContent(content);

        McqSubmissionAnswer answer = new McqSubmissionAnswer(Arrays.asList(
                new McqSubmissionAnswer.QuestionAnswer("1", 0)
        ));

        SubmitAnswerRequest request = new SubmitAnswerRequest(taskId, answer);

        // Capture the saved submission to verify its state
        TaskSubmission[] capturedSubmission = new TaskSubmission[1];
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(mcqTask));
        when(submissionRepository.save(any(TaskSubmission.class))).thenAnswer(invocation -> {
            capturedSubmission[0] = invocation.getArgument(0);
            capturedSubmission[0].setId(submissionId);
            return capturedSubmission[0];
        });
        when(submissionMapper.toDTO(any(TaskSubmission.class))).thenReturn(new TaskSubmissionDTO());

        submissionService.createSubmission(request, userId);

        // Verify status was set to COMPLETED (not PENDING)
        assertEquals(SubmissionStatus.COMPLETED, capturedSubmission[0].getStatus());
    }

    @Test
    void testCreateMcqSubmission_DoesNotPublishSubmissionCreatedEvent() {
        // Verify MCQ does NOT publish SubmissionCreatedEvent (unlike ESSAY/CODING)
        Task mcqTask = Task.builder()
                .id(taskId)
                .title("MCQ Test")
                .type(TaskType.MULTIPLE_CHOICE)
                .difficulty(TaskDifficulty.BEGINNER)
                .build();

        McqTaskContent content = new McqTaskContent(Arrays.asList(
                McqTaskContent.Question.builder()
                        .question_number("1")
                        .question_text("Test?")
                        .options(Arrays.asList("Yes", "No"))
                        .correct_answer(0)
                        .explanation("Yes")
                        .build()
        ));
        mcqTask.setContent(content);

        McqSubmissionAnswer answer = new McqSubmissionAnswer(Arrays.asList(
                new McqSubmissionAnswer.QuestionAnswer("1", 0)
        ));

        SubmitAnswerRequest request = new SubmitAnswerRequest(taskId, answer);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(mcqTask));
        when(submissionRepository.save(any(TaskSubmission.class))).thenReturn(new TaskSubmission());
        when(submissionMapper.toDTO(any(TaskSubmission.class))).thenReturn(new TaskSubmissionDTO());

        submissionService.createSubmission(request, userId);

        // Verify SubmissionCreatedEvent is NOT published for MCQ
        verify(eventProducer, never()).publishSubmissionCreated(any(SubmissionCreatedEvent.class));
    }

    @Test
    void testCreateMcqSubmission_EvaluatesImmediately() {
        // Verify MCQ evaluation happens synchronously in createSubmission
        Task mcqTask = Task.builder()
                .id(taskId)
                .title("MCQ Test")
                .type(TaskType.MULTIPLE_CHOICE)
                .difficulty(TaskDifficulty.BEGINNER)
                .build();

        McqTaskContent content = new McqTaskContent(Arrays.asList(
                McqTaskContent.Question.builder()
                        .question_number("1")
                        .question_text("Test?")
                        .options(Arrays.asList("Yes", "No"))
                        .correct_answer(0)
                        .explanation("Yes")
                        .build()
        ));
        mcqTask.setContent(content);

        McqSubmissionAnswer answer = new McqSubmissionAnswer(Arrays.asList(
                new McqSubmissionAnswer.QuestionAnswer("1", 0)
        ));

        SubmitAnswerRequest request = new SubmitAnswerRequest(taskId, answer);

        TaskSubmission[] capturedSubmission = new TaskSubmission[1];
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(mcqTask));
        when(submissionRepository.save(any(TaskSubmission.class))).thenAnswer(invocation -> {
            capturedSubmission[0] = invocation.getArgument(0);
            capturedSubmission[0].setId(submissionId);
            return capturedSubmission[0];
        });
        when(submissionMapper.toDTO(any(TaskSubmission.class))).thenReturn(new TaskSubmissionDTO());

        submissionService.createSubmission(request, userId);

        // Verify submission was evaluated synchronously (has feedback)
        assertNotNull(capturedSubmission[0].getFeedback());
        // Verify feedback is MCQSubmissionFeedback
        assertInstanceOf(McqSubmissionFeedback.class, capturedSubmission[0].getFeedback());
    }

    @Test
    void testCreateCodingSubmission_StillPublishesSubmissionCreatedEvent() {
        // Verify CODING tasks still use async path (SubmissionCreatedEvent)
        Task codingTask = Task.builder()
                .id(taskId)
                .title("Coding Test")
                .type(TaskType.CODING)
                .difficulty(TaskDifficulty.BEGINNER)
                .build();

        CodingSubmissionAnswer answer = new CodingSubmissionAnswer("code", 71);
        SubmitAnswerRequest request = new SubmitAnswerRequest(taskId, answer);

        TaskSubmission savedSubmission = new TaskSubmission();
        savedSubmission.setId(submissionId);
        savedSubmission.setStatus(SubmissionStatus.PENDING);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(codingTask));
        when(submissionRepository.save(any(TaskSubmission.class))).thenReturn(savedSubmission);
        when(submissionMapper.toDTO(any(TaskSubmission.class))).thenReturn(new TaskSubmissionDTO());
        when(submissionMapper.toCreatedEvent(any(TaskSubmission.class)))
                .thenReturn(SubmissionCreatedEvent.builder().build());

        submissionService.createSubmission(request, userId);

        // Verify SubmissionCreatedEvent IS published for CODING (async path)
        verify(eventProducer, times(1)).publishSubmissionCreated(any(SubmissionCreatedEvent.class));
        // Verify TaskCompletedEvent is NOT published (will come later from async path)
        verify(eventProducer, never()).publishTaskCompleted(any());
    }
}
