package com.amalitech.feedback.service.evaluator;

import com.amalitech.common.event.events.SubmissionCreatedEvent;
import com.amalitech.common.event.events.SubmissionEvaluatedEvent;
import com.amalitech.feedback.service.dto.client.content.McqTaskContent;
import com.amalitech.feedback.service.dto.client.submission.impl.McqSubmissionAnswer;
import com.amalitech.feedback.service.dto.client.submission.impl.McqSubmissionFeedback;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/**
 * Evaluator for MCQ (Multiple Choice Question) tasks.
 * Evaluates multiple answers by comparing each answer against the correct_answer index
 * and generates per-question feedback.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class MCQTaskEvaluator implements TaskEvaluator {

    private final ObjectMapper objectMapper;

    @Override
    public Mono<SubmissionEvaluatedEvent> evaluate(SubmissionCreatedEvent event) {
        log.info("Evaluating MCQ task submission: {}", event.getSubmissionId());

        try {
            McqSubmissionAnswer answer = parseAnswer(event.getContentToEvaluate());
            McqTaskContent taskContent = parseTaskContent(event.getTaskDescription());

            validateContent(taskContent, answer);

            List<McqSubmissionFeedback.QuestionFeedback> feedbacks = new ArrayList<>();
            int totalCorrect = 0;

            for (McqSubmissionAnswer.QuestionAnswer qa : answer.getAnswers()) {
                McqTaskContent.Question question = findQuestion(taskContent, qa.getQuestionNumber());

                int correctOption = question.getCorrect_answer();
                boolean isCorrect = qa.getSelectedOption() == correctOption;

                if (isCorrect) totalCorrect++;

                String explanation = question.getExplanation() != null
                        ? question.getExplanation()
                        : "No explanation available.";

                feedbacks.add(new McqSubmissionFeedback.QuestionFeedback(
                        qa.getQuestionNumber(),
                        isCorrect,
                        correctOption,
                        explanation
                ));
            }

            int totalQuestions = answer.getAnswers().size();
            double scorePercentage = (totalCorrect * 100.0) / totalQuestions;

            McqSubmissionFeedback feedback = new McqSubmissionFeedback(
                    feedbacks,
                    totalCorrect,
                    totalQuestions,
                    scorePercentage
            );

            String detailedFeedbackJson = serializeFeedback(feedback);

            return Mono.just(SubmissionEvaluatedEvent.builder()
                    .submissionId(event.getSubmissionId())
                    .userId(event.getUserId())
                    .status("COMPLETED")
                    .score((int) scorePercentage)
                    .isCorrect(totalCorrect == totalQuestions)
                    .feedbackType("MULTIPLE_CHOICE")
                    .detailedFeedback(detailedFeedbackJson)
                    .overallFeedback(String.format(
                            "You got %d out of %d questions correct (%.0f%%)",
                            totalCorrect, totalQuestions, scorePercentage
                    ))
                    .avgExecutionTimeMs(0.0)
                    .avgMemoryUsedKb(0)
                    .build());

        } catch (Exception e) {
            log.error("MCQ evaluation failed for submission {}: {}", event.getSubmissionId(), e.getMessage(), e);
            return buildFallbackEvent(event);
        }
    }

    @Override
    public String getTaskType() {
        return "MULTIPLE_CHOICE";
    }

    /**
     * Parses the submission answer JSON string.
     */
    private McqSubmissionAnswer parseAnswer(String answerJson) throws Exception {
        return objectMapper.readValue(answerJson, McqSubmissionAnswer.class);
    }

    /**
     * Parses the task content JSON string.
     */
    private McqTaskContent parseTaskContent(String contentJson) throws Exception {
        return objectMapper.readValue(contentJson, McqTaskContent.class);
    }

    /**
     * Validates that the MCQ content and answer structures are valid.
     */
    private void validateContent(McqTaskContent taskContent, McqSubmissionAnswer answer) {
        if (taskContent == null || taskContent.getQuestions() == null) {
            throw new IllegalArgumentException("Invalid MCQ task content");
        }

        if (answer == null || answer.getAnswers() == null) {
            throw new IllegalArgumentException("Invalid MCQ submission answer");
        }

        for (McqTaskContent.Question question : taskContent.getQuestions()) {
            int correctOption = question.getCorrect_answer();
            int optionCount = question.getOptions().size();

            if (correctOption < 0 || correctOption >= optionCount) {
                throw new IllegalArgumentException(
                        String.format(
                                "Invalid correct_answer index %d for question %s (valid range: 0-%d)",
                                correctOption,
                                question.getQuestion_number(),
                                optionCount - 1
                        )
                );
            }
        }
    }

    /**
     * Finds a question by its question_number.
     */
    private McqTaskContent.Question findQuestion(
            McqTaskContent taskContent,
            String questionNumber
    ) {
        return taskContent.getQuestions().stream()
                .filter(q -> q.getQuestion_number().equals(questionNumber))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Question not found: " + questionNumber
                ));
    }

    /**
     * Serializes the detailed MCQ feedback to JSON with polymorphic type info.
     */
    private String serializeFeedback(McqSubmissionFeedback feedback) throws Exception {
        java.util.Map<String, Object> polymorphicFeedback = new java.util.HashMap<>();
        polymorphicFeedback.put("feedbackType", "MULTIPLE_CHOICE");
        polymorphicFeedback.put("totalCorrect", feedback.getTotalCorrect());
        polymorphicFeedback.put("totalQuestions", feedback.getTotalQuestions());
        polymorphicFeedback.put("scorePercentage", feedback.getScorePercentage());
        polymorphicFeedback.put("feedbacks", feedback.getFeedbacks());

        return objectMapper.writeValueAsString(polymorphicFeedback);
    }

    /**
     * Builds a fallback event when evaluation fails.
     */
    private Mono<SubmissionEvaluatedEvent> buildFallbackEvent(SubmissionCreatedEvent event) {
        return Mono.just(SubmissionEvaluatedEvent.builder()
                .submissionId(event.getSubmissionId())
                .userId(event.getUserId())
                .status("ERROR")
                .score(0)
                .isCorrect(false)
                .feedbackType("MULTIPLE_CHOICE")
                .overallFeedback("MCQ evaluation failed. Please contact support.")
                .build());
    }
}
