package com.amalitech.analytics.service.service.interfaces;

import com.amalitech.analytics.service.dto.DashboardDTO;
import com.amalitech.analytics.service.dto.TrajectoryPointDTO;
import com.amalitech.analytics.service.model.enums.Granularity;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Defines a read-only analytics service contract for retrieving
 * user dashboards, streak history, and skill trajectories.
 *
 * <p>This interface separates read logic from write/update flows,
 * allowing independent scaling and mocking in unit tests.</p>
 *
 * @since 1.0
 */
public interface AnalyticsReadServiceInterface {

    /**
     * Builds a comprehensive dashboard for the specified user.
     *
     * @param userId the user’s unique identifier
     * @return fully populated {@link DashboardDTO}
     */
    DashboardDTO buildDashboard(UUID userId);

    /**
     * Retrieves skill trajectory (average XP and tasks completed)
     * over time, using the specified granularity.
     *
     * @param userId      the user ID
     * @param skillId     the skill ID
     * @param granularity aggregation level (daily, weekly, monthly)
     * @return ordered list of {@link TrajectoryPointDTO}
     */
    List<TrajectoryPointDTO> getSkillTrajectory(UUID userId, UUID skillId, Granularity granularity);

    /**
     * Returns all dates within the user’s current practice streak.
     *
     * @param userId the user ID
     * @return ordered list of consecutive practice dates
     */
    List<LocalDate> getCurrentStreakDays(UUID userId);

    /**
     * Returns all unique practice dates for a user, typically used for
     * calendar visualizations.
     *
     * @param userId the user ID
     * @return unordered list of practice dates
     */
    List<LocalDate> getAllPracticeDays(UUID userId);
}
