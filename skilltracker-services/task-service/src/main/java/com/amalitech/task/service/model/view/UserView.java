package com.amalitech.task.service.model.view;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Read-only view entity representing user information.
 *
 * <p>This entity is mapped to the {@code user_view} database view, which provides
 * a simplified, denormalized representation of user data optimized for read-heavy
 * operations. It contains only essential user information without sensitive data
 * like passwords or authentication tokens.</p>
 *
 * <p><b>Usage Context:</b></p>
 * <ul>
 *   <li>User identification in task assignments and submissions</li>
 *   <li>Display of user information in application interfaces</li>
 *   <li>Permission checks and role-based access control</li>
 *   <li>Audit logs and activity tracking</li>
 *   <li>Performance-critical queries where full user entity graphs are unnecessary</li>
 * </ul>
 *
 * <p><b>Security Note:</b> This view intentionally excludes sensitive user data
 * such as passwords, tokens, and detailed authentication information. For operations
 * requiring such data, use the primary user entity through appropriate secure channels.</p>
 *
 * <p><b>Note:</b> As a view entity, this class should be treated as read-only.
 * User data modifications should be performed through the primary user entity
 * and its associated service layer with proper authentication and authorization.</p>
 *
 */
@Entity
@Table(name = "user_view")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserView {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    private String role; // e.g., USER, ADMIN
}

