package com.amalitech.task.service.events;

import com.amalitech.common.event.events.SubmissionCreatedEvent;
import com.amalitech.common.event.events.SubmissionEvaluatedEvent;
import com.amalitech.common.event.events.TaskCompletedEvent;
import com.amalitech.task.service.config.RabbitMQConfig;
import com.amalitech.task.service.dto.request.BatchGenerationRequest;
import com.amalitech.task.service.dto.request.GenerateTaskRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * Service component responsible for producing and publishing asynchronous events and requests
 * to the RabbitMQ message broker.
 * <p>
 * This class serves as the primary gateway for the Task Service to communicate with other
 * microservices (like the AI Evaluation Service and the Notification Service) and the
 * dedicated AI Task Generator agent. It encapsulates the messaging logic, shielding
 * business services from direct broker interactions.
 */
@Service
@Primary
@RequiredArgsConstructor
@Slf4j
public class RabbitMQEventProducer implements EventProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Publishes a request to the AI Task Generation service to create a batch of tasks.
     * <p>
     * This method is typically triggered by a user's action or an adaptive learning
     * agent to ensure the immediate availability (restock) of tasks for a given skill.
     * It sends the request to a dedicated queue processed by the task generation worker.
     *
     * @param request The {@link BatchGenerationRequest} detailing the desired skill, difficulty, and count.
     */
    @Override
    public void requestBatchTaskGeneration(BatchGenerationRequest request) {
        log.info("Publishing BATCH generation request: {}", request);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.TASK_GENERATION_EXCHANGE,
                RabbitMQConfig.BATCH_GENERATION_ROUTING_KEY,
                request
        );
    }

    /**
     * Publishes a request for the manual, specific generation of a single task.
     * <p>
     * This method is often used by administrative tools or content managers to
     * generate high-quality, precise tasks that are not part of the standard batch flow.
     *
     * @param request The {@link GenerateTaskRequest} detailing the specific task parameters (topic, language, etc.).
     */
    @Override
    public void requestSpecificTaskGeneration(GenerateTaskRequest request) {
        log.info("Publishing ADMIN generation request: {}", request);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.TASK_GENERATION_EXCHANGE,
                RabbitMQConfig.ADMIN_GENERATION_ROUTING_KEY,
                request
        );
    }

    /**
     * Publishes an event indicating that a user has submitted an answer to a task.
     * <p>
     * This event triggers the downstream Evaluation Service to begin processing
     * and scoring the user's submission, adhering to the asynchronous, decoupled
     * microservice architecture.
     *
     * @param submission The {@link SubmissionCreatedEvent} containing the necessary details for evaluation.
     */
    public void publishSubmissionCreated(SubmissionCreatedEvent submission) {
        log.info("Publishing submission created event: {}", submission.getSubmissionId());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.SUBMISSION_EXCHANGE,
                RabbitMQConfig.SUBMISSION_CREATED_ROUTING_KEY,
                submission
        );
    }

    /**
     * Publishes an event when the evaluation of a submission has been completed.
     * <p>
     * This event notifies services, such as the Notification Service, that results are
     * available, potentially triggering user feedback or progress updates.
     *
     * @param event The fully evaluated {@link SubmissionEvaluatedEvent} with scores and feedback.
     */
    public void publishSubmissionEvaluated(SubmissionEvaluatedEvent event) {
        log.info("Publishing submission evaluated event: {}", event.getSubmissionId());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.SUBMISSION_EXCHANGE,
                RabbitMQConfig.SUBMISSION_EVALUATED_ROUTING_KEY,
                event
        );
    }

    /**
     * Publishes an event when a task has been completed by a user.
     * <p>
     * This event notifies analytics services with comprehensive task completion information
     * including XP earned, pass/fail status, and detailed rubric scores for analysis
     * and progress tracking.
     *
     * @param event The {@link TaskCompletedEvent} containing completion details.
     */
    public void publishTaskCompleted(TaskCompletedEvent event) {
        log.info("Publishing task completed event for user: {} and task: {}", event.getUserId(), event.getTaskId());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.TASK_EXCHANGE,
                RabbitMQConfig.TASK_COMPLETED_ROUTING_KEY,
                event
        );
    }
}