package com.amalitech.task.service.events;

import com.amalitech.common.event.events.SubmissionCreatedEvent;
import com.amalitech.common.event.events.SubmissionEvaluatedEvent;
import com.amalitech.task.service.config.RabbitMQConfig;
import com.amalitech.task.service.dto.request.BatchGenerationRequest;
import com.amalitech.task.service.dto.request.GenerateTaskRequest;
import com.amalitech.task.service.model.enums.TaskDifficulty;
import com.amalitech.task.service.model.enums.TaskType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RabbitMQEventProducerTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private RabbitMQEventProducer eventProducer;

    private UUID userId;
    private UUID taskId;
    private UUID submissionId;
    private BatchGenerationRequest batchRequest;
    private GenerateTaskRequest adminRequest;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        taskId = UUID.randomUUID();
        submissionId = UUID.randomUUID();

        batchRequest = new BatchGenerationRequest(
                userId,
                "PYTHON",
                TaskDifficulty.BEGINNER,
                5,
                TaskType.CODING
        );

        adminRequest = new GenerateTaskRequest(
                userId,
                TaskType.CODING,
                "PYTHON",
                TaskDifficulty.INTERMEDIATE,
                "String Manipulation",
                "Python"
        );
    }

    @Test
    void testRequestBatchTaskGeneration() {
        eventProducer.requestBatchTaskGeneration(batchRequest);

        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(RabbitMQConfig.TASK_GENERATION_EXCHANGE),
                eq(RabbitMQConfig.BATCH_GENERATION_ROUTING_KEY),
                eq(batchRequest)
        );
    }

    @Test
    void testRequestSpecificTaskGeneration() {
        eventProducer.requestSpecificTaskGeneration(adminRequest);

        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(RabbitMQConfig.TASK_GENERATION_EXCHANGE),
                eq(RabbitMQConfig.ADMIN_GENERATION_ROUTING_KEY),
                eq(adminRequest)
        );
    }

    @Test
    void testPublishSubmissionCreated() {
        SubmissionCreatedEvent event = SubmissionCreatedEvent.builder()
                .submissionId(submissionId)
                .userId(userId)
                .taskId(taskId)
                .taskType("CODING")
                .contentToEvaluate("test answer")
                .skillName("PYTHON")
                .difficulty("BEGINNER")
                .build();

        eventProducer.publishSubmissionCreated(event);

        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(RabbitMQConfig.SUBMISSION_EXCHANGE),
                eq(RabbitMQConfig.SUBMISSION_CREATED_ROUTING_KEY),
                eq(event)
        );
    }

    @Test
    void testPublishSubmissionEvaluated() {
        SubmissionEvaluatedEvent event = SubmissionEvaluatedEvent.builder()
                .submissionId(submissionId)
                .userId(userId)
                .status("COMPLETED")
                .score(85)
                .isCorrect(true)
                .overallFeedback("Good answer")
                .feedbackType("CODING")
                .build();

        eventProducer.publishSubmissionEvaluated(event);

        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(RabbitMQConfig.SUBMISSION_EXCHANGE),
                eq(RabbitMQConfig.SUBMISSION_EVALUATED_ROUTING_KEY),
                eq(event)
        );
    }

    @Test
    void testRequestBatchTaskGeneration_MultipleTypes() {
        BatchGenerationRequest essayRequest = new BatchGenerationRequest(
                userId,
                "JAVA",
                TaskDifficulty.INTERMEDIATE,
                3,
                TaskType.ESSAY
        );

        eventProducer.requestBatchTaskGeneration(essayRequest);

        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(RabbitMQConfig.TASK_GENERATION_EXCHANGE),
                eq(RabbitMQConfig.BATCH_GENERATION_ROUTING_KEY),
                eq(essayRequest)
        );
    }

    @Test
    void testRequestSpecificTaskGeneration_EssayTask() {
        GenerateTaskRequest essayRequest = new GenerateTaskRequest(
                userId,
                TaskType.ESSAY,
                "JAVA",
                TaskDifficulty.ADVANCED,
                "Advanced Concurrency",
                "Java"
        );

        eventProducer.requestSpecificTaskGeneration(essayRequest);

        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(RabbitMQConfig.TASK_GENERATION_EXCHANGE),
                eq(RabbitMQConfig.ADMIN_GENERATION_ROUTING_KEY),
                eq(essayRequest)
        );
    }

    @Test
    void testPublishSubmissionCreated_DifferentUsers() {
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();

        SubmissionCreatedEvent event1 = SubmissionCreatedEvent.builder()
                .submissionId(submissionId)
                .userId(user1)
                .taskId(taskId)
                .taskType("CODING")
                .contentToEvaluate("answer1")
                .skillName("PYTHON")
                .difficulty("BEGINNER")
                .build();
        SubmissionCreatedEvent event2 = SubmissionCreatedEvent.builder()
                .submissionId(UUID.randomUUID())
                .userId(user2)
                .taskId(taskId)
                .taskType("CODING")
                .contentToEvaluate("answer2")
                .skillName("PYTHON")
                .difficulty("BEGINNER")
                .build();

        eventProducer.publishSubmissionCreated(event1);
        eventProducer.publishSubmissionCreated(event2);

        verify(rabbitTemplate, times(2)).convertAndSend(
                eq(RabbitMQConfig.SUBMISSION_EXCHANGE),
                eq(RabbitMQConfig.SUBMISSION_CREATED_ROUTING_KEY),
                any(SubmissionCreatedEvent.class)
        );
    }

    @Test
    void testPublishSubmissionEvaluated_CorrectAnswer() {
        SubmissionEvaluatedEvent event = SubmissionEvaluatedEvent.builder()
                .submissionId(submissionId)
                .userId(userId)
                .status("COMPLETED")
                .score(100)
                .isCorrect(true)
                .overallFeedback("Perfect answer")
                .feedbackType("CODING")
                .build();

        eventProducer.publishSubmissionEvaluated(event);

        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(RabbitMQConfig.SUBMISSION_EXCHANGE),
                eq(RabbitMQConfig.SUBMISSION_EVALUATED_ROUTING_KEY),
                eq(event)
        );
    }

    @Test
    void testPublishSubmissionEvaluated_IncorrectAnswer() {
        SubmissionEvaluatedEvent event = SubmissionEvaluatedEvent.builder()
                .submissionId(submissionId)
                .userId(userId)
                .status("COMPLETED")
                .score(45)
                .isCorrect(false)
                .overallFeedback("Incorrect answer")
                .feedbackType("ESSAY")
                .build();

        eventProducer.publishSubmissionEvaluated(event);

        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(RabbitMQConfig.SUBMISSION_EXCHANGE),
                eq(RabbitMQConfig.SUBMISSION_EVALUATED_ROUTING_KEY),
                eq(event)
        );
    }
}
