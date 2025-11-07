package com.amalitech.notification.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * WebSocket message sent when task generation completes for a user.
 * Notifies the frontend that new tasks are now available.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskGenerationMessage {

    /**
     * The user ID who requested task generation
     */
    private String userId;

    /**
     * Status of task generation ("COMPLETED" or "FAILED")
     */
    private String status;

    /**
     * Optional message describing what happened
     */
    private String message;

    /**
     * Timestamp when the generation completed
     */
    private LocalDateTime completedAt;

    /**
     * Number of new tasks generated (if applicable)
     */
    private Integer tasksGenerated;
}
