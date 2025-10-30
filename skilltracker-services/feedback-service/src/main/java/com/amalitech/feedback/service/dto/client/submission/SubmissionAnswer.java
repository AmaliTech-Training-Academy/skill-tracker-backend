package com.amalitech.feedback.service.dto.client.submission;

import com.amalitech.feedback.service.dto.client.submission.impl.CodingSubmissionAnswer;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Base interface for polymorphic submission answers.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "answerType")
@JsonSubTypes({
        @JsonSubTypes.Type(value = CodingSubmissionAnswer.class, name = "CODING")
})
public interface SubmissionAnswer {
}
