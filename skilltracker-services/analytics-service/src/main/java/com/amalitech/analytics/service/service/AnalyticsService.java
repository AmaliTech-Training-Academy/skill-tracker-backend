package com.amalitech.analytics.service.service;

import com.amalitech.analytics.service.dto.RubricScoreDTO;
import com.amalitech.analytics.service.dto.TaskCompletedEvent;
import com.amalitech.analytics.service.dto.TaskSubmissionRequestDTO;
import com.amalitech.analytics.service.events.AnalyticsUpdateEvent;
import com.amalitech.analytics.service.events.GoalCompletedEvent;
import com.amalitech.analytics.service.exception.EntityNotFoundException;
import com.amalitech.analytics.service.model.*;
import com.amalitech.analytics.service.model.enums.GoalStatus;
import com.amalitech.analytics.service.repository.*;
import com.amalitech.analytics.service.service.interfaces.AnalyticsServiceInterface;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * Default implementation of {@link AnalyticsServiceInterface}.
 *
 * <p>This service handles all write and update operations for analytics,
 * including task completions, progress tracking, and aggregate updates.
 * It also publishes internal domain events to notify other layers (e.g., WebSocket push).</p>
 *
 * <p>All operations are transactional and ensure consistency across
 * progress tracking, trajectory snapshots, and aggregate statistics.</p>
 *
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService implements AnalyticsServiceInterface {

    private final TaskSubmissionLogRepository logRepository;
    private final UserSkillProgressRepository skillProgressRepository;
    private final UserAggregateStatsRepository aggregateStatsRepository;
    private final SkillTrajectorySnapshotRepository trajectoryRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final SkillSnapshotRepository skillSnapshotRepository;
    private final UserGoalRepository goalRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void processTaskCompletion(TaskCompletedEvent event) {
        log.info("Processing TaskCompletedEvent for user: {}", event.userId());

        UserSkillProgress progress = updateSkillProgress(event);
        updateUserAggregateStats(event);
        logSubmission(event);
        updateRubricAggregates(event);
        updateTrajectorySnapshot(event);
        updateGoalProgress(progress);

        log.debug("Publishing AnalyticsUpdateEvent for user: {}", event.userId());

        eventPublisher.publishEvent(new AnalyticsUpdateEvent(this, event.userId()));
    }

    @Override
    @Transactional
    public void submitTaskDirectly(TaskSubmissionRequestDTO request) {
        log.warn("DIRECT SUBMISSION API USED: Bypassing message queue for user {}", request.userId());
        processTaskCompletion(request.toEvent());
    }


    private UserSkillProgress updateSkillProgress(TaskCompletedEvent event) {
        UserSkillProgress progress = getSkillProgress(event.skillId(), event.userId());
        SkillSnapShot snapshot = fetchSkillSnapshot(event.skillId());
        int totalXp = progress.getTotalXpEarned() + event.totalXpEarned();
        double proficiency = calculateProficiency(totalXp, snapshot);
        progress.updateProgress(event, Math.min(proficiency, 100.0));
        return skillProgressRepository.save(progress);
    }

    private SkillSnapShot fetchSkillSnapshot(UUID skillId) {
        return skillSnapshotRepository.findById(skillId)
                .orElseThrow(() -> new EntityNotFoundException("Skill snapshot not found: ", skillId));
    }

    public double calculateProficiency(int currentXp, SkillSnapShot snapshot) {
        long intermediate = snapshot.getLevelXpMap().getOrDefault("INTERMEDIATE", 1000L);
        long advanced = snapshot.getLevelXpMap().getOrDefault("ADVANCED", 3000L);
        double proficiency;

        if (currentXp < intermediate) {
            proficiency = (double) currentXp / intermediate * 33.3;
        } else if (currentXp < advanced) {
            proficiency = 33.3 + ((double) (currentXp - intermediate) / (advanced - intermediate)) * 33.3;
        } else {
            long capstoneChunk = (advanced - intermediate > 0) ? (advanced - intermediate) : 2000L;
            proficiency = 66.6 + ((double) (currentXp - advanced) / capstoneChunk) * 33.4;
        }

        return proficiency;
    }

    private UserSkillProgress getSkillProgress(UUID skillId, UUID userId) {
        return skillProgressRepository
                .findByUserIdAndSkillId(userId, skillId)
                .orElseGet(() -> {
                    UserSkillProgress newProgress = new UserSkillProgress();
                    newProgress.setUserId(userId);
                    newProgress.setSkillId(skillId);
                    newProgress.setTasksCompleted(0);
                    newProgress.setTotalXpEarned(0);
                    newProgress.setAverageXpEarned(0.0);
                    newProgress.setProficiency(0.0);
                    return newProgress;
                });
    }


    private void updateGoalProgress(UserSkillProgress progress) {
        List<UserGoal> activeGoals = fetchActiveGoals(progress.getUserId(), progress.getSkillId());
        if (activeGoals.isEmpty()) return;

        log.debug("Found {} active goals for user {} and skill {}",
                activeGoals.size(), progress.getUserId(), progress.getSkillId());

        for (UserGoal goal : activeGoals) {
            boolean updated = updateSingleGoal(goal, progress);
            if (updated) handleGoalCompletion(goal);
        }

        goalRepository.saveAll(activeGoals);
    }

    private List<UserGoal> fetchActiveGoals(UUID userId, UUID skillId) {
        return goalRepository.findByUserIdAndSkillIdAndStatus(userId, skillId, GoalStatus.ACTIVE);
    }

    private void updateRubricAggregates(TaskCompletedEvent event) {
        if (event.rubricsScores() == null || event.rubricsScores().isEmpty()) {
            return;
        }

        String sql = """
        INSERT INTO user_rubric_stats (user_id, rubric, total_score, total_max_score, submission_count, updated_at)
        VALUES (?::uuid, ?::text, ?::bigint, ?::bigint, 1, NOW())
        ON CONFLICT (user_id, rubric) DO UPDATE SET
            total_score = user_rubric_stats.total_score + EXCLUDED.total_score,
            total_max_score = user_rubric_stats.total_max_score + EXCLUDED.total_max_score,
            submission_count = user_rubric_stats.submission_count +  1,
            updated_at = NOW()
        """;

        List<Object[]> batch = event.rubricsScores().entrySet().stream()
                .map(entry -> {
                    String rubric = entry.getKey();
                    RubricScoreDTO dto = entry.getValue();
                    return new Object[] {
                            event.userId(),
                            rubric,
                            (long) dto.score(),      // safe cast, scores are small
                            (long) dto.maxScore()
                    };
                })
                .toList();

        jdbcTemplate.batchUpdate(sql, batch);
    }

    private boolean updateSingleGoal(UserGoal goal, UserSkillProgress progress) {
        boolean updated = false;
        switch (goal.getGoalType()) {
            case TARGET_XP:
            case REACH_LEVEL:
                if (progress.getTotalXpEarned() > goal.getCurrentValue()) {
                    goal.setCurrentValue(progress.getTotalXpEarned());
                    updated = true;
                }
                break;
            case TASKS_COMPLETED:
                if (progress.getTasksCompleted() > goal.getCurrentValue()) {
                    goal.setCurrentValue(progress.getTasksCompleted());
                    updated = true;
                }
                break;
        }
        return updated;
    }

    private void handleGoalCompletion(UserGoal goal) {
        if (goal.checkAndMarkCompleted()) {
            log.info("User {} completed goal: {}", goal.getUserId(), goal.getId());
            String description = String.format("Completed goal for %s", goal.getSkillName());
            eventPublisher.publishEvent(new GoalCompletedEvent(this, goal.getUserId(), goal.getId(), description));
        }
    }

    private LocalDate getEventDate(TaskCompletedEvent event) {
        if (event.completedAt() == null) {  // Defensive: Prevent NPE upstream
            log.warn("Invalid event date for user {} and skill {}", event.userId(), event.skillId());
        }
        return event.completedAt().atZone(ZoneOffset.UTC).toLocalDate();
    }


    private void updateTrajectorySnapshot(TaskCompletedEvent event) {
        fetchOrCreateTrajectorySnapshot(event);
    }

    private void fetchOrCreateTrajectorySnapshot(TaskCompletedEvent event) {
        LocalDate eventDate = getEventDate(event);

        SkillTrajectorySnapshot snapshot =  trajectoryRepository
                .findByUserIdAndSkillIdAndSnapshotDate(event.userId(), event.skillId(), eventDate)
                .orElse(null);
        if (snapshot == null) {
            snapshot = SkillTrajectorySnapshot.createNew(
                    event.userId(),
                    event.skillId(),
                    eventDate,
                    event.totalXpEarned()
            );
        } else {
            snapshot.recordTaskCompletion(event.totalXpEarned());
        }
        trajectoryRepository.save(snapshot);

    }


    private void updateUserAggregateStats(TaskCompletedEvent event) {
        UserAggregateStats stats = fetchOrCreateAggregateStats(event.userId());
        LocalDate practiceDate = getEventDate(event);
        if (event.passed()) {
            stats.incrementTasksCompleted();
        }
        stats.updateStreak(practiceDate);
        aggregateStatsRepository.save(stats);
    }

    private UserAggregateStats fetchOrCreateAggregateStats(UUID userId) {
        return aggregateStatsRepository.findById(userId)
                .orElseGet(() -> new UserAggregateStats(userId));
    }


    private void logSubmission(TaskCompletedEvent event) {
        TaskSubmissionLog logEntry = buildSubmissionLog(event);
        logRepository.save(logEntry);
        log.debug("Logged new task submission for user {} and skill {}", event.userId(), event.skillId());
    }

    private TaskSubmissionLog buildSubmissionLog(TaskCompletedEvent event) {
        return TaskSubmissionLog.builder()
                .userId(event.userId())
                .skillId(event.skillId())
                .taskId(event.taskId())
                .totalXpEarned(event.totalXpEarned())
                .passed(event.passed())
                .taskType(event.taskType())
                .rubricsScores(event.rubricsScores())
                .completedAt(event.completedAt())
                .build();
    }
}
