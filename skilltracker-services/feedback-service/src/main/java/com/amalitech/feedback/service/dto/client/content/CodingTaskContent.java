package com.amalitech.feedback.service.dto.client.content;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents the content for a CODING task.
 * This is the object we will parse to get test cases.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodingTaskContent implements TaskContent {

    private String prompt;
    private List<Example> examples;
    private String constraints;
    private String submissionHarness;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Example {
        private String input;
        private String output;
    }
}
