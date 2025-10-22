package com.amalitech.task.service.model.submission.impl;

import com.amalitech.task.service.model.submission.SubmissionAnswer;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EssaySubmissionAnswer implements SubmissionAnswer {
    @NotBlank
    private String submissionText;
}
