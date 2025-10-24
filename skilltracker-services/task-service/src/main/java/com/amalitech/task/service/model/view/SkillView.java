package com.amalitech.task.service.model.view;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

/**
 * Read-only view entity representing skill information.
 *
 * <p>This entity is mapped to the {@code skill_view} database view, which provides
 * a lightweight, denormalized representation of skill data optimized for read
 * operations. It is typically used in scenarios where only basic skill information
 * is needed without loading full entity relationships.</p>
 *
 * <p><b>Usage Context:</b></p>
 * <ul>
 *   <li>Task generation processes that need skill metadata</li>
 *   <li>Quick skill lookups by name or ID</li>
 *   <li>Display purposes in user interfaces</li>
 *   <li>Performance-critical read operations where full entity graphs are unnecessary</li>
 * </ul>
 *
 * <p><b>Note:</b> As a view entity, this class should be treated as read-only.
 * Modifications to skill data should be performed through the primary skill entity
 * and its associated service layer.</p>
 *
 */

@Entity
@Table(name = "skill_view")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkillView {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    private String description;
}

