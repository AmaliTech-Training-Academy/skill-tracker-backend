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

        //existingSubmission.setIsCorrect(event.);
        existingSubmission.setScoreEarned(event.getScore());
        existingSubmission.setEvaluatedAt(LocalDateTime.now());

        try {
            SubmissionStatus status = SubmissionStatus.valueOf(event.getStatus());
            existingSubmission.setStatus(status);
        } catch (Exception e) {
            log.warn("Invalid status '{}' from event. Defaulting to COMPLETED.", event.getStatus());
            existingSubmission.setStatus(SubmissionStatus.COMPLETED);
        }

        TaskSubmission updatedSubmission = submissionRepository.save(existingSubmission);

        // We can publish *another* event, e.g., "submission.processed"
        // for a WebSocket service to pick up.
        // eventProducer.publishSubmissionProcessed(submissionMapper.toDTO(updatedSubmission));

        log.info("Submission {} updated from event.", updatedSubmission.getId());
    }
}