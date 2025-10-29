package com.amalitech.task.service.model.submission.impl;

import com.amalitech.task.service.model.submission.SubmissionAnswer;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Concrete implementation of the {@link SubmissionAnswer} interface, specifically designed
 * to capture a user's long-form textual response for an Essay task submission.
 * <p>
 * This class serves as the clean data structure for receiving and validating the submission
 * payload via REST APIs. It ensures that the core submission text is present and non-empty
 * (via {@code @NotBlank}) before being passed to the evaluation service for NLP grading.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EssaySubmissionAnswer implements SubmissionAnswer {
    @NotBlank
    private String submissionText;
}