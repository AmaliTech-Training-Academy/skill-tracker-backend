package com.amalitech.user.service.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents the membership of a specific {@link User} in a specific {@link UserGroup}.
 * This entity acts as a **junction table** to manage the many-to-many relationship
 * between the {@code User} and {@code UserGroup} entities, while also storing
 * metadata like the time the user joined the group.
 *
 * <p>It is mapped to the database table named "user_group_members".
 *
 * <p>The composite unique index on {@code group_id} and {@code user_id} ensures
 * that a user can only be a member of a given group once.
 *
 * @see User
 * @see UserGroup
 */
@Entity
@Table(name = "user_group_members", indexes = {
        @Index(name = "idx_group_user", columnList = "group_id, user_id", unique = true),
        @Index(name = "idx_user_id", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserGroupMember {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "group_id", nullable = false)
    private UserGroup group;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @CreationTimestamp
    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;
}
