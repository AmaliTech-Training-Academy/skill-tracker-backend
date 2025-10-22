package com.amalitech.task.service.model.feedback.impl;

import com.amalitech.task.service.model.feedback.SubmissionFeedback;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class McqSubmissionFeedback implements SubmissionFeedback {
    private boolean isCorrect;
    private int correctOption;
    private String explanation;
}