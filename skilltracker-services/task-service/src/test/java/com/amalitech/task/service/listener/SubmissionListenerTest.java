package com.amalitech.task.service.listener;

import com.amalitech.common.event.events.SubmissionEvaluatedEvent;
import com.amalitech.task.service.service.SubmissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubmissionListenerTest {

    @Mock
    private SubmissionService submissionService;

    @InjectMocks
    private SubmissionListener submissionListener;

    private UUID submissionId;
    private UUID userId;
    private SubmissionEvaluatedEvent testEvent;

    @BeforeEach
    void setUp() {
        submissionId = UUID.randomUUID();
        userId = UUID.randomUUID();

        testEvent = SubmissionEvaluatedEvent.builder()
                .submissionId(submissionId)
                .userId(userId)
                .score(85)
                .isCorrect(true)
                .status("COMPLETED")
                .detailedFeedback("{}")
                .feedbackType("CODING")
                .build();
    }

    @Test
    void testHandleSubmissionEvaluated_Success() {
        doNothing().when(submissionService).updateSubmissionFromEvent(any(SubmissionEvaluatedEvent.class));

        submissionListener.handleSubmissionEvaluated(testEvent);

        verify(submissionService, times(1)).updateSubmissionFromEvent(testEvent);
    }

    @Test
    void testHandleSubmissionEvaluated_WithDifferentScores() {
        SubmissionEvaluatedEvent[] events = {
                createEventWithScore(100),
                createEventWithScore(75),
                createEventWithScore(50),
                createEventWithScore(0)
        };

        for (SubmissionEvaluatedEvent event : events) {
            doNothing().when(submissionService).updateSubmissionFromEvent(event);
            submissionListener.handleSubmissionEvaluated(event);
            verify(submissionService, times(1)).updateSubmissionFromEvent(event);
        }
    }

    @Test
    void testHandleSubmissionEvaluated_WithCorrectFlag() {
        SubmissionEvaluatedEvent correctEvent = SubmissionEvaluatedEvent.builder()
                .submissionId(submissionId)
                .userId(userId)
                .score(100)
                .isCorrect(true)
                .status("COMPLETED")
                .build();

        SubmissionEvaluatedEvent incorrectEvent = SubmissionEvaluatedEvent.builder()
                .submissionId(UUID.randomUUID())
                .userId(userId)
                .score(30)
                .isCorrect(false)
                .status("COMPLETED")
                .build();

        doNothing().when(submissionService).updateSubmissionFromEvent(any());

        submissionListener.handleSubmissionEvaluated(correctEvent);
        submissionListener.handleSubmissionEvaluated(incorrectEvent);

        verify(submissionService, times(2)).updateSubmissionFromEvent(any());
    }

    @Test
    void testHandleSubmissionEvaluated_WithDifferentStatuses() {
        SubmissionEvaluatedEvent completedEvent = createEventWithStatus("COMPLETED");
        SubmissionEvaluatedEvent failedEvent = createEventWithStatus("FAILED");
        SubmissionEvaluatedEvent pendingEvent = createEventWithStatus("PENDING");

        doNothing().when(submissionService).updateSubmissionFromEvent(any());

        submissionListener.handleSubmissionEvaluated(completedEvent);
        submissionListener.handleSubmissionEvaluated(failedEvent);
        submissionListener.handleSubmissionEvaluated(pendingEvent);

        verify(submissionService, times(3)).updateSubmissionFromEvent(any());
    }

    @Test
    void testHandleSubmissionEvaluated_ServiceThrowsException() {
        doThrow(new RuntimeException("Database error"))
                .when(submissionService).updateSubmissionFromEvent(any());

        assertThrows(AmqpRejectAndDontRequeueException.class, () -> {
            submissionListener.handleSubmissionEvaluated(testEvent);
        });

        verify(submissionService, times(1)).updateSubmissionFromEvent(testEvent);
    }

    @Test
    void testHandleSubmissionEvaluated_ServiceThrowsIllegalArgumentException() {
        doThrow(new IllegalArgumentException("Invalid submission"))
                .when(submissionService).updateSubmissionFromEvent(any());

        assertThrows(AmqpRejectAndDontRequeueException.class, () -> {
            submissionListener.handleSubmissionEvaluated(testEvent);
        });

        verify(submissionService, times(1)).updateSubmissionFromEvent(testEvent);
    }

    @Test
    void testHandleSubmissionEvaluated_NullPointerExceptionHandling() {
        doThrow(new NullPointerException("Null field"))
                .when(submissionService).updateSubmissionFromEvent(any());

        assertThrows(AmqpRejectAndDontRequeueException.class, () -> {
            submissionListener.handleSubmissionEvaluated(testEvent);
        });
    }

    @Test
    void testHandleSubmissionEvaluated_WithDetailedFeedback() {
        String detailedFeedback = "{\"evaluation\": {\"correctness\": {\"score\": 85}}}";
        SubmissionEvaluatedEvent event = SubmissionEvaluatedEvent.builder()
                .submissionId(submissionId)
                .userId(userId)
                .score(85)
                .isCorrect(true)
                .status("COMPLETED")
                .detailedFeedback(detailedFeedback)
                .feedbackType("CODING")
                .build();

        doNothing().when(submissionService).updateSubmissionFromEvent(event);

        submissionListener.handleSubmissionEvaluated(event);

        verify(submissionService, times(1)).updateSubmissionFromEvent(event);
    }

    @Test
    void testHandleSubmissionEvaluated_WithDifferentFeedbackTypes() {
        SubmissionEvaluatedEvent codingEvent = createEventWithFeedbackType("CODING");
        SubmissionEvaluatedEvent essayEvent = createEventWithFeedbackType("ESSAY");
        SubmissionEvaluatedEvent mcqEvent = createEventWithFeedbackType("MCQ");

        doNothing().when(submissionService).updateSubmissionFromEvent(any());

        submissionListener.handleSubmissionEvaluated(codingEvent);
        submissionListener.handleSubmissionEvaluated(essayEvent);
        submissionListener.handleSubmissionEvaluated(mcqEvent);

        verify(submissionService, times(3)).updateSubmissionFromEvent(any());
    }

    @Test
    void testHandleSubmissionEvaluated_MultipleCallsInSequence() {
        doNothing().when(submissionService).updateSubmissionFromEvent(any());

        SubmissionEvaluatedEvent event1 = createEventWithScore(85);
        SubmissionEvaluatedEvent event2 = createEventWithScore(92);
        SubmissionEvaluatedEvent event3 = createEventWithScore(78);

        submissionListener.handleSubmissionEvaluated(event1);
        submissionListener.handleSubmissionEvaluated(event2);
        submissionListener.handleSubmissionEvaluated(event3);

        verify(submissionService, times(3)).updateSubmissionFromEvent(any());
        verify(submissionService, times(1)).updateSubmissionFromEvent(event1);
        verify(submissionService, times(1)).updateSubmissionFromEvent(event2);
        verify(submissionService, times(1)).updateSubmissionFromEvent(event3);
    }

    @Test
    void testHandleSubmissionEvaluated_ExceptionCauseIsPropagated() {
        RuntimeException cause = new RuntimeException("Original cause");
        doThrow(cause).when(submissionService).updateSubmissionFromEvent(any());

        AmqpRejectAndDontRequeueException thrown = assertThrows(
                AmqpRejectAndDontRequeueException.class,
                () -> submissionListener.handleSubmissionEvaluated(testEvent)
        );

        assertEquals(cause, thrown.getCause());
    }

    @Test
    void testHandleSubmissionEvaluated_EmptyFeedback() {
        SubmissionEvaluatedEvent event = SubmissionEvaluatedEvent.builder()
                .submissionId(submissionId)
                .userId(userId)
                .score(0)
                .isCorrect(false)
                .status("COMPLETED")
                .detailedFeedback("")
                .feedbackType("CODING")
                .build();

        doNothing().when(submissionService).updateSubmissionFromEvent(event);

        submissionListener.handleSubmissionEvaluated(event);

        verify(submissionService, times(1)).updateSubmissionFromEvent(event);
    }

    @Test
    void testHandleSubmissionEvaluated_WithTestResults() {
        SubmissionEvaluatedEvent.TestResultData testResult = SubmissionEvaluatedEvent.TestResultData.builder()
                .input("1 2")
                .expectedOutput("3")
                .actualOutput("3")
                .passed(true)
                .statusDescription("Passed")
                .build();

        SubmissionEvaluatedEvent event = SubmissionEvaluatedEvent.builder()
                .submissionId(submissionId)
                .userId(userId)
                .score(100)
                .isCorrect(true)
                .status("COMPLETED")
                .detailedFeedback("")
                .feedbackType("CODING")
                .testResults(java.util.List.of(testResult))
                .build();

        doNothing().when(submissionService).updateSubmissionFromEvent(event);

        submissionListener.handleSubmissionEvaluated(event);

        verify(submissionService, times(1)).updateSubmissionFromEvent(event);
    }

    private SubmissionEvaluatedEvent createEventWithScore(int score) {
        return SubmissionEvaluatedEvent.builder()
                .submissionId(UUID.randomUUID())
                .userId(userId)
                .score(score)
                .isCorrect(score >= 70)
                .status("COMPLETED")
                .detailedFeedback("{}")
                .feedbackType("CODING")
                .build();
    }

    private SubmissionEvaluatedEvent createEventWithStatus(String status) {
        return SubmissionEvaluatedEvent.builder()
                .submissionId(UUID.randomUUID())
                .userId(userId)
                .score(85)
                .isCorrect(true)
                .status(status)
                .detailedFeedback("{}")
                .feedbackType("CODING")
                .build();
    }

    private SubmissionEvaluatedEvent createEventWithFeedbackType(String feedbackType) {
        return SubmissionEvaluatedEvent.builder()
                .submissionId(UUID.randomUUID())
                .userId(userId)
                .score(85)
                .isCorrect(true)
                .status("COMPLETED")
                .detailedFeedback("{}")
                .feedbackType(feedbackType)
                .build();
    }
}
