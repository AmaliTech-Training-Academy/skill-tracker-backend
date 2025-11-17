package com.amalitech.common.event.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
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

    /**
     * The list of skill IDs for which tasks were generated.
     * Must be non-null and populated with the actual skill IDs.
     */
    private List<UUID> skillIds;

    /**
     * The list of generated task IDs.
     * Used for tracking and notification purposes.
     * Must be non-null and populated with the IDs of all generated tasks.
     */
    private List<UUID> generatedTaskIds;
}
