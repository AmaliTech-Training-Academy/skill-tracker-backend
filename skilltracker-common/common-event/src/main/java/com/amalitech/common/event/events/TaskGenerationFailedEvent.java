package com.amalitech.common.event.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

/**
 * Event published when the task-service fails to generate
 * tasks for a newly onboarded user.
 * This event triggers a compensating transaction (rollback)
 * in the user-service to handle the onboarding failure.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskGenerationFailedEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * The unique identifier of the user for whom
     * task generation failed.
     */
    private UUID userId;

    /**
     * The error message or reason for the failure.
     * This is crucial for logging and debugging.
     */
    private String errorMessage;
}