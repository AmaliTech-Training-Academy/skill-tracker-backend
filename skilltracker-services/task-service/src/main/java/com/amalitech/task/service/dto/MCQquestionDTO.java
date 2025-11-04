package com.amalitech.task.service.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MCQquestionDTO {
    private String question_text;
    private int question_duration;
    private List<String> possible_answers;
    private String hint;
    private String correct_answer_index;
    private String explanation;
}