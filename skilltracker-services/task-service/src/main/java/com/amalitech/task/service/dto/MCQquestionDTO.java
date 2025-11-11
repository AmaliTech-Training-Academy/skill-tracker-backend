package com.amalitech.task.service.dto;

import com.amalitech.task.service.model.TaskDefinition;
import com.amalitech.task.service.model.content.TaskContent;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MCQquestionDTO {
    private UUID userId;
    private String question_number;
    private String question_title;
    private String question_description;
    private String question_type;
    private String question_text;
    private int question_duration;
    private String question_difficulty;
    private List<String> options;
    private String hint;
    private String correct_answer;
    private Integer xpReward;
    private String explanation;
}