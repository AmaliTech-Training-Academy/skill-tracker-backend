package com.amalitech.task.service.model.content;

import com.amalitech.task.service.model.content.impl.CodingTaskContent;
import com.amalitech.task.service.model.content.impl.EssayTaskContent;
import com.amalitech.task.service.model.content.impl.McqTaskContent;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Base interface for all task content types.
 *
 * Each concrete implementation defines the structure
 * of a specific task type:
 * - McqTaskContent
 * - CodingTaskContent
 * - EssayTaskContent
 *
 * Stored in the 'content' JSON field of the Task entity.
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "contentType"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = McqTaskContent.class, name = "MULTIPLE_CHOICE"),
        @JsonSubTypes.Type(value = CodingTaskContent.class, name = "CODING"),
        @JsonSubTypes.Type(value = EssayTaskContent.class, name = "ESSAY")
})
public interface TaskContent { }
