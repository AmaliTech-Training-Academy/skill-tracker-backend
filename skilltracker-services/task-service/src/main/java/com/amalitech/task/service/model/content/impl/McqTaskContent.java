package com.amalitech.task.service.model.content.impl;

import com.amalitech.task.service.model.content.TaskContent;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;
import java.util.UUID;

/**
 * Defines the structure for multiple-choice question (MCQ) tasks.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class McqTaskContent implements TaskContent {

    private UUID userId;
    private String question_number;
    private String question_title;
    private String question_description;
    private String type;
    private String question_text;
    private int question_duration;
    private String question_difficulty;
    private List<String> options;
    private String hint;
    private String correct_answer;
    private int xpReward;
    private String explanation;
}

//    @NotBlank(message = "Question cannot be blank")
//    private String question;
//
//    @NotNull(message = "Options cannot be null")
//    @Size(min = 2, message = "At least two options are required")
//    private List<@NotBlank(message = "Option text cannot be blank") String> options;
//
//    @Min(value = 0, message = "Correct option index must be 0 or higher")
//    private int correctOption; // index of the correct answer
//
//    @NotBlank(message = "Explanation cannot be blank")
//    private String explanation;