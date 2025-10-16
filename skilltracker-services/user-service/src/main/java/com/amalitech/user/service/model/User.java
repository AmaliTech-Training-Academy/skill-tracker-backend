package com.amalitech.user.service.model;

import com.amalitech.user.service.model.enums.PremiumTier;
import com.amalitech.user.service.model.enums.Role;
import com.amalitech.user.service.model.enums.UserState;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.util.UUID;
import java.time.LocalDateTime;

/**
 * The core entity representing a user in the system.
 * This class holds essential authentication, authorization, and state information
 * for a platform user, such as ID, email, hashed password, role, and current state.
 *
 * <p>It is mapped to the database table named "users".
 *
 * <p>This entity is the primary side for several relationships:
 * <ul>
 * <li>One-to-one with {@link UserProfile} (via {@code mappedBy}).</li>
 * <li>One-to-many relationship with {@link UserSkill} and {@link UserGroupMember} (not explicitly mapped here, but referenced in other models).</li>
 * <li>Many-to-one relationship from {@link UserGroup} (as the creator).</li>
 * </ul>
 *
 * @see UserProfile
 * @see Role
 * @see UserState
 * @see PremiumTier
 */
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_email", columnList = "email", unique = true),
        @Index(name = "idx_state_premium", columnList = "state, premium_tier")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(unique = true, nullable = false)
    private UUID id;

    @NotBlank
    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = true, unique = true, length = 255)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserState state = UserState.REGISTERED;

    @Enumerated(EnumType.STRING)
    @Column(name = "premium_tier", nullable = false)
    private PremiumTier premiumTier = PremiumTier.FREE;

    @Column(length = 10)
    private String language = "en";

    @Column(length = 50)
    private String timezone = "UTC";

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private UserProfile userProfile;
}
