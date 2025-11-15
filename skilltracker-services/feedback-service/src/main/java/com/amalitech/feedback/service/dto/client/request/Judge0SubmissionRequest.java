package com.amalitech.feedback.service.dto.client.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

/**
 * CLIENT DTO we send TO Judge0 (POST /submissions?wait=true).
 */
@Data
@Builder
public class Judge0SubmissionRequest {

    /**
     * The language ID (e.g., 71 for Python, 62 for Java).
     * Must be a positive integer.
     */
    @JsonProperty("language_id")
    @NotNull(message = "Language ID must be provided")
    @Min(value = 1, message = "Language ID must be a positive integer")
    private Integer languageId;

    /**
     * The source code to execute.
     * Cannot be blank.
     */
    @JsonProperty("source_code")
    @NotBlank(message = "Source code must not be blank")
    private String sourceCode;

    /**
     * The input for the test case.
     * Can be empty, but cannot be null.
     */
    @JsonProperty("stdin")
    @NotNull(message = "Stdin must not be null (use empty string if no input)")
    private String stdin;

    /**
     * The expected output for the test case.
     * Can be empty, but cannot be null.
     */
    @JsonProperty("expected_output")
    private String expectedOutput;
}
