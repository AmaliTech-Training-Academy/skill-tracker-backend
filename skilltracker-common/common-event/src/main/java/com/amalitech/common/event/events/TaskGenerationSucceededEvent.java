package com.amalitech.common.event.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

/**
 * Event published when the task-service successfully completes
 * the generation of tasks for a newly onboarded user.
 * This event triggers the completion of the onboarding saga.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskGenerationSucceededEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * The unique identifier of the user for whom
     * tasks were successfully generated.
     */
    private UUID userId;
}
