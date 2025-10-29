package com.amalitech.feedback.service.dto.client;

import com.amalitech.common.event.events.SubmissionCreatedEvent;
import com.amalitech.feedback.service.dto.client.response.Judge0SubmissionResponse;

import java.util.List;

public record GradingData(SubmissionCreatedEvent event, List<Judge0SubmissionResponse> judge0Results) {}