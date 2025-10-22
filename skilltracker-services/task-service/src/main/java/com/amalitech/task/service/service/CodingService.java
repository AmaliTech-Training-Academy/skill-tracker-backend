//package com.amalitech.task.service.service;
//
//import com.amalitech.task.service.dto.SubmissionResultDTO;
//import com.amalitech.task.service.dto.SubmitAnswerRequest;
//import com.amalitech.task.service.exception.InvalidTaskTypeException;
//import com.amalitech.task.service.exception.ResourceNotFoundException;
//import com.amalitech.task.service.model.Task;
//import com.amalitech.task.service.model.TaskSubmission;
//import com.amalitech.task.service.model.TaskTestCase;
//import com.amalitech.task.service.model.enums.TaskType;
//import com.amalitech.task.service.model.feedback.impl.CodingSubmissionFeedback;
//import com.amalitech.task.service.model.submission.impl.CodingSubmissionAnswer;
//import com.amalitech.task.service.repository.TaskRepository;
//import com.amalitech.task.service.repository.TaskSubmissionRepository;
//import com.amalitech.task.service.repository.TaskTestCaseRepository;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.UUID;
//import java.util.stream.Collectors;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class CodingService {
//
//    private final TaskRepository taskRepository;
//    private final TaskSubmissionRepository submissionRepository;
//    private final TaskTestCaseRepository testCaseRepository;
//    private final CodeExecutionService codeExecutionService;
//
//    @Transactional
//    public SubmissionResultDTO submitCodingAnswer(UUID userId, SubmitAnswerRequest request) {
//        log.info("Processing coding submission for user: {}, task: {}", userId, request.getTaskId());
//
//        Task task = taskRepository.findById(request.getTaskId())
//                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
//
//        if (task.getType() != TaskType.CODING) {
//            throw new InvalidTaskTypeException("Task is not a coding challenge");
//        }
//
//        CodingSubmissionAnswer answer = (CodingSubmissionAnswer) request.getAnswer();
//
//        // Get all test cases
//        List<TaskTestCase> testCases = testCaseRepository
//                .findByTaskIdOrderByVisibility(task.getId());
//
//        // Execute code against test cases
//        CodingSubmissionFeedback feedback = codeExecutionService
//                .executeCodeAgainstTestCases(answer.getCode(), answer.getProgrammingLanguageId(), testCases);
//
//        boolean allPassed = feedback.isAllPassed();
//
//        // Calculate score based on passed tests
//        int totalWeight = testCases.stream().mapToInt(TaskTestCase::getWeight).sum();
//        int earnedWeight = feedback.getTestCaseResults().stream()
//                .filter(CodingSubmissionFeedback.TestCaseResult::isPassed)
//                .mapToInt(r -> testCases.stream()
//                        .filter(tc -> tc.getId().equals(r.getTestCaseId()))
//                        .findFirst()
//                        .map(TaskTestCase::getWeight)
//                        .orElse(0))
//                .sum();
//
//        int score = totalWeight > 0 ? (int) ((double) earnedWeight / totalWeight * task.getXpReward()) : 0;
//
//        // Save submission
//        TaskSubmission submission = new TaskSubmission();
//        submission.setUserId(userId);
//        submission.setTask(task);
//        submission.setAnswer(answer);
//        submission.setIsCorrect(allPassed);
//        submission.setScoreEarned(score);
//        submission.setFeedback(feedback);
//        submission.setEvaluatedAt(LocalDateTime.now());
//
//        submission = submissionRepository.save(submission);
//
//        Integer totalXp = submissionRepository.getTotalScoreByUser(userId);
//
//        return SubmissionResultDTO.builder()
//                .submissionId(submission.getId())
//                .isCorrect(allPassed)
//                .scoreEarned(score)
//                .totalXp(totalXp)
//                .feedback(feedback)
//                .build();
//    }
//}