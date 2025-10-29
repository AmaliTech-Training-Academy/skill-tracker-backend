package com.amalitech.feedback.service.dto.client.request;

import com.fasterxml.jackson.annotation.JsonProperty;
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
     */
    @JsonProperty("language_id")
    private int languageId;

    @JsonProperty("source_code")
    private String sourceCode;

    /**
     * The input for the test case.
     */
    @JsonProperty("stdin")
    private String stdin;

    /**
     * The expected output for the test case.
     */
    @JsonProperty("expected_output")
    private String expectedOutput;
}
