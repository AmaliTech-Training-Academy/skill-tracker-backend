package com.amalitech.analytics.service;


import com.amalitech.analytics.service.dto.RubricScoreDTO;
import com.amalitech.analytics.service.dto.TaskCompletedEvent;
import com.amalitech.analytics.service.events.AnalyticsUpdateEvent;
import com.amalitech.analytics.service.events.GoalCompletedEvent;
import com.amalitech.analytics.service.exception.EntityNotFoundException;
import com.amalitech.analytics.service.model.*;
import com.amalitech.analytics.service.model.enums.GoalStatus;
import com.amalitech.analytics.service.model.enums.GoalType;
import com.amalitech.analytics.service.model.enums.TaskType;
import com.amalitech.analytics.service.repository.*;
import com.amalitech.analytics.service.service.AnalyticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AnalyticsServiceTest {

    @Mock private TaskSubmissionLogRepository logRepository;
    @Mock private UserSkillProgressRepository skillProgressRepository;
    @Mock private UserAggregateStatsRepository aggregateStatsRepository;
    @Mock private SkillTrajectorySnapshotRepository trajectoryRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private SkillSnapshotRepository skillSnapshotRepository;
    @Mock private UserGoalRepository goalRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    private UUID userId;
    private UUID skillId;
    private TaskCompletedEvent event;
    private SkillSnapShot snapshot;
    private final int NEW_XP = 100;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        skillId = UUID.randomUUID();

        snapshot = SkillSnapShot.builder()
                .id(skillId)
                .name("Java")
                .levelXpMap(Map.of("INTERMEDIATE", 1000L, "ADVANCED", 3000L))
                .build();

        event = new TaskCompletedEvent(
                userId,
                skillId,
                UUID.randomUUID().toString(),
                "Debug",
                NEW_XP,
                true,
                Instant.now(),
                TaskType.CODING,
                "MEDIUM",
                Map.of("Clarity", new RubricScoreDTO(8, 10, 80))
        );

        when(skillSnapshotRepository.findById(skillId)).thenReturn(Optional.of(snapshot));
        when(skillProgressRepository.save(any(UserSkillProgress.class))).thenAnswer(inv -> inv.getArgument(0));
        when(aggregateStatsRepository.save(any(UserAggregateStats.class))).thenAnswer(inv -> inv.getArgument(0));
        when(trajectoryRepository.save(any(SkillTrajectorySnapshot.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("processTaskCompletion: New User should initialize all entities correctly")
    void processTaskCompletion_NewUserInitialization() {
        when(skillProgressRepository.findByUserIdAndSkillId(userId, skillId)).thenReturn(Optional.empty());
        when(aggregateStatsRepository.findById(userId)).thenReturn(Optional.empty());
        when(trajectoryRepository.findByUserIdAndSkillIdAndSnapshotDate(any(), any(), any())).thenReturn(Optional.empty());
        when(goalRepository.findByUserIdAndSkillIdAndStatus(any(), any(), any())).thenReturn(List.of());

        analyticsService.processTaskCompletion(event);

        ArgumentCaptor<UserSkillProgress> progressCaptor = ArgumentCaptor.forClass(UserSkillProgress.class);
        verify(skillProgressRepository).save(progressCaptor.capture());
        UserSkillProgress savedProgress = progressCaptor.getValue();
        assertThat(savedProgress.getTotalXpEarned()).isEqualTo(NEW_XP);
        assertThat(savedProgress.getProficiency()).isBetween(3.3, 3.4);
        assertThat(savedProgress.getTasksCompleted()).isEqualTo(1);

        ArgumentCaptor<UserAggregateStats> statsCaptor = ArgumentCaptor.forClass(UserAggregateStats.class);
        verify(aggregateStatsRepository).save(statsCaptor.capture());
        assertThat(statsCaptor.getValue().getTotalTasksCompleted()).isEqualTo(1);
        assertThat(statsCaptor.getValue().getCurrentStreakInDays()).isEqualTo(1);

        verify(logRepository, times(1)).save(any(TaskSubmissionLog.class));
        verify(trajectoryRepository, times(1)).save(any(SkillTrajectorySnapshot.class));
        verify(eventPublisher, times(1)).publishEvent(any(AnalyticsUpdateEvent.class));
    }

    @Test
    @DisplayName("processTaskCompletion: Existing User should update all entities correctly")
    void processTaskCompletion_ExistingUserUpdate() {
        UserSkillProgress progress = new UserSkillProgress();
        progress.setUserId(userId);
        progress.setSkillId(skillId);
        progress.setTasksCompleted(5);
        progress.setTotalXpEarned(500);

        UserAggregateStats stats = new UserAggregateStats(userId);
        stats.setTotalTasksCompleted(10);
        stats.updateStreak(LocalDate.now(ZoneOffset.UTC).minusDays(1));

        when(skillProgressRepository.findByUserIdAndSkillId(userId, skillId)).thenReturn(Optional.of(progress));
        when(aggregateStatsRepository.findById(userId)).thenReturn(Optional.of(stats));
        when(trajectoryRepository.findByUserIdAndSkillIdAndSnapshotDate(any(), any(), any())).thenReturn(Optional.empty());
        when(goalRepository.findByUserIdAndSkillIdAndStatus(any(), any(), any())).thenReturn(List.of());

        analyticsService.processTaskCompletion(event);

        assertThat(progress.getTotalXpEarned()).isEqualTo(600);
        assertThat(progress.getTasksCompleted()).isEqualTo(6);
        assertThat(progress.getProficiency()).isBetween(19.9, 20.0);

        verify(aggregateStatsRepository).save(stats);
        assertThat(stats.getTotalTasksCompleted()).isEqualTo(11);
        assertThat(stats.getCurrentStreakInDays()).isEqualTo(2);

        ArgumentCaptor<SkillTrajectorySnapshot> trajectoryCaptor = ArgumentCaptor.forClass(SkillTrajectorySnapshot.class);
        verify(trajectoryRepository).save(trajectoryCaptor.capture());
        assertThat(trajectoryCaptor.getValue().getTasksCompletedToday()).isEqualTo(6);
    }

    @Test
    @DisplayName("processTaskCompletion should throw EntityNotFoundException if snapshot is missing")
    void processTaskCompletion_Throws_MissingSnapshot() {
        when(skillSnapshotRepository.findById(skillId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> analyticsService.processTaskCompletion(event))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Skill snapshot not found");

        verify(skillProgressRepository, never()).save(any());
        verify(aggregateStatsRepository, never()).save(any());
        verify(logRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("updateGoalProgress should update multiple active goals and mark completion")
    void updateGoalProgress_GoalCompletion() {
        UserSkillProgress progress = new UserSkillProgress();
        progress.setUserId(userId);
        progress.setSkillId(skillId);
        progress.setTotalXpEarned(950);
        progress.setTasksCompleted(10);

        when(skillProgressRepository.findByUserIdAndSkillId(userId, skillId))
                .thenReturn(Optional.of(progress));

        UserGoal goal1 = Mockito.spy(new UserGoal());
        goal1.setId(UUID.randomUUID());
        goal1.setGoalType(GoalType.TARGET_XP);
        goal1.setTargetValue(1000);
        goal1.setCurrentValue(900);
        goal1.setInitialValue(500);
        doReturn(true).when(goal1).checkAndMarkCompleted();

        UserGoal goal2 = Mockito.spy(new UserGoal());
        goal2.setId(UUID.randomUUID());
        goal2.setGoalType(GoalType.TASKS_COMPLETED);
        goal2.setTargetValue(20);
        goal2.setCurrentValue(10);
        goal2.setInitialValue(5);
        doReturn(false).when(goal2).checkAndMarkCompleted();

        when(goalRepository.findByUserIdAndSkillIdAndStatus(userId, skillId, GoalStatus.ACTIVE))
                .thenReturn(List.of(goal1, goal2));

        analyticsService.processTaskCompletion(event);

        assertThat(goal1.getCurrentValue()).isEqualTo(1050);
        assertThat(goal2.getCurrentValue()).isEqualTo(11);

        ArgumentCaptor<List<UserGoal>> goalListCaptor = ArgumentCaptor.forClass(List.class);
        verify(goalRepository).saveAll(goalListCaptor.capture());
        assertThat(goalListCaptor.getValue()).containsExactlyInAnyOrder(goal1, goal2);

        verify(eventPublisher, times(1)).publishEvent(any(GoalCompletedEvent.class));
        verify(goal1, times(1)).checkAndMarkCompleted();
        verify(goal2, times(1)).checkAndMarkCompleted();
    }

    @Test
    @DisplayName("calculateProficiency: BEGINNER threshold (1000 XP)")
    void calculateProficiency_BeginnerThreshold() {
        double proficiency = analyticsService.calculateProficiency(1000, snapshot);
        assertThat(proficiency).isEqualTo(33.3);
    }

    @Test
    @DisplayName("calculateProficiency: INTERMEDIATE mid-point (2000 XP)")
    void calculateProficiency_IntermediateMidpoint() {
        double proficiency = analyticsService.calculateProficiency(2000, snapshot);
        assertThat(proficiency).isBetween(49.9, 50.0);
    }

    @Test
    @DisplayName("calculateProficiency: ADVANCED threshold (3000 XP)")
    void calculateProficiency_AdvancedThreshold() {
        double proficiency = analyticsService.calculateProficiency(3000, snapshot);
        assertThat(proficiency).isEqualTo(66.6);
    }

    @Test
    @DisplayName("calculateProficiency: Capstone completion (5000 XP)")
    void calculateProficiency_Capstone() {
        double proficiency = analyticsService.calculateProficiency(5000, snapshot);
        assertThat(proficiency).isEqualTo(100.0);
    }
}
