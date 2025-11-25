package com.amalitech.task.service.validation;

import com.amalitech.task.service.model.content.impl.McqTaskContent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Validator for MCQ task content.
 * Ensures that all questions have valid structure with correct_answer indices
 * that reference valid options.
 */
@Component
@Slf4j
public class McqContentValidator {

    /**
     * Validates the entire MCQ content structure.
     *
     * @param content The MCQ task content to validate
     * @throws IllegalArgumentException if content is invalid
     */
    public void validateMcqContent(McqTaskContent content) {
        if (content == null) {
            throw new IllegalArgumentException("MCQ content cannot be null");
        }

        if (content.getQuestions() == null || content.getQuestions().isEmpty()) {
            throw new IllegalArgumentException("MCQ content must contain at least one question");
        }

        for (int i = 0; i < content.getQuestions().size(); i++) {
            McqTaskContent.Question question = content.getQuestions().get(i);
            validateQuestion(question, i);
        }

        log.debug("MCQ content validation passed for {} questions", content.getQuestions().size());
    }

    /**
     * Validates a single question structure.
     *
     * @param question The question to validate
     * @param index The index of the question in the questions list (for error reporting)
     * @throws IllegalArgumentException if the question is invalid
     */
    private void validateQuestion(McqTaskContent.Question question, int index) {
        if (question == null) {
            throw new IllegalArgumentException("Question at index " + index + " is null");
        }

        String questionNumber = question.getQuestion_number();

        if (questionNumber == null || questionNumber.isBlank()) {
            throw new IllegalArgumentException("Question at index " + index + " has no question_number");
        }

        if (question.getQuestion_text() == null || question.getQuestion_text().isBlank()) {
            throw new IllegalArgumentException("Question " + questionNumber + " has no question_text");
        }

        int correctAnswer = getCorrectAnswer(question, questionNumber);

        log.debug("Question {} validation passed (correct_answer index: {})", questionNumber, correctAnswer);
    }

    private static int getCorrectAnswer(McqTaskContent.Question question, String questionNumber) {
        List<String> options = question.getOptions();
        if (options == null || options.isEmpty()) {
            throw new IllegalArgumentException("Question " + questionNumber + " has no options");
        }

        for (int i = 0; i < options.size(); i++) {
            String option = options.get(i);
            if (option == null || option.isBlank()) {
                throw new IllegalArgumentException(
                        "Question " + questionNumber + " has empty option at index " + i
                );
            }
        }

        int correctAnswer = question.getCorrect_answer();
        if (correctAnswer < 0 || correctAnswer >= options.size()) {
            throw new IllegalArgumentException(
                    String.format(
                            "Question %s has invalid correct_answer index %d (valid range: 0-%d)",
                            questionNumber,
                            correctAnswer,
                            options.size() - 1
                    )
            );
        }
        return correctAnswer;
    }
}
