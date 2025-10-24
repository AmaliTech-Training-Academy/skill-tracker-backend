package com.amalitech.task.service.model.submission.impl;

import com.amalitech.task.service.model.submission.SubmissionAnswer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CodingSubmissionAnswer implements SubmissionAnswer {

    @NotBlank
    private String code;

    /**
     * Coding language preference.
     * It tells the code evaluator which runtime to use.
     */
    @NotNull
    private UUID programmingLanguageId;
}
