package com.amalitech.feedback.service.dto.client.submission.impl;

import com.amalitech.feedback.service.dto.client.submission.SubmissionAnswer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CodingSubmissionAnswer implements SubmissionAnswer {

    @NotBlank(message = "Code cannot be blank.")
    private String code;

    /**
     * The Judge0 Language ID.
     * This ID tells the code evaluator which runtime to use.
     * e.g., 71 = Python, 62 = Java, 63 = JavaScript
     * The client (frontend) is responsible for sending the correct ID.
     */
    @NotNull(message = "Language ID is required.")
    private Integer languageId;
}