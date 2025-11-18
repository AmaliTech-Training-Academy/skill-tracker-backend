package com.amalitech.task.service.dto.client.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * CLIENT DTO we get FROM Judge0 (POST /submissions?wait=true).
 * This is the raw execution result.
 */
@Data
public class Judge0SubmissionResponse {
    private String stdout;
    private String stderr;

    @JsonProperty("compile_output")
    private String compileOutput;

    private String message;
    private Double time;
    private Integer memory;
    private Judge0Status status;

    @Data
    public static class Judge0Status {
        private int id;
        /**
         * e.g., "Accepted", "Wrong Answer", "Time Limit Exceeded", "Compilation Error"
         */
        private String description;
    }
}
