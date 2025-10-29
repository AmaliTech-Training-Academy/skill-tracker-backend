package com.amalitech.task.service.service.impl;

import com.amalitech.common.event.events.SubmissionCreatedEvent;
import com.amalitech.common.event.events.SubmissionEvaluatedEvent;
import com.amalitech.task.service.dto.TaskSubmissionDTO;
import com.amalitech.task.service.dto.request.SubmitAnswerRequest;
import com.amalitech.task.service.events.EventProducer;
import com.amalitech.task.service.exception.ResourceNotFoundException;
import com.amalitech.task.service.mapper.SubmissionMapper;
import com.amalitech.task.service.model.Task;
import com.amalitech.task.service.model.TaskSubmission;
import com.amalitech.task.service.model.enums.SubmissionStatus;
import com.amalitech.task.service.model.feedback.impl.CodingSubmissionFeedback;
import com.amalitech.task.service.repository.TaskRepository;
import com.amalitech.task.service.repository.TaskSubmissionRepository;
import com.amalitech.task.service.service.SubmissionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of the {@link SubmissionService} interface, handling the business logic
 * for creating, updating, and retrieving user task submissions.
 * <p>
 * This service is responsible for the critical transactional logic of the Task Service:
 * persisting new submissions and integrating asynchronous evaluation results back into the
 * persistence layer. It serves as a command/query gateway for submission data.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubmissionServiceImpl implements SubmissionService {

    private final TaskSubmissionRepository submissionRepository;
    private final TaskRepository taskRepository;
    private final EventProducer eventProducer;
    private final SubmissionMapper submissionMapper;

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
     * {@link CodingSubmissionFeedback}) based on the event's payload. It is typically
     * called by a message listener.
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

        if ("CODING".equals(event.getFeedbackType())) {
            CodingSubmissionFeedback feedback = new CodingSubmissionFeedback();
            feedback.setAllPassed(event.isCorrect());

            int passedCount = event.getTestResults() != null
                    ? (int) event.getTestResults().stream().filter(SubmissionEvaluatedEvent.TestResultData::isPassed).count()
                    : 0;
            int totalCount = event.getTestResults() != null ? event.getTestResults().size() : 0;

            feedback.setTestCasesPassed(passedCount);
            feedback.setTestCasesTotal(totalCount);
            feedback.setStdout(event.getStdout());
            feedback.setStderr(event.getStderr());

            if (event.getTestResults() != null && !event.getTestResults().isEmpty()) {
                List<CodingSubmissionFeedback.TestCaseResult> testResults = event.getTestResults().stream()
                        .map(tr -> {
                            CodingSubmissionFeedback.TestCaseResult tcr = new CodingSubmissionFeedback.TestCaseResult();
                            tcr.setPassed(tr.isPassed());
                            tcr.setExpected(tr.getExpectedOutput());
                            tcr.setActual(tr.getActualOutput());
                            tcr.setExecutionTimeMs(tr.getExecutionTimeMs() != null ? tr.getExecutionTimeMs() : 0);
                            return tcr;
                        })
                        .collect(Collectors.toList());
                feedback.setTestCaseResults(testResults);
            }

            if (event.getOverallFeedback() != null) {
                feedback.setLintingReport(event.getOverallFeedback());
            }

            existingSubmission.setFeedback(feedback);
        }

        TaskSubmission updatedSubmission = submissionRepository.save(existingSubmission);

        log.info("Submission {} updated with feedback and results.", updatedSubmission.getId());
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
}