package com.amalitech.task.service.model.feedback.impl;

import com.amalitech.task.service.model.feedback.SubmissionFeedback;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Concrete implementation of {@link SubmissionFeedback} specifically designed to store
 * the evaluation results and feedback for a Multiple-Choice Question (MCQ) task submission.
 * <p>
 * This feedback structure is optimized for rapid, binary evaluation results common
 * to selection-based tasks, providing immediate closure on correctness alongside a brief
 * explanation for learning purposes.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class McqSubmissionFeedback implements SubmissionFeedback {
    private boolean isCorrect;
    private int correctOption;
    private String explanation;
}