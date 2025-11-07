package com.amalitech.task.service.model.content.impl;

import com.amalitech.task.service.model.content.TaskContent;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Defines the structure for essay-based tasks.
 * Includes the essay prompt, detailed instructions, evaluation criteria, and grading rubric.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EssayTaskContent implements TaskContent {

    @NotBlank(message = "Essay prompt cannot be blank")
    private String prompt;

    @NotBlank(message = "Detailed instructions cannot be blank")
    private String detailedInstructions;

    @NotNull(message = "Evaluation criteria cannot be null")
    private EvaluationCriteria evaluationCriteria;

    @NotNull(message = "Rubric cannot be null")
    private Rubric rubric;

    @NotNull(message = "Hints cannot be empty")
    @Size(min = 1, message = "At least one hint is required")
    private List<@NotBlank(message = "Hint cannot be blank") String> hints;

    @NotBlank(message = "Expected length cannot be blank")
    private String expectedLength;

    /**
     * Structured evaluation criteria for essay assessment.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EvaluationCriteria {
        @NotNull
        @Size(min = 3, message = "At least 3 completeness criteria required")
        private List<@NotBlank String> completeness;

        @NotNull
        @Size(min = 3, message = "At least 3 accuracy criteria required")
        private List<@NotBlank String> accuracy;

        @NotNull
        @Size(min = 3, message = "At least 3 clarity criteria required")
        private List<@NotBlank String> clarity;

        @NotNull
        @Size(min = 3, message = "At least 3 depth criteria required")
        private List<@NotBlank String> depth;
    }

    /**
     * Performance level descriptions for each evaluation category.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Rubric {
        @NotNull
        private PerformanceLevels completeness;

        @NotNull
        private PerformanceLevels accuracy;

        @NotNull
        private PerformanceLevels clarity;

        @NotNull
        private PerformanceLevels depth;

        /**
         * Performance levels for a single evaluation category.
         */
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public static class PerformanceLevels {
            @NotBlank(message = "Excellent description required")
            private String excellent;

            @NotBlank(message = "Good description required")
            private String good;

            @NotBlank(message = "Satisfactory description required")
            private String satisfactory;

            @NotBlank(message = "Needs improvement description required")
            private String needsImprovement;
        }
    }
}
