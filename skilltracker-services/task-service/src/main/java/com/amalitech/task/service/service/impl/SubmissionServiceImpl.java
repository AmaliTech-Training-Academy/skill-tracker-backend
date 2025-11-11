package com.amalitech.task.service.service.impl;

import com.amalitech.common.event.events.SubmissionCreatedEvent;
import com.amalitech.common.event.events.SubmissionEvaluatedEvent;
import com.amalitech.common.event.events.TaskCompletedEvent;
import com.amalitech.task.service.dto.TaskSubmissionDTO;
import com.amalitech.task.service.dto.request.SubmitAnswerRequest;
import com.amalitech.task.service.events.EventProducer;
import com.amalitech.task.service.exception.ResourceNotFoundException;
import com.amalitech.task.service.mapper.FallbackFeedbackMapper;
import com.amalitech.task.service.mapper.SubmissionMapper;
import com.amalitech.task.service.mapper.TaskCompletionMapper;
import com.amalitech.task.service.model.Task;
import com.amalitech.task.service.model.TaskSubmission;
import com.amalitech.task.service.model.enums.SubmissionStatus;
import com.amalitech.task.service.model.feedback.SubmissionFeedback;
import com.amalitech.task.service.model.feedback.impl.CodingSubmissionFeedback;
import com.amalitech.task.service.model.feedback.impl.EssaySubmissionFeedback;
import com.amalitech.task.service.repository.TaskRepository;
import com.amalitech.task.service.repository.TaskSubmissionRepository;
import com.amalitech.task.service.service.SubmissionService;
import com.amalitech.task.service.validation.TaskCompletedEventValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of the {@link SubmissionService} interface, handling the business logic
 * for creating, updating, and retrieving user task submissions.
 * <p>
 * This service is responsible for the critical transactional logic of the Task Service:
 * persisting new submissions and integrating asynchronous evaluation results back into the
 * persistence layer. It serves as a command/query gateway for submission data.
 */
@Service
@Slf4j
public class SubmissionServiceImpl implements SubmissionService {

    private final TaskSubmissionRepository submissionRepository;
    private final TaskRepository taskRepository;
    private final EventProducer eventProducer;
    private final SubmissionMapper submissionMapper;
    private final ObjectMapper objectMapper;
    private final FallbackFeedbackMapper fallbackMapper;
    private final TaskCompletionMapper taskCompletionMapper;
    private final TaskCompletedEventValidator validator;

    public SubmissionServiceImpl(TaskSubmissionRepository submissionRepository,
                                 TaskRepository taskRepository,
                                 EventProducer eventProducer,
                                 SubmissionMapper submissionMapper,
                                 ObjectMapper objectMapper,
                                 FallbackFeedbackMapper fallbackMapper,
                                 TaskCompletionMapper taskCompletionMapper,
                                 TaskCompletedEventValidator validator) {
        this.submissionRepository = submissionRepository;
        this.taskRepository = taskRepository;
        this.eventProducer = eventProducer;
        this.submissionMapper = submissionMapper;
        this.objectMapper = objectMapper;
        this.fallbackMapper = fallbackMapper;
        this.taskCompletionMapper = taskCompletionMapper;
        this.validator = validator;
    }

