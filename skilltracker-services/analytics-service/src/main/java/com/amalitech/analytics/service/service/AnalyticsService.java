package com.amalitech.analytics.service.service;


import com.amalitech.analytics.service.dto.TaskCompletedEvent;
import com.amalitech.analytics.service.dto.TaskSubmissionRequestDTO;
import com.amalitech.analytics.service.events.AnalyticsUpdateEvent;
import com.amalitech.analytics.service.model.SkillTrajectorySnapshot;
import com.amalitech.analytics.service.model.TaskSubmissionLog;
import com.amalitech.analytics.service.model.UserAggregateStats;
import com.amalitech.analytics.service.model.UserSkillProgress;
import com.amalitech.analytics.service.repository.SkillTrajectorySnapshotRepository;
import com.amalitech.analytics.service.repository.TaskSubmissionLogRepository;
import com.amalitech.analytics.service.repository.UserAggregateStatsRepository;
import com.amalitech.analytics.service.repository.UserSkillProgressRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.ZoneOffset;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final TaskSubmissionLogRepository logRepository;
    private final UserSkillProgressRepository skillProgressRepository;
    private final UserAggregateStatsRepository aggregateStatsRepository;
    private final SkillTrajectorySnapshotRepository trajectoryRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     Processes a completed task event, updating skill progress, user statistics, and snapshots.
     */
    @Transactional
    public void processTaskCompletion(TaskCompletedEvent event) {
        log.info("Processing TaskCompletedEvent for user: {}", event.userId());
        UserSkillProgress progress = updateSkillProgress(event);
        updateUserAggregateStats(event);
        logSubmission(event);
        updateTrajectorySnapshot(progress);
        eventPublisher.publishEvent(new AnalyticsUpdateEvent(this, event.userId()));
    }

    @Transactional
    public void submitTaskDirectly(TaskSubmissionRequestDTO request) {
        log.warn("DIRECT SUBMISSION API USED: Bypassing message queue for user {}", request.userId());
        TaskCompletedEvent event = request.toEvent();
        this.processTaskCompletion(event);
    }

    /**
     Retrieves or creates a UserSkillProgress record and updates it with the XP from the event.

     @param event The completed task event.
     @return Updated UserSkillProgress entity.
     */
    private UserSkillProgress updateSkillProgress(TaskCompletedEvent event) {
        UserSkillProgress progress = skillProgressRepository
                .findByUserIdAndSkillId(event.userId(), event.skillId())
                .orElseGet(() -> {
                    UserSkillProgress newProgress = new UserSkillProgress();
                    newProgress.setUserId(event.userId());
                    newProgress.setSkillId(event.skillId());
                    newProgress.setTasksCompleted(0);
                    newProgress.setTotalXpEarned(0);
                    newProgress.setAverageXpEarned(0.0);
                    newProgress.setProficiency(0.0);
                    return newProgress;
                });

        progress.updateProgress(event.totalXpEarned());
        return skillProgressRepository.save(progress);
    }

    /**
     Updates the daily trajectory snapshot for a user's skill based on current progress.

     @param progress The user's skill progress.
     */
    private void updateTrajectorySnapshot(UserSkillProgress progress) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        SkillTrajectorySnapshot snapshot = trajectoryRepository
                .findByUserIdAndSkillIdAndSnapshotDate(progress.getUserId(), progress.getSkillId(), today)
                .orElseGet(() -> SkillTrajectorySnapshot.fromProgress(progress, today));
        trajectoryRepository.save(snapshot);
    }

    /**
     Updates the user's aggregate stats such as total tasks completed and streaks.

     @param event The task completed event.
     @return The updated UserAggregateStats entity.
     */
    private UserAggregateStats updateUserAggregateStats(TaskCompletedEvent event) {
        LocalDate practiceDate = event.completedAt().atZone(ZoneOffset.UTC).toLocalDate();
        UserAggregateStats stats = aggregateStatsRepository.findById(event.userId())
                .orElseGet(() -> new UserAggregateStats(event.userId()));
        stats.updateStreak(practiceDate);
        return aggregateStatsRepository.save(stats);
    }

    /**
     Logs a task submission event by persisting it as an immutable TaskSubmissionLog entity.

     @param event The completed task event.
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