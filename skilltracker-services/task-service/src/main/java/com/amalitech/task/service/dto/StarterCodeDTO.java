package com.amalitech.task.service.dto;

import lombok.Builder;

import java.util.UUID;

@Builder
public record StarterCodeDTO(
        UUID languageId,
        String languageName,
        String code
) { }