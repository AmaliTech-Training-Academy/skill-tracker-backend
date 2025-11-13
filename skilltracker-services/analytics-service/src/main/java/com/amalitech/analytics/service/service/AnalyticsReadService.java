package com.amalitech.analytics.service.service;

import com.amalitech.analytics.service.dto.*;
import com.amalitech.analytics.service.exception.EntityNotFoundException;
import com.amalitech.analytics.service.model.SkillSnapShot;
import com.amalitech.analytics.service.model.UserAggregateStats;
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
     * Retrieves aggregate statistics for a user: total tasks, streaks, last practice.
     */
    private UserStatsDTO getUserStats(UUID userId) {
        return aggregateStatsRepository.findById(userId)
                .map(this::mapToUserStatsDTO)
                .orElse(new UserStatsDTO(0, 0, 0, null));
    }

    /** Maps entity to DTO. */
    private UserStatsDTO mapToUserStatsDTO(UserAggregateStats stats) {
        return new UserStatsDTO(
                stats.getTotalTasksCompleted(),
                stats.getCurrentStreakInDays(),
                stats.getLongestStreakInDays(),
                stats.getLastPracticeDate()
        );
    }


    /**
     * Retrieves skill progress for a user enriched with snapshot XP levels.
     */
    private List<SkillProgressDTO> getSkillProgress(UUID userId) {
        List<UserSkillProgress> progresses = skillProgressRepository.findAllByUserId(userId);
        if (progresses.isEmpty()) return List.of();

        Map<UUID, SkillSnapShot> snapshotMap = getSnapshotMap(progresses);

        return progresses.stream()
                .map(progress -> mapToSkillProgressDTO(progress, snapshotMap.get(progress.getSkillId())))
                .collect(Collectors.toList());
    }

    /** Fetches all snapshots for a list of progresses and maps by ID. */
    private Map<UUID, SkillSnapShot> getSnapshotMap(List<UserSkillProgress> progresses) {
        Set<UUID> skillIds = progresses.stream()
                .map(UserSkillProgress::getSkillId)
                .collect(Collectors.toSet());

        return skillSnapshotRepository.findAllById(skillIds)
                .stream()
                .collect(Collectors.toMap(SkillSnapShot::getId, s -> s));
    }

    /** Maps progress + snapshot into SkillProgressDTO. */
    private SkillProgressDTO mapToSkillProgressDTO(UserSkillProgress progress, SkillSnapShot snapshot) {
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
    }


    /** Retrieves all active goals for a user. */
    private List<GoalStatusDTO> getActiveGoalStatus(UUID userId) {
        List<UserGoal> activeGoals = goalRepository.findByUserIdAndStatus(userId, GoalStatus.ACTIVE);
        return activeGoals.stream()
                .map(this::mapToGoalStatusDTO)
                .collect(Collectors.toList());
    }

    /** Maps a goal entity to GoalStatusDTO. */
    private GoalStatusDTO mapToGoalStatusDTO(UserGoal goal) {
        String description = switch (goal.getGoalType()) {
            case TARGET_XP -> String.format("Reach %d XP in %s", goal.getTargetValue(), goal.getSkillName());
            case TASKS_COMPLETED -> String.format("Complete %d tasks in %s", goal.getTargetValue(), goal.getSkillName());
            case REACH_LEVEL -> String.format("Reach next level in %s", goal.getSkillName());
        };

        double progressPercentage = calculateGoalProgressPercentage(goal);
        String status = calculateGoalStatus(goal);

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
    }

    private double calculateGoalProgressPercentage(UserGoal goal) {
        int range = goal.getTargetValue() - goal.getInitialValue();
        int currentProgress = goal.getCurrentValue() - goal.getInitialValue();
        return (range > 0) ? Math.max(0, Math.min(100.0, ((double) currentProgress / range) * 100.0)) : 0.0;
    }

    private String calculateGoalStatus(UserGoal goal) {
        if (goal.getDeadline() != null && LocalDate.now().isAfter(goal.getDeadline())) {
            return "Overdue";
        }
        return "On Track";
    }


    /** Identifies skill gaps based on rubric scores. */
    private List<SkillGapDTO> getSkillGaps(UUID userId) {
        String sql = """
                SELECT key AS rubric,
                    CASE
                        WHEN SUM((value ->> 'maxScore')::numeric) = 0 THEN 0
                        ELSE (SUM((value ->> 'score')::numeric) / SUM((value ->> 'maxScore')::numeric)) * 100
                    END AS avg_score
                FROM task_submission_logs,
                     jsonb_each(rubrics) AS t(key, value)
                WHERE user_id = ?
                GROUP BY key
                HAVING
                    CASE
                        WHEN SUM((value ->> 'maxScore')::numeric) = 0 THEN 0
                        ELSE (SUM((value ->> 'score')::numeric) / SUM((value ->> 'maxScore')::numeric)) * 100
                    END < ?
                ORDER BY avg_score ASC
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


    /** Generates recommendations based on skill gaps. */
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


    /** {@inheritDoc} */
    @Override
    public List<TrajectoryPointDTO> getSkillTrajectory(UUID userId, UUID skillId, Granularity granularity) {
        String periodUnit = mapGranularityToPostgresUnit(granularity);
        String sql = String.format("""
        WITH PeriodDeltas AS (
            -- Step 1: Aggregate daily deltas into the requested period (day, week, month)
            SELECT
                DATE_TRUNC('%s', snapshot_date) AS period_start,
                SUM(tasks_completed_today) AS tasks_in_period,
                SUM(xp_earned_today) AS xp_in_period
            FROM
                skill_trajectory_snapshots
            WHERE
                user_id = ? AND skill_id = ?
            GROUP BY
                period_start
        ),
        CumulativeCalculation AS (
            -- Step 2: Use window functions to create running totals (cumulative trajectory)
            SELECT
                period_start,
                SUM(tasks_in_period) OVER (ORDER BY period_start) AS cumulative_tasks,
                SUM(xp_in_period) OVER (ORDER BY period_start) AS cumulative_xp
            FROM
                PeriodDeltas
        )
        -- Step 3: Select final points, calculating the lifetime average XP as of that date
        SELECT
            period_start::date,
            cumulative_tasks AS tasks_completed_up_to_date,
            (cumulative_xp / NULLIF(cumulative_tasks, 0)) AS average_xp_earned
        FROM
            CumulativeCalculation
        ORDER BY
            period_start ASC;
        """, periodUnit);

        return jdbcTemplate.query(sql,
                (rs, rowNum) -> new TrajectoryPointDTO(
                        rs.getDate("period_start").toLocalDate(),
                        rs.getDouble("average_xp_earned"),
                        rs.getInt("tasks_completed_up_to_date")
                ),
                userId, skillId);
    }


    /** Maps granularity enum to PostgreSQL DATE_TRUNC unit. */
    private String mapGranularityToPostgresUnit(Granularity granularity) {
        return switch (granularity) {
            case DAILY -> "day";
            case WEEKLY -> "week";
            case MONTHLY -> "month";
        };
    }


    /** {@inheritDoc} */
    @Override
    public List<LocalDate> getCurrentStreakDays(UUID userId) {
        List<LocalDate> allPracticeDays = logRepository.findAllDistinctPracticeDaysByUserId(userId);
        if (allPracticeDays.isEmpty()) return List.of();

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
