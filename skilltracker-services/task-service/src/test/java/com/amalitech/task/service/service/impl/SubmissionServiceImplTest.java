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
import com.amalitech.task.service.model.feedback.impl.CodingSubmissionFeedback;
import com.amalitech.task.service.model.submission.impl.CodingSubmissionAnswer;
import com.amalitech.task.service.repository.TaskRepository;
import com.amalitech.task.service.repository.TaskSubmissionRepository;
import com.amalitech.task.service.events.EventProducer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
}
