package com.amalitech.feedback.service.dto.client.content;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;

/**
 * Defines the structure for multiple-choice question (MCQ) tasks.
 * Stores an array of questions, each with their own options and evaluation criteria.
 * This DTO mirrors the task-service's McqTaskContent structure for deserialization.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class McqTaskContent implements TaskContent {
    private List<Question> questions;

    /**
     * Represents a single multiple-choice question within an MCQ task.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Question {
        private String question_number;
        private String question_title;
        private String question_description;
        private String question_text;
        private int question_duration;
        private String question_difficulty;
        private List<String> options;
        private String hint;
        private int correct_answer;  // Index of the correct option (0-based), not the string value
        private int xpReward;
        private String explanation;
    }
}
