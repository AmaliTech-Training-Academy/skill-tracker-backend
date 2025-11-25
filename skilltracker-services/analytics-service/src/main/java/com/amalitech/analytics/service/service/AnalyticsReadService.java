package com.amalitech.analytics.service.service;

import com.amalitech.analytics.service.dto.*;
import com.amalitech.analytics.service.exception.EntityNotFoundException;
import com.amalitech.analytics.service.model.*;
import com.amalitech.analytics.service.model.enums.GoalStatus;
import com.amalitech.analytics.service.model.enums.Granularity;
import com.amalitech.analytics.service.repository.*;
import com.amalitech.analytics.service.service.interfaces.AnalyticsReadServiceInterface;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static com.amalitech.analytics.service.service.DashboardMaterializer.MAPPER;

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

    private static final Logger log = LoggerFactory.getLogger(AnalyticsReadService.class);


    @Value("${analytics.score.low-rubric-threshold}")
    private double lowRubricScoreThreshold;
    private final UserAggregateStatsRepository aggregateStatsRepository;
    private final UserSkillProgressRepository skillProgressRepository;
    private final SkillSnapshotRepository skillSnapshotRepository;
    private final JdbcTemplate jdbcTemplate;
    private final TaskSubmissionLogRepository logRepository;
    private final UserGoalRepository goalRepository;
    private final UserSkillSelectionRepository selectionRepository;

    /** {@inheritDoc} */
    @Override
    public DashboardDTO buildDashboard(UUID userId) {
        String sql = "SELECT data FROM user_dashboard_materialized WHERE user_id = ?";

        try {
            String json = jdbcTemplate.queryForObject(sql, String.class, userId);
            return MAPPER.readValue(json, DashboardDTO.class);
        } catch (EmptyResultDataAccessException e) {
            log.info("First time for user {} — building from scratch", userId);
            return buildFromScratch(userId);
        } catch (Exception e) {
            log.warn("Materialized data corrupt for user {} — rebuilding", userId, e);
            return buildFromScratch(userId);
        }
    }


    protected DashboardDTO buildFromScratch(UUID userId) {
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
                .map(progress -> mapToSkillProgressDTO(userId ,progress, snapshotMap.get(progress.getSkillId())))
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
    private SkillProgressDTO mapToSkillProgressDTO(UUID userId, UserSkillProgress progress, SkillSnapShot snapshot) {
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
        String levelBasedOnXp = details.getCurrentLevel(currentXp);
        UserSkillSelection userSkill = selectionRepository.findByUserIdAndSkillId(userId, snapshot.getId());
        String initialClaimLevel = userSkill.getInitialClaimLevel();
        String currentLevel = getEffectiveLevel(initialClaimLevel, levelBasedOnXp);
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
                progress.getTasksSubmitted(),
                progress.getTasksFailed(),
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

    /**
     * Determines the final level shown to the user.
     * It ensures the user's initial claim is honored until XP naturally surpasses it.
     */
    private String getEffectiveLevel(String initialClaim, String calculatedLevel) {
        if (initialClaim == null) {
            return calculatedLevel;
        }

        int calculatedRank = getLevelRank(calculatedLevel);
        int claimedRank = getLevelRank(initialClaim);

        if (claimedRank > calculatedRank) {
            return initialClaim;
        }

        return calculatedLevel;
    }

    private int getLevelRank(String level) {
        return switch (level.toUpperCase()) {
            case "BEGINNER" -> 1;
            case "INTERMEDIATE" -> 2;
            case "ADVANCED" -> 3;
            default -> 0;
        };
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
                    SELECT 
                        rubric,
                        (total_score::numeric / NULLIF(total_max_score, 0)) * 100 AS score_pct
                    FROM user_rubric_stats
                    WHERE user_id = ? 
                      AND total_max_score > 0 
                      AND (total_score::numeric / NULLIF(total_max_score, 0)) * 100 < ?
                    ORDER BY score_pct ASC
                    LIMIT 10
                    """;

        return jdbcTemplate.query(sql, (rs, __) -> new SkillGapDTO(
                rs.getString("rubric"),
                rs.getDouble("score_pct"),
                String.format("Weak in %s, (Score: %.1f%%)",
                rs.getString("rubric"), rs.getDouble("score_pct"))
        ), userId, lowRubricScoreThreshold);
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
