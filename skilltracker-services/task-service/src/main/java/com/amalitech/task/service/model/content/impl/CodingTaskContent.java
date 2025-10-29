package com.amalitech.task.service.model.content.impl;

import com.amalitech.task.service.model.content.TaskContent;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

/**
 * Defines the structure for coding tasks.
 * Contains a coding prompt, sample input-output examples,
 * constraints, starter code, test cases, evaluation criteria, and hints.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodingTaskContent implements TaskContent {
    @NotBlank(message = "Coding prompt cannot be blank")
    private String prompt;

    @NotNull(message = "Examples cannot be null")
    @Size(min = 1, message = "At least one example is required")
    private List<@Valid Example> examples;

    @NotBlank(message = "Constraints cannot be blank")
    private String constraints;

    private String starterCode;

    private List<@Valid TestCase> testCases;

    private EvaluationCriteria evaluationCriteria;

    private List<String> hints;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Example {
        @NotBlank(message = "Example input cannot be blank")
        private String input;

        @NotBlank(message = "Example output cannot be blank")
        private String output;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TestCase {
        @NotBlank(message = "Test case input cannot be blank")
        private String input;

        @NotBlank(message = "Test case expected output cannot be blank")
        private String expectedOutput;

        private Boolean isHidden;

        private String description;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EvaluationCriteria {
        private List<String> correctness;
        private List<String> efficiency;
        private List<String> style;
    }
}