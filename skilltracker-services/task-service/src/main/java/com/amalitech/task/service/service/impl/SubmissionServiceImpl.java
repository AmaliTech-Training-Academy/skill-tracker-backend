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

@Service
@RequiredArgsConstructor
@Slf4j
public class SubmissionServiceImpl implements SubmissionService {

    private final TaskSubmissionRepository submissionRepository;
    private final TaskRepository taskRepository;
    private final EventProducer eventProducer;
    private final SubmissionMapper submissionMapper;

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
            feedback.setTestCasesPassed(event.isCorrect() ? (event.getTestCaseResults() != null ? event.getTestCaseResults().size() : 0) : 0);
            feedback.setTestCasesTotal(event.getTestCaseResults() != null ? event.getTestCaseResults().size() : 0);
            
            if (event.getTestCaseResults() != null && !event.getTestCaseResults().isEmpty()) {
                List<CodingSubmissionFeedback.TestCaseResult> testResults = event.getTestCaseResults().stream()
                        .map(result -> {
                            CodingSubmissionFeedback.TestCaseResult tcr = new CodingSubmissionFeedback.TestCaseResult();
                            tcr.setPassed(result.contains("PASSED") || result.contains("Accepted"));
                            tcr.setExpected(result);
                            tcr.setActual(result);
                            return tcr;
                        })
                        .collect(Collectors.toList());
                feedback.setTestCaseResults(testResults);
            }
            
            feedback.setStdout(event.getOverallFeedback());
            existingSubmission.setFeedback(feedback);
        }

        TaskSubmission updatedSubmission = submissionRepository.save(existingSubmission);

        log.info("Submission {} updated with feedback and results.", updatedSubmission.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public TaskSubmissionDTO getSubmissionById(UUID submissionId) {
        log.info("Fetching submission by ID: {}", submissionId);

        TaskSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found: " + submissionId));

        return submissionMapper.toDTO(submission);
    }
}