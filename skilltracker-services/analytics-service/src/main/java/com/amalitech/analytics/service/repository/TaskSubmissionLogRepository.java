package com.amalitech.analytics.service.repository;

import com.amalitech.analytics.service.model.TaskSubmissionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;


public interface TaskSubmissionLogRepository extends JpaRepository<TaskSubmissionLog, UUID> {

    /**
     * Finds all unique days a user has completed a task.
     * Used by the AnalyticsReadService to calculate the Current Streak History.
     * The logic is pushed to the database for performance (SELECT DISTINCT DATE(...)).
     */
    @Query(value = "SELECT DISTINCT DATE(completed_at AT TIME ZONE 'UTC') FROM task_submission_logs WHERE user_id = ?1 ORDER BY 1 ASC", nativeQuery = true)
    List<LocalDate> findAllDistinctPracticeDaysByUserId(UUID userId);
}
