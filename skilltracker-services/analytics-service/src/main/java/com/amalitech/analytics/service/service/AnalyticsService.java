package com.amalitech.analytics.service.service;

import com.amalitech.analytics.service.dto.TaskCompletedEvent;
import com.amalitech.analytics.service.dto.TaskSubmissionRequestDTO;
import com.amalitech.analytics.service.events.AnalyticsUpdateEvent;
import com.amalitech.analytics.service.events.GoalCompletedEvent;
import com.amalitech.analytics.service.model.*;
import com.amalitech.analytics.service.model.enums.GoalStatus;
import com.amalitech.analytics.service.repository.*;
import com.amalitech.analytics.service.service.interfaces.AnalyticsServiceInterface;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
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

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void processTaskCompletion(TaskCompletedEvent event) {
        log.info("Processing TaskCompletedEvent for user: {}", event.userId());

        UserSkillProgress progress = updateSkillProgress(event);
        updateUserAggregateStats(event);
        logSubmission(event);
        updateTrajectorySnapshot(progress);
        updateGoalProgress(progress);

        eventPublisher.publishEvent(new AnalyticsUpdateEvent(this, event.userId()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void submitTaskDirectly(TaskSubmissionRequestDTO request) {
        log.warn("DIRECT SUBMISSION API USED: Bypassing message queue for user {}", request.userId());
        TaskCompletedEvent event = request.toEvent();
        this.processTaskCompletion(event);
    }

    /**
     * Retrieves or creates a {@link UserSkillProgress} record and updates it
     * based on the completed task event.
     *
     * @param event the task completion event
     * @return the updated {@link UserSkillProgress} entity
     */
    private UserSkillProgress updateSkillProgress(TaskCompletedEvent event) {
        UserSkillProgress progress = getSkillProgress(event.skillId(), event.userId());

        // --- NEW: Proficiency Calculation (from Solution B) ---
        SkillSnapShot snapshot = skillSnapshotRepository.findById(event.skillId())
                .orElseThrow(() -> new EntityNotFoundException("Skill snapshot not found: " + event.skillId()));

        // Use defaults as fallbacks
        long intermediate = snapshot.getLevelXpMap().getOrDefault("INTERMEDIATE", 1000L);
        long advanced = snapshot.getLevelXpMap().getOrDefault("ADVANCED", 3000L);
        int currentXp = progress.getTotalXpEarned();

        double prof;
        if (currentXp < intermediate) {
            // Scale 0-33% of proficiency based on progress to Intermediate
            prof = (double) currentXp / intermediate * 33.3;
        } else if (currentXp < advanced) {
            // Scale 33-66% of proficiency based on progress to Advanced
            prof = 33.3 + ((double) (currentXp - intermediate) / (advanced - intermediate)) * 33.3;
        } else {
            // Scale 66-100% of proficiency.
            // Using (advanced - intermediate) as a "capstone" XP chunk size
            long capstoneChunk = (advanced - intermediate > 0) ? (advanced - intermediate) : 2000L;
            prof = 66.6 + ((double) (currentXp - advanced) / capstoneChunk) * 33.4;
        }

        progress.updateProgress(event.totalXpEarned(), Math.min(prof, 100.0));
        return skillProgressRepository.save(progress);
    }


    /**
     * Retrieves or creates a {@link UserSkillProgress} record and updates it
     * based on userId and SkillId passed to it
     *
     * @param skillId the skill which you want to st a goal on
     * @param userId the user who wants to set the goal
     * @return the updated {@link UserSkillProgress} entity
     */
    private UserSkillProgress getSkillProgress(UUID skillId, UUID userId) {
        UserSkillProgress skillProgress = skillProgressRepository
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

        return skillProgress;
    }

    /**
     * NEW: Updates all active user goals related to the completed task's skill.
     *
     * @param progress The updated UserSkillProgress entity.
     */
    private void updateGoalProgress(UserSkillProgress progress) {
        List<UserGoal> activeGoals = goalRepository.findByUserIdAndSkillIdAndStatus(
                progress.getUserId(),
                progress.getSkillId(),
                GoalStatus.ACTIVE
        );

        if (activeGoals.isEmpty()) {
            return;
        }

        log.debug("Found {} active goals for user {} and skill {}",
                activeGoals.size(), progress.getUserId(), progress.getSkillId());

        for (UserGoal goal : activeGoals) {
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

            if (updated) {
                boolean justCompleted = goal.checkAndMarkCompleted();
                if (justCompleted) {
                    log.info("User {} completed goal: {}", goal.getUserId(), goal.getId());
                    String description = String.format("Completed goal for %s", goal.getSkillName());
                    eventPublisher.publishEvent(new GoalCompletedEvent(
                            this, goal.getUserId(), goal.getId(), description
                    ));
                }
            }
        }
        goalRepository.saveAll(activeGoals);
    }


    /**
     * Updates or creates a trajectory snapshot entry for the user’s skill
     * on the current date, reflecting the latest progress state.
     *
     * @param progress the current user skill progress
     */
    private void updateTrajectorySnapshot(UserSkillProgress progress) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        SkillTrajectorySnapshot snapshot = trajectoryRepository
                .findByUserIdAndSkillIdAndSnapshotDate(progress.getUserId(), progress.getSkillId(), today)
                .orElseGet(() -> SkillTrajectorySnapshot.fromProgress(progress, today));

        trajectoryRepository.save(snapshot);
    }

    /**
     * Updates the aggregate user statistics such as total tasks completed
     * and streak information.
     *
     * @param event the completed task event
     * @return the updated {@link UserAggregateStats} entity
     */
    private UserAggregateStats updateUserAggregateStats(TaskCompletedEvent event) {
        LocalDate practiceDate = event.completedAt().atZone(ZoneOffset.UTC).toLocalDate();

        UserAggregateStats stats = aggregateStatsRepository.findById(event.userId())
                .orElseGet(() -> new UserAggregateStats(event.userId()));

        stats.updateStreak(practiceDate);
        return aggregateStatsRepository.save(stats);
    }

    /**
     * Logs a task submission by persisting a {@link TaskSubmissionLog} record.
     *
     * <p>This log provides an immutable audit trail of user submissions
     * for analytical and compliance purposes.</p>
     *
     * @param event the completed task event
     */
    private void logSubmission(TaskCompletedEvent event) {
        TaskSubmissionLog submissionLog = TaskSubmissionLog.builder()
                .userId(event.userId())
                .skillId(event.skillId())
                .taskId(event.taskId())
                .totalXpEarned(event.totalXpEarned())
                .passed(event.passed())
                .taskType(event.taskType())
                .rubricsScores(event.rubricsScores())
                .completedAt(event.completedAt())
                .build();

        logRepository.save(submissionLog);
        log.debug("Logged new task submission for user {} and skill {}", event.userId(), event.skillId());
    }
}