    /**
     * Creates a new task submission record in the database and publishes a creation event
     * to the message broker to initiate the asynchronous AI evaluation process.
     * <p>
     * This method ensures transactional integrity: it verifies the {@link Task} existence,
     * persists the {@link TaskSubmission} in a PENDING state, and then publishes the
     * {@link SubmissionCreatedEvent} for the downstream Evaluation Service to consume.
     *
     * @param request The {@link SubmitAnswerRequest} containing the task ID and user's answer.
     * @param userId The ID of the authenticated user submitting the answer.
     * @return A {@link TaskSubmissionDTO} representing the newly created submission.
     * @throws ResourceNotFoundException if the specified task does not exist.
     */
    @Override
    @Transactional
    public TaskSubmissionDTO createSubmission(SubmitAnswerRequest request, UUID userId) {
        log.info("Creating submission for user {} and task {}", userId, request.taskId());

        Task task = taskRepository.findById(request.taskId())
                .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + request.taskId()));

        TaskSubmission submission = new TaskSubmission();
        submission.setUserId(userId);
        submission.setTask(task);

        submission.setAnswer(request.answer());
        submission.setStatus(SubmissionStatus.PENDING);

        TaskSubmission savedSubmission = submissionRepository.save(submission);

        SubmissionCreatedEvent event = submissionMapper.toCreatedEvent(savedSubmission);

        eventProducer.publishSubmissionCreated(event);

        log.info("Submission {} created and event published.", savedSubmission.getId());

        return submissionMapper.toDTO(savedSubmission);
    }

    /**
     * Updates an existing submission record using the results received from the
     * asynchronous Evaluation Service via the {@link SubmissionEvaluatedEvent}.
     * <p>
     * This method finds the submission, updates the score, correctness, evaluation time,
     * status, and dynamically constructs the appropriate polymorphic feedback (e.g.,
     * {@link CodingSubmissionFeedback}) based on the event's payload. It then publishes
     * a {@link TaskCompletedEvent} to analytics services. It is typically called by a
     * message listener.
     *
     * @param event The {@link SubmissionEvaluatedEvent} containing the evaluation results.
     * @throws ResourceNotFoundException if the submission ID in the event does not match an existing record.
     */
    @Override
    @Transactional
    public void updateSubmissionFromEvent(SubmissionEvaluatedEvent event) {
        log.info("Updating submission {} from evaluated event.", event.getSubmissionId());

        TaskSubmission existingSubmission = submissionRepository.findById(event.getSubmissionId())
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found: " + event.getSubmissionId()));

        existingSubmission.setScoreEarned(event.getScore());
        existingSubmission.setIsCorrect(event.isCorrect());
        existingSubmission.setEvaluatedAt(LocalDateTime.now());

        try {
            SubmissionStatus status = SubmissionStatus.valueOf(event.getStatus());
            existingSubmission.setStatus(status);
        } catch (Exception e) {
            log.warn("Invalid status '{}' from event. Defaulting to COMPLETED.", event.getStatus());
            existingSubmission.setStatus(SubmissionStatus.COMPLETED);
        }

        if (event.getDetailedFeedback() != null && !event.getDetailedFeedback().trim().isEmpty()) {
            try {
                SubmissionFeedback detailedFeedback = objectMapper.readValue(
                        event.getDetailedFeedback(),
                        SubmissionFeedback.class
                );
                existingSubmission.setFeedback(detailedFeedback);

            } catch (Exception e) {
                log.warn("Failed to parse detailed feedback JSON, falling back to basic: {}", e.getMessage());
                if ("CODING".equals(event.getFeedbackType())) {
                    existingSubmission.setFeedback(fallbackMapper.createBasicCodingFeedback(event));
                } else if ("ESSAY".equals(event.getFeedbackType())) {
                    existingSubmission.setFeedback(fallbackMapper.createBasicEssayFeedback(event));
                }
            }
        }
        else {
            log.warn("No detailed feedback found for event, delegating to fallback mapper.");
            if ("CODING".equals(event.getFeedbackType())) {
                existingSubmission.setFeedback(fallbackMapper.createBasicCodingFeedback(event));
            } else if ("ESSAY".equals(event.getFeedbackType())) {
                existingSubmission.setFeedback(fallbackMapper.createBasicEssayFeedback(event));
            }
        }

        TaskSubmission updatedSubmission = submissionRepository.save(existingSubmission);
        log.info("Submission {} updated with feedback and results.", updatedSubmission.getId());

        publishTaskCompletionEvent(updatedSubmission, event);
    }

    /**
     * Publishes a task completion event to analytics services after submission evaluation.
     * <p>
     * This method constructs a {@link TaskCompletedEvent} containing comprehensive
     * information about task completion and publishes it for consumption by analytics services.
     * The event is validated before publication to ensure data integrity.
     *
     * @param submission The updated {@link TaskSubmission}.
     * @param event The {@link SubmissionEvaluatedEvent} with evaluation results.
     */
    private void publishTaskCompletionEvent(TaskSubmission submission, SubmissionEvaluatedEvent event) {
        try {
            Task task = submission.getTask();
            UUID skillId = task.getTaskDefinition().getSkill().getId();
            Integer totalXpEarned = task.getXpReward();

            TaskCompletedEvent taskCompletedEvent = taskCompletionMapper.toTaskCompletedEvent(
                    submission,
                    task,
                    event,
                    skillId,
                    totalXpEarned
            );

            if (!validator.isValid(taskCompletedEvent)) {
                log.warn("TaskCompletedEvent validation failed for submission: {}. Errors: {}",
                        submission.getId(), validator.validate(taskCompletedEvent));
            }

            eventProducer.publishTaskCompleted(taskCompletedEvent);

            log.info("Task completion event published for user: {} and task: {}",
                    submission.getUserId(), task.getId());

        } catch (Exception e) {
            log.error("Failed to publish task completion event for submission: {}. Error: {}",
                    submission.getId(), e.getMessage());
        }
    }

    /**
     * Retrieves a submission record by its unique identifier and converts it to a DTO.
     * <p>
     * This method is a read-only transaction, primarily used by the REST controller
     * to fulfill user requests for checking the status or final result of a submission.
     *
     * @param submissionId The UUID of the submission to retrieve.
     * @return The retrieved {@link TaskSubmissionDTO}.
     * @throws ResourceNotFoundException if the submission ID is not found.
     */
    @Override
    @Transactional(readOnly = true)
    public TaskSubmissionDTO getSubmissionById(UUID submissionId) {
        log.info("Fetching submission by ID: {}", submissionId);

        TaskSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found: " + submissionId));

        return submissionMapper.toDTO(submission);
    }

    /**
     * Creates basic essay feedback when detailed feedback is not available.
     */
    private EssaySubmissionFeedback createBasicEssayFeedback(SubmissionEvaluatedEvent event) {
        EssaySubmissionFeedback essayFeedback = new EssaySubmissionFeedback();

        essayFeedback.setGrammarScore(0.0);
        essayFeedback.setRelevanceScore(event.getScore() / 100.0);
        essayFeedback.setToneAnalysis("Essay evaluation completed");
        essayFeedback.setSuggestions(List.of());

        return essayFeedback;
    }
}