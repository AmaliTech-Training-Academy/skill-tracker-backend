package com.amalitech.feedback.service.dto.client.submission;

import com.amalitech.feedback.service.dto.client.submission.impl.CodingSubmissionFeedback;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Base interface for polymorphic submission feedback.
 * This is what we will be BUILDING.
 * (Copied from task-service)
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "feedbackType")
@JsonSubTypes({
        @JsonSubTypes.Type(value = CodingSubmissionFeedback.class, name = "CODING")
        // Add other types as needed
})
public interface SubmissionFeedback {
}