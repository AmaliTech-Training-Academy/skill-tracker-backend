package com.amalitech.task.service.dto.events;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

/**
 * An immutable Data Transfer Object (DTO) representing a high-level skill-related event
 * that is typically broadcast across the SkillBoost microservice landscape (e.g., via Kafka).
 * <p>
 * This record ensures data consistency and contract stability for asynchronous communication
 * between services like the Task Service, Analytics Service, and User Profile Service.
 * Being a {@code record} and implementing {@code Serializable} makes it ideal for
 * efficient message queue consumption.
 *
 * @param id The unique identifier for the specific event instance.
 * @param name A human-readable name of the skill or achievement related to the event (e.g., "Java Fundamentals").
 * @param description A brief explanation of the event or skill context.
 * @param eventType The specific type of the event (e.g., "SKILL_COMPLETED", "SKILL_STARTED", "SKILL_LEVEL_UP").
 * @param supportedTaskTypes List of task types this skill supports (e.g., ["CODING", "MULTIPLE_CHOICE", "ESSAY"]).
 */
public record SkillEventDTO(
        UUID id,
        String name,
        String description,
        String eventType,
        List<String> supportedTaskTypes
) implements Serializable {}