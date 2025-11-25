package com.amalitech.feedback.service.dto.client.content;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Base interface for all task content types.
 * Must match the task-service's TaskContent interface.
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "contentType"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = CodingTaskContent.class, name = "CODING"),
        @JsonSubTypes.Type(value = EssayTaskContent.class, name = "ESSAY")
})
public interface TaskContent {
}
