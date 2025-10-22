package com.amalitech.task.service.model.submission.impl;

import com.amalitech.task.service.model.submission.SubmissionAnswer;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class McqSubmissionAnswer implements SubmissionAnswer {
    @Min(0)
    private int selectedOption;
}
