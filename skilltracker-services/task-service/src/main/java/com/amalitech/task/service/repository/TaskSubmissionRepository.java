package com.amalitech.task.service.repository;

import com.amalitech.task.service.model.TaskSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public interface TaskSubmissionRepository extends JpaRepository<TaskSubmission, UUID>,
        JpaSpecificationExecutor<TaskSubmission> {

    /**
     * Find all submissions by user
     */
    List<TaskSubmission> findByUserIdOrderBySubmittedAtDesc(UUID userId);

    /**
     * Count submissions by user and correctness
     */
    @Query("SELECT COUNT(ts) FROM TaskSubmission ts " +
            "WHERE ts.userId = :userId AND ts.isCorrect = :isCorrect")
    Long countByUserIdAndIsCorrect(
            @Param("userId") UUID userId,
            @Param("isCorrect") Boolean isCorrect
    );

    /**
     * Count correct submissions by user
     */
    @Query("SELECT COUNT(ts) FROM TaskSubmission ts " +
            "WHERE ts.userId = :userId AND ts.isCorrect = true")
    Long countCorrectSubmissionsByUser(@Param("userId") UUID userId);

    /**
     * Get total score (XP) earned by user
     */
    @Query("SELECT COALESCE(SUM(ts.scoreEarned), 0) FROM TaskSubmission ts " +
            "WHERE ts.userId = :userId")
    Integer getTotalScoreByUser(@Param("userId") UUID userId);


    /**
     * Gets the set of Task IDs that the user has correctly submitted.
     * This is the single source of truth for "completion"
     */
    @Query("SELECT ts.task.id FROM TaskSubmission ts " +
            "WHERE ts.userId = :userId AND ts.isCorrect = true")
    Set<UUID> findCompletedTaskIdsByUser(@Param("userId") UUID userId);
}