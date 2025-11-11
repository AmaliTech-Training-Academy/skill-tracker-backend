package com.amalitech.task.service.dto.request;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class McqRequestDTO {
    private UUID userId;
    private String interest;
    private String difficulty;
    private Integer no_of_questions;
}