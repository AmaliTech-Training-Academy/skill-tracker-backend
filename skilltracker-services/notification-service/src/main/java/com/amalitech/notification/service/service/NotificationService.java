package com.amalitech.notification.service.service;

import com.amalitech.common.event.events.SubmissionEvaluatedEvent;
import com.amalitech.common.event.events.SubmissionExecutedEvent;
import com.amalitech.common.event.events.TaskGenerationFailedEvent;
import com.amalitech.common.event.events.TaskGenerationSucceededEvent;

public interface NotificationService {

    void sendExecutionResults(SubmissionExecutedEvent event);

    void sendEvaluationFeedback(SubmissionEvaluatedEvent event);

    void sendTaskGenerationSuccessNotification(TaskGenerationSucceededEvent event);

    void sendTaskGenerationFailedNotification(TaskGenerationFailedEvent event);
}
