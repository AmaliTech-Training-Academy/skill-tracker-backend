package com.amalitech.task.service.repository;

import com.amalitech.task.service.model.view.UserView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserViewRepository extends JpaRepository<UserView, UUID> {

    /**
     * Find user by email
     */
    @Query("SELECT u FROM UserView u WHERE LOWER(u.email) = LOWER(:email)")
    Optional<UserView> findByEmail(String email);

    /**
     * Check if user exists by email
     */
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END " +
            "FROM UserView u WHERE LOWER(u.email) = LOWER(:email)")
    boolean existsByEmail(String email);
}