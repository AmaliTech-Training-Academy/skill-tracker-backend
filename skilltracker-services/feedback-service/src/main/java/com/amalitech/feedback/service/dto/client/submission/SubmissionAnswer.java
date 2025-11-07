package com.amalitech.feedback.service.dto.client.submission;

import com.amalitech.feedback.service.dto.client.submission.impl.CodingSubmissionAnswer;
import com.amalitech.feedback.service.dto.client.submission.impl.EssaySubmissionAnswer;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Base interface for polymorphic submission answers.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "answerType")
@JsonSubTypes({
        @JsonSubTypes.Type(value = CodingSubmissionAnswer.class, name = "CODING"),
        @JsonSubTypes.Type(value = EssaySubmissionAnswer.class, name = "ESSAY")
})
public interface SubmissionAnswer {
}
