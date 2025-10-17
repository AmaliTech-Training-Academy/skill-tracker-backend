package com.amalitech.user.service.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents the detailed profile information for a {@link User} in the system.
 * This entity uses a **shared primary key** mapping (one-to-one with {@code @MapsId})
 * with the {@code User} entity, meaning the primary key of a {@code UserProfile}
 * is the same as the primary key of its corresponding {@code User}.
 *
 * <p>It is mapped to the database table named "user_profiles" and holds non-essential
 * or optional user details like name, avatar, bio, and notification preferences,
 * separating them from the core {@code User} entity (e.g., login credentials).
 *
 */
@Entity
@Table(name = "user_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile {
    @Id
    @Column(name = "user_id")
    private UUID userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "full_name", length = 255)
    private String fullName;

    @Column(name = "avatar_url", columnDefinition = "TEXT")
    private String avatarUrl;

    @Column(columnDefinition = "TEXT", length = 1000)
    private String bio;

    @Column(name = "email_notifications")
    private Boolean emailNotifications = true;

    @Column(name = "push_notifications")
    private Boolean pushNotifications = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
