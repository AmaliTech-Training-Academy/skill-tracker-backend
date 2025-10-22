//package com.amalitech.task.service.service;
//
//import com.amalitech.task.service.dto.SubmissionResultDTO;
//import com.amalitech.task.service.dto.request.SubmitAnswerRequest;
//import com.amalitech.task.service.exception.InvalidTaskTypeException;
//import com.amalitech.task.service.exception.ResourceNotFoundException;
//import com.amalitech.task.service.model.Task;
//import com.amalitech.task.service.model.TaskSubmission;
//import com.amalitech.task.service.model.content.impl.EssayTaskContent;
//import com.amalitech.task.service.model.enums.TaskType;
//import com.amalitech.task.service.model.feedback.impl.EssaySubmissionFeedback;
//import com.amalitech.task.service.model.submission.impl.EssaySubmissionAnswer;
//import com.amalitech.task.service.repository.TaskRepository;
//import com.amalitech.task.service.repository.TaskSubmissionRepository;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.LocalDateTime;
//import java.util.ArrayList;
//import java.util.UUID;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class EssayService {
//
//    private final TaskRepository taskRepository;
//    private final TaskSubmissionRepository submissionRepository;
//    private final AiFeedbackService aiFeedbackService;
//
//    @Transactional
//    public SubmissionResultDTO submitEssayAnswer(UUID userId, SubmitAnswerRequest request) {
//        log.info("Processing essay submission for user: {}, task: {}", userId, request.getTaskId());
//
//        Task task = taskRepository.findById(request.getTaskId())
//                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
//
//        if (task.getType() != TaskType.ESSAY) {
//            throw new InvalidTaskTypeException("Task is not an essay");
//        }
//
//        EssayTaskContent essayContent = (EssayTaskContent) task.getContent();
//        EssaySubmissionAnswer answer = (EssaySubmissionAnswer) request.getAnswer();
//
//        String essayText = answer.getSubmissionText();
//
//        // Basic validation
//        int wordCount = countWords(essayText);
//        boolean meetsRequirements = wordCount >= essayContent.getMinWords()
//                && wordCount <= essayContent.getMaxWords();
//
//        // Generate AI feedback
//        EssaySubmissionFeedback feedback = aiFeedbackService.generateEssayFeedback(
//                essayContent.getTopic(),
//                essayText,
//                essayContent.getGuidelines()
//        );
//
//        // Calculate score
//        double wordCountPenalty = meetsRequirements ? 1.0 : 0.7;
//        int score = (int) (task.getXpReward() * wordCountPenalty);
//
//        // Save submission
//        TaskSubmission submission = new TaskSubmission();
//        submission.setUserId(userId);
//        submission.setTask(task);
//        submission.setAnswer(answer);
//        submission.setIsCorrect(meetsRequirements);
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
//                .isCorrect(meetsRequirements)
//                .scoreEarned(score)
//                .totalXp(totalXp)
//                .feedback(feedback)
//                .build();
//    }
//
//    private int countWords(String text) {
//        if (text == null || text.trim().isEmpty()) {
//            return 0;
//        }
//        return text.trim().split("\\s+").length;
//    }
//}
