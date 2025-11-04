package com.amalitech.task.service.dto.response;

import com.amalitech.task.service.dto.MCQquestionDTO;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class McqResponseDTO {
    public List<MCQquestionDTO> mcqQuestion;
}
