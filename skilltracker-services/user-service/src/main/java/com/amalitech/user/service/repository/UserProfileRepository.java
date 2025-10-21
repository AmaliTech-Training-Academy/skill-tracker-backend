package com.amalitech.user.service.repository;

import com.amalitech.user.service.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing UserProfile entities.
 * Provides database access methods for user profile-related operations.
 *
 * <p>Note: UserProfile uses a shared primary key strategy with User,
 * so the primary key is the User's UUID.
 */
@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

    /**
     * Finds a user profile by the associated user's ID.
     *
     * <p>Since UserProfile uses @MapsId, this is equivalent to findById,
     * but provides semantic clarity.
     *
     * @param userId the UUID of the user
     * @return an Optional containing the user profile if found
     */
    Optional<UserProfile> findByUserId(UUID userId);

    /**
     * Finds a user profile by the associated user's email.
     *
     * @param email the email of the user
     * @return an Optional containing the user profile if found
     */
    Optional<UserProfile> findByUserEmail(String email);

    /**
     * Checks if a user profile exists for a given user ID.
     *
     * @param userId the UUID of the user
     * @return true if a user profile exists, false otherwise
     */
    boolean existsByUserId(UUID userId);

    /**
     * Deletes a user profile by the associated user's ID.
     *
     * @param userId the UUID of the user
     */
    void deleteByUserId(UUID userId);
}