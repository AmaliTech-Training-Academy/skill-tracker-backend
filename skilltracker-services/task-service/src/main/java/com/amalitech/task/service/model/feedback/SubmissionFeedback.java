package com.amalitech.task.service.model.feedback;

import com.amalitech.task.service.model.feedback.impl.CodingSubmissionFeedback;
import com.amalitech.task.service.model.feedback.impl.EssaySubmissionFeedback;
import com.amalitech.task.service.model.feedback.impl.McqSubmissionFeedback;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Base interface for all AI-generated feedback types.
 * Stored in the 'feedback' JSON field of TaskSubmission.
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "feedbackType"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = McqSubmissionFeedback.class, name = "MCQ"),
        @JsonSubTypes.Type(value = EssaySubmissionFeedback.class, name = "ESSAY"),
        @JsonSubTypes.Type(value = CodingSubmissionFeedback.class, name = "CODING")
})
public interface SubmissionFeedback {
}