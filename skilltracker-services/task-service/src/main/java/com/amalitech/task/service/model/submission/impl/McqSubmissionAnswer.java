package com.amalitech.task.service.model.submission.impl;

import com.amalitech.task.service.model.submission.SubmissionAnswer;

import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * Concrete implementation of the {@link SubmissionAnswer} interface, specifically designed
 * to capture a user's answer for a Multiple-Choice Question (MCQ) task.
 * <p>
 * This class serves as the clean data structure for receiving and validating the submission
 * payload via REST APIs. It enforces that the selected option index is a non-negative
 * integer, ensuring data integrity before the submission is passed to the evaluation service.
 */
@Data
public class McqSubmissionAnswer implements SubmissionAnswer {
    @Min(0)
    private int selectedOption;
}