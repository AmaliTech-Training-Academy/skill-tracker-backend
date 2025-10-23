package com.amalitech.task.service.model.content.impl;

import com.amalitech.task.service.model.content.TaskContent;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

/**
 * Defines the structure for essay-based tasks.
 * Includes the essay topic, word limits, and writing guidelines.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EssayTaskContent implements TaskContent {

    @NotBlank(message = "Essay topic cannot be blank")
    private String topic;

    @Min(value = 50, message = "Minimum word count should be at least 50")
    private int minWords;

    @Max(value = 5000, message = "Maximum word count cannot exceed 5000")
    private int maxWords;

    @NotNull(message = "Guidelines cannot be empty")
    @Size(min = 1, message = "At least one guideline is required")
    private List<@NotBlank(message = "Guideline cannot be blank") String> guidelines;
}
