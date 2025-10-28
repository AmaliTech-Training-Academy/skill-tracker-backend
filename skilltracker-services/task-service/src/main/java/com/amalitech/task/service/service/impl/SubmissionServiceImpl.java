package com.amalitech.task.service.service.impl;

import com.amalitech.task.service.dto.request.SubmitAnswerRequest;
import com.amalitech.task.service.events.TaskEventProducer;
import com.amalitech.task.service.exception.ResourceNotFoundException;
import com.amalitech.task.service.model.Task;
import com.amalitech.task.service.model.TaskSubmission;
import com.amalitech.task.service.model.enums.SubmissionStatus;
import com.amalitech.task.service.repository.TaskRepository;
import com.amalitech.task.service.repository.TaskSubmissionRepository;
import com.amalitech.task.service.service.SubmissionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubmissionServiceImpl implements SubmissionService {

    private final TaskSubmissionRepository submissionRepository;
    private final TaskRepository taskRepository;
    private final TaskEventProducer eventProducer;

    @Override
    @Transactional
    public TaskSubmission createSubmission(SubmitAnswerRequest request, UUID userId) {
        log.info("Creating submission for user {} and task {}", userId, request.taskId());

        Task task = taskRepository.findById(request.taskId())
                .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + request.taskId()));

        TaskSubmission submission = new TaskSubmission();
        submission.setUserId(userId);
        submission.setTask(task);
        submission.setAnswer(request.answer());

        TaskSubmission savedSubmission = submissionRepository.save(submission);

        eventProducer.publishSubmissionCreated(savedSubmission);

        log.info("Submission {} created and event published.", savedSubmission.getId());
        return savedSubmission;
    }

    @Override
    @Transactional
    public TaskSubmission updateSubmission(UUID submissionId, TaskSubmission evaluationResult) {
        log.info("Updating submission {} with evaluation results.", submissionId);

        TaskSubmission existingSubmission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found: " + submissionId));

        existingSubmission.setFeedback(evaluationResult.getFeedback());
        existingSubmission.setIsCorrect(evaluationResult.getIsCorrect());
        existingSubmission.setScoreEarned(evaluationResult.getScoreEarned());
        existingSubmission.setEvaluatedAt(LocalDateTime.now());

        if (evaluationResult.getStatus() == SubmissionStatus.COMPLETED || evaluationResult.getStatus() == SubmissionStatus.ERROR) {
            existingSubmission.setStatus(evaluationResult.getStatus());
        } else {
            existingSubmission.setStatus(SubmissionStatus.COMPLETED);
            log.warn("Evaluation result for {} had an unexpected status ({}). Defaulting to COMPLETED.",
                    submissionId, evaluationResult.getStatus());
        }


        TaskSubmission updatedSubmission = submissionRepository.save(existingSubmission);

        eventProducer.publishSubmissionEvaluated(updatedSubmission);

        log.info("Submission {} updated and evaluated event published.", updatedSubmission.getId());
        return updatedSubmission;
    }
}