package com.amalitech.task.service.dto.response;

import com.amalitech.task.service.model.enums.SubmissionStatus;

import java.util.UUID;

public record SubmissionResponse(UUID submissionId, SubmissionStatus status) {}
