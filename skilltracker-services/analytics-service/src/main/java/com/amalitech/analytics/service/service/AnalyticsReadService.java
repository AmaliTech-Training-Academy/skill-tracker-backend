package com.amalitech.analytics.service.service;

import com.amalitech.analytics.service.dto.*;
import com.amalitech.analytics.service.exception.EntityNotFoundException;
import com.amalitech.analytics.service.model.SkillSnapShot;
import com.amalitech.analytics.service.model.UserGoal;
import com.amalitech.analytics.service.model.UserSkillProgress;
import com.amalitech.analytics.service.model.enums.GoalStatus;
import com.amalitech.analytics.service.model.enums.Granularity;
import com.amalitech.analytics.service.repository.*;
import com.amalitech.analytics.service.service.interfaces.AnalyticsReadServiceInterface;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link AnalyticsReadServiceInterface}.
 *
 * <p>Provides optimized, read-only analytics queries using a blend of
 * JPA repositories and direct JDBC access for aggregation tasks.
 * Each method is transactional and read-only, ensuring data consistency
 * without incurring write-locking overhead.</p>
 *
 * <p><strong>Performance Notes:</strong></p>
 * <ul>
 *   <li>All repository methods are batch-optimized to avoid N + 1 issues.</li>
 *   <li>JDBC is used selectively for time-series and aggregated computations.</li>
 * </ul>
 *
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsReadService implements AnalyticsReadServiceInterface {

    private static final double LOW_RUBRIC_SCORE_THRESHOLD = 70.0;

    private final UserAggregateStatsRepository aggregateStatsRepository;
    private final UserSkillProgressRepository skillProgressRepository;
    private final SkillSnapshotRepository skillSnapshotRepository;
    private final JdbcTemplate jdbcTemplate;
    private final TaskSubmissionLogRepository logRepository;
    private final UserGoalRepository goalRepository;

    /** {@inheritDoc} */
    @Override
    public DashboardDTO buildDashboard(UUID userId) {
        UserStatsDTO userStats = getUserStats(userId);
        List<SkillProgressDTO> skillProgress = getSkillProgress(userId);
        List<GoalStatusDTO> goalStatus = getActiveGoalStatus(userId);
        List<SkillGapDTO> skillGaps = getSkillGaps(userId);
        List<RecommendationDTO> recommendations = getRecommendations(skillGaps);
        GlobalRankDTO globalRank = null;

        return new DashboardDTO(
                userStats,
                skillProgress,
                goalStatus,
                skillGaps,
                recommendations,
                globalRank
        );
    }

    /**
     * Retrieves aggregate user statistics including total tasks,
     * current streak, and longest streak.
     *
     * @param userId the user ID
     * @return {@link UserStatsDTO} representing summarized stats
     */
    private UserStatsDTO getUserStats(UUID userId) {
        return aggregateStatsRepository.findById(userId)
                .map(stats -> new UserStatsDTO(
                        stats.getTotalTasksCompleted(),
                        stats.getCurrentStreakInDays(),
                        stats.getLongestStreakInDays(),
                        stats.getLastPracticeDate()))
                .orElse(new UserStatsDTO(0, 0, 0, null));
    }

    /**
     * Loads all skill progress for the specified user and enriches
     * with corresponding snapshot data to compute XP thresholds and levels.
     *
     * @param userId the user ID
     * @return list of {@link SkillProgressDTO}
     */
    private List<SkillProgressDTO> getSkillProgress(UUID userId) {
        List<UserSkillProgress> progresses = skillProgressRepository.findAllByUserId(userId);
        if (progresses.isEmpty()) {
            return List.of();
        }

        Set<UUID> skillIds = progresses.stream()
                .map(UserSkillProgress::getSkillId)
                .collect(Collectors.toSet());

        Map<UUID, SkillSnapShot> snapshotMap = skillSnapshotRepository.findAllById(skillIds)
                .stream()
                .collect(Collectors.toMap(SkillSnapShot::getId, s -> s));

        return progresses.stream().map(progress -> {
            SkillSnapShot snapshot = snapshotMap.get(progress.getSkillId());
            if (snapshot == null) {
                throw new EntityNotFoundException("Skill not found", progress.getSkillId());
            }

            Map<String, Long> xpMap = snapshot.getLevelXpMap();
            SkillDetailsDTO details = new SkillDetailsDTO(
                    snapshot.getId(),
                    snapshot.getName(),
                    xpMap.getOrDefault("INTERMEDIATE", 0L).intValue(),
                    xpMap.getOrDefault("ADVANCED", 0L).intValue()
            );

            int currentXp = progress.getTotalXpEarned();
            String currentLevel = details.getCurrentLevel(currentXp);
            String nextLevel = details.getNextLevel(currentLevel);
            int xpForNextLevel = details.getXpForLevel(nextLevel);
            int xpForCurrentLevel = details.getXpForLevel(currentLevel);
            int xpToNextLevel = Math.max(0, xpForNextLevel - currentXp);
            int currentLevelTotalXp = Math.max(0, xpForNextLevel - xpForCurrentLevel);

            return new SkillProgressDTO(
                    progress.getSkillId(),
                    details.skillName(),
                    progress.getAverageXpEarned(),
                    progress.getProficiency(),
                    progress.getTasksCompleted(),
                    currentXp,
                    currentLevel,
                    nextLevel,
                    xpToNextLevel,
                    currentLevelTotalXp
            );
        }).collect(Collectors.toList());
    }

    /**
     * Fetches all active goals for the user and maps them to DTOs.
     */
    private List<GoalStatusDTO> getActiveGoalStatus(UUID userId) {
        List<UserGoal> activeGoals = goalRepository.findByUserIdAndStatus(userId, GoalStatus.ACTIVE);

        return activeGoals.stream().map(goal -> {
            String description = switch (goal.getGoalType()) {
                case TARGET_XP -> String.format("Reach %d XP in %s",
                        goal.getTargetValue(), goal.getSkillName());
                case TASKS_COMPLETED -> String.format("Complete %d tasks in %s",
                        goal.getTargetValue(), goal.getSkillName());
                case REACH_LEVEL -> String.format("Reach next level in %s",
                        goal.getSkillName());
            };

            double progressPercentage = 0.0;
            int range = goal.getTargetValue() - goal.getInitialValue();
            int currentProgress = goal.getCurrentValue() - goal.getInitialValue();
            if (range > 0) {
                progressPercentage = Math.max(0, Math.min(100.0, ((double) currentProgress / range) * 100.0));
            }

            // Determine status string
            String status;
            if (goal.getDeadline() != null && LocalDate.now().isAfter(goal.getDeadline())) {
                status = "Overdue";
            } else {
                status = "On Track";
            }

            return new GoalStatusDTO(
                    goal.getId(),
                    description,
                    goal.getGoalType(),
                    goal.getCurrentValue(),
                    goal.getTargetValue(),
                    goal.getInitialValue(),
                    progressPercentage,
                    goal.getDeadline(),
                    status
            );
        }).collect(Collectors.toList());
    }

    /**
     * Identifies skill gaps by querying rubric scores from submission logs.
     *
     * CORRECTED: Now calculates performance using SUM(score) / SUM(maxScore).
     */
    private List<SkillGapDTO> getSkillGaps(UUID userId) {
        String sql = """
                SELECT
                    key AS rubric,
                    -- Correct Calculation: SUM(Score) / SUM(MaxScore) * 100.
                    -- Uses CASE to prevent divide-by-zero errors.
                    CASE
                        -- Safety check: If total maxScore is zero, performance is 0%.
                        WHEN SUM((value ->> 'maxScore')::numeric) = 0 THEN 0
                        -- Otherwise, calculate (Total Score / Total Max Score) * 100
                        ELSE (SUM((value ->> 'score')::numeric) / SUM((value ->> 'maxScore')::numeric)) * 100
                    END AS avg_score
                FROM
                    task_submission_logs,
                    -- jsonb_each is used to break out the nested {score, maxScore, percentage} object
                    jsonb_each(rubrics) AS t(key, value)
                WHERE
                    user_id = ?
                GROUP BY
                    key
                HAVING
                    -- Apply the filter on the calculated average performance
                    CASE
                        WHEN SUM((value ->> 'maxScore')::numeric) = 0 THEN 0
                        ELSE (SUM((value ->> 'score')::numeric) / SUM((value ->> 'maxScore')::numeric)) * 100
                    END < ?
                ORDER BY
                    avg_score ASC
                """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new SkillGapDTO(
                        rs.getString("rubric"),
                        rs.getDouble("avg_score"),
                        String.format("Weak performance in %s (Avg: %.1f/100)",
                                rs.getString("rubric"), rs.getDouble("avg_score"))
                ),
                userId, LOW_RUBRIC_SCORE_THRESHOLD
        );
    }

    /**
     * Generates simple recommendations based on identified rubric gaps.
     */
    private List<RecommendationDTO> getRecommendations(List<SkillGapDTO> gaps) {
        if (gaps.isEmpty()) {
            return List.of(new RecommendationDTO("Keep up the great work! No specific gaps found.", null));
        }

        return gaps.stream()
                .map(gap -> new RecommendationDTO(
                        String.format("To improve, focus on '%s' in your next tasks.", gap.rubric()),
                        gap.rubric()
                ))
                .collect(Collectors.toList());
    }



    /**
     * Maps {@link Granularity} values to PostgreSQL {@code DATE_TRUNC} units.
     *
     * @param granularity desired aggregation level
     * @return SQL-compatible time unit string
     * @throws IllegalArgumentException if granularity is unsupported
     */
    private String mapGranularityToPostgresUnit(Granularity granularity) {
        return switch (granularity) {
            case DAILY -> "day";
            case WEEKLY -> "week";
            case MONTHLY -> "month";
        };
    }

    /** {@inheritDoc} */
    @Override
    public List<TrajectoryPointDTO> getSkillTrajectory(UUID userId, UUID skillId, Granularity granularity) {
        if (granularity == Granularity.DAILY) {
            String dailySql = """
                SELECT DISTINCT ON (snapshot_date::date)
                       snapshot_date::date AS period_start,
                       average_xp_earned,
                       tasks_completed_up_to_date
                FROM skill_trajectory_snapshots
                WHERE user_id = ? AND skill_id = ?
                ORDER BY snapshot_date::date, snapshot_date DESC
                """;

            return jdbcTemplate.query(
                    dailySql,
                    (rs, rowNum) -> new TrajectoryPointDTO(
                            rs.getDate("period_start").toLocalDate(),
                            rs.getDouble("average_xp_earned"),
                            rs.getInt("tasks_completed_up_to_date")
                    ),
                    userId, skillId
            );
        }

        String periodUnit = mapGranularityToPostgresUnit(granularity);
        String sql = String.format("""
                SELECT DISTINCT ON (DATE_TRUNC('%s', snapshot_date))
                       DATE_TRUNC('%s', snapshot_date) AS period_start,
                       average_xp_earned,
                       tasks_completed_up_to_date
                FROM skill_trajectory_snapshots
                WHERE user_id = ? AND skill_id = ?
                ORDER BY period_start, snapshot_date DESC
                """, periodUnit, periodUnit);

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new TrajectoryPointDTO(
                        rs.getDate("period_start").toLocalDate(),
                        rs.getDouble("average_xp_earned"),
                        rs.getInt("tasks_completed_up_to_date")
                ),
                userId, skillId
        );
    }

    /** {@inheritDoc} */
    @Override
    public List<LocalDate> getCurrentStreakDays(UUID userId) {
        List<LocalDate> allPracticeDays = logRepository.findAllDistinctPracticeDaysByUserId(userId);
        if (allPracticeDays.isEmpty()) {
            return List.of();
        }

        Set<LocalDate> practiced = new HashSet<>(allPracticeDays);
        List<LocalDate> streak = new LinkedList<>();
        LocalDate check = allPracticeDays.get(allPracticeDays.size() - 1);

        while (practiced.contains(check)) {
            streak.add(0, check);
            check = check.minusDays(1);
        }

        return streak;
    }

    /** {@inheritDoc} */
    @Override
    public List<LocalDate> getAllPracticeDays(UUID userId) {
        return logRepository.findAllDistinctPracticeDaysByUserId(userId);
    }
}
