package com.amalitech.task.service.model.submission;

import com.amalitech.task.service.model.submission.impl.CodingSubmissionAnswer;
import com.amalitech.task.service.model.submission.impl.EssaySubmissionAnswer;
import com.amalitech.task.service.model.submission.impl.McqSubmissionAnswer;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Base interface for all submission answer types.
 * Stored in the 'answer' JSON field of TaskSubmission.
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "answerType" // This 'answerType' field will be in the JSON
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = McqSubmissionAnswer.class, name = "MCQ"),
        @JsonSubTypes.Type(value = EssaySubmissionAnswer.class, name = "ESSAY"),
        @JsonSubTypes.Type(value = CodingSubmissionAnswer.class, name = "CODING")
})
public interface SubmissionAnswer {
}