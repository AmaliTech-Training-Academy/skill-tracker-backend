package com.amalitech.analytics.service.service;

import com.amalitech.analytics.service.dto.*;
import com.amalitech.analytics.service.model.SkillSnapShot;
import com.amalitech.analytics.service.model.UserSkillProgress;
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

    private final UserAggregateStatsRepository aggregateStatsRepository;
    private final UserSkillProgressRepository skillProgressRepository;
    private final SkillSnapshotRepository skillSnapshotRepository;
    private final JdbcTemplate jdbcTemplate;
    private final TaskSubmissionLogRepository logRepository;

    /** {@inheritDoc} */
    @Override
    public DashboardDTO buildDashboard(UUID userId) {
        UserStatsDTO userStats = getUserStats(userId);
        List<SkillProgressDTO> skillProgress = getSkillProgress(userId);
        List<GoalStatusDTO> goalStatus = List.of();
        List<SkillGapDTO> skillGaps = List.of();
        GlobalRankDTO globalRank = null;

        return new DashboardDTO(userStats, skillProgress, goalStatus, skillGaps, globalRank);
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
                throw new IllegalStateException("Skill snapshot not found: " + progress.getSkillId());
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
