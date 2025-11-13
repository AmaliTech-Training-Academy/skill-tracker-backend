package com.amalitech.analytics.service;

import com.amalitech.analytics.service.dto.*;
import com.amalitech.analytics.service.exception.EntityNotFoundException;
import com.amalitech.analytics.service.model.SkillSnapShot;
import com.amalitech.analytics.service.model.UserAggregateStats;
import com.amalitech.analytics.service.model.UserGoal;
import com.amalitech.analytics.service.model.UserSkillProgress;
import com.amalitech.analytics.service.model.enums.GoalStatus;
import com.amalitech.analytics.service.model.enums.GoalType;
import com.amalitech.analytics.service.model.enums.Granularity;
import com.amalitech.analytics.service.repository.*;
import com.amalitech.analytics.service.service.AnalyticsReadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsReadServiceTest {

    @Mock private UserAggregateStatsRepository aggregateStatsRepository;
    @Mock private UserSkillProgressRepository skillProgressRepository;
    @Mock private SkillSnapshotRepository skillSnapshotRepository;
    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private UserGoalRepository goalRepository;
    @Mock private TaskSubmissionLogRepository logRepository;

    @InjectMocks
    private AnalyticsReadService analyticsReadService;

    private UUID userId;
    private UUID skillId1, skillId2;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        skillId1 = UUID.randomUUID();
        skillId2 = UUID.randomUUID();
        today = LocalDate.now(ZoneOffset.UTC);
    }

    @Test
    @DisplayName("Dashboard should build correctly when all data is present")
    void buildDashboard_AllDataPresent() {
        UserAggregateStats stats = new UserAggregateStats(userId);
        stats.setTotalTasksCompleted(50);
        stats.setCurrentStreakInDays(5);
        stats.setLongestStreakInDays(10);
        stats.setLastPracticeDate(today.minusDays(1));

        UserSkillProgress progress1 = new UserSkillProgress();
        progress1.setSkillId(skillId1);
        progress1.setTotalXpEarned(1500);
        progress1.setProficiency(50.0);

        SkillSnapShot snap1 = SkillSnapShot.builder()
                .id(skillId1)
                .name("Java")
                .levelXpMap(Map.of("INTERMEDIATE", 1000L, "ADVANCED", 3000L))
                .build();

        when(aggregateStatsRepository.findById(userId)).thenReturn(Optional.of(stats));
        when(skillProgressRepository.findAllByUserId(userId)).thenReturn(List.of(progress1));
        when(skillSnapshotRepository.findAllById(anySet())).thenReturn(List.of(snap1));
        when(goalRepository.findByUserIdAndStatus(userId, GoalStatus.ACTIVE)).thenReturn(List.of());
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(userId), anyDouble())).thenReturn(List.of());

        DashboardDTO dashboard = analyticsReadService.buildDashboard(userId);

        assertThat(dashboard).isNotNull();
        assertThat(dashboard.userStats().totalTasksCompleted()).isEqualTo(50);
        assertThat(dashboard.skillProgress()).hasSize(1);
        assertThat(dashboard.skillProgress().get(0).skillName()).isEqualTo("Java");
        assertThat(dashboard.skillGaps()).isEmpty();
        assertThat(dashboard.recommendations()).hasSize(1);
        assertThat(dashboard.recommendations().get(0).recommendationText()).contains("Keep up the great work");
    }

    @Test
    @DisplayName("Dashboard should return empty/default DTOs when no data exists")
    void buildDashboard_NoData() {
        when(aggregateStatsRepository.findById(userId)).thenReturn(Optional.empty());
        when(skillProgressRepository.findAllByUserId(userId)).thenReturn(List.of());
        when(goalRepository.findByUserIdAndStatus(userId, GoalStatus.ACTIVE)).thenReturn(List.of());
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(userId), anyDouble())).thenReturn(List.of());

        DashboardDTO dashboard = analyticsReadService.buildDashboard(userId);

        assertThat(dashboard).isNotNull();
        assertThat(dashboard.userStats().totalTasksCompleted()).isEqualTo(0);
        assertThat(dashboard.skillProgress()).isEmpty();
        assertThat(dashboard.goalStatus()).isEmpty();
        assertThat(dashboard.skillGaps()).isEmpty();
        assertThat(dashboard.recommendations()).hasSize(1);
    }

    @Test
    @DisplayName("getUserStats should return mapped DTO when stats exist")
    void getUserStats_Exists() {
        UserAggregateStats stats = new UserAggregateStats(userId);
        stats.setTotalTasksCompleted(10);
        stats.setCurrentStreakInDays(2);
        stats.setLastPracticeDate(today);

        when(aggregateStatsRepository.findById(userId)).thenReturn(Optional.of(stats));

        UserStatsDTO result = analyticsReadService.buildDashboard(userId).userStats();

        assertThat(result.totalTasksCompleted()).isEqualTo(10);
        assertThat(result.currentStreakInDays()).isEqualTo(2);
        assertThat(result.lastPracticeDate()).isEqualTo(today);
    }

    @Test
    @DisplayName("getUserStats should return default DTO when stats do not exist")
    void getUserStats_NotExists() {
        when(aggregateStatsRepository.findById(userId)).thenReturn(Optional.empty());

        UserStatsDTO result = analyticsReadService.buildDashboard(userId).userStats();

        assertThat(result.totalTasksCompleted()).isEqualTo(0);
        assertThat(result.currentStreakInDays()).isEqualTo(0);
        assertThat(result.lastPracticeDate()).isNull();
    }

    @Test
    @DisplayName("getSkillProgress should map progress correctly including level calculation")
    void getSkillProgress_MappingSuccess() {
        UserSkillProgress progress = new UserSkillProgress();
        progress.setSkillId(skillId1);
        progress.setTotalXpEarned(2500);
        progress.setProficiency(57.75);
        progress.setAverageXpEarned(250.0);
        progress.setTasksCompleted(10);

        SkillSnapShot snap = SkillSnapShot.builder()
                .id(skillId1)
                .name("React")
                .levelXpMap(Map.of("INTERMEDIATE", 1000L, "ADVANCED", 3000L))
                .build();

        when(skillProgressRepository.findAllByUserId(userId)).thenReturn(List.of(progress));
        when(skillSnapshotRepository.findAllById(Set.of(skillId1))).thenReturn(List.of(snap));

        List<SkillProgressDTO> result = analyticsReadService.buildDashboard(userId).skillProgress();

        assertThat(result).hasSize(1);
        SkillProgressDTO dto = result.get(0);
        assertThat(dto.skillName()).isEqualTo("React");
        assertThat(dto.proficiency()).isEqualTo(57.75);
        assertThat(dto.currentXp()).isEqualTo(2500);
        assertThat(dto.currentLevel()).isEqualTo("INTERMEDIATE");
        assertThat(dto.nextLevel()).isEqualTo("ADVANCED");
        assertThat(dto.xpToNextLevel()).isEqualTo(500);
        assertThat(dto.currentLevelTotalXp()).isEqualTo(2000);
    }

    @Test
    @DisplayName("getSkillProgress should throw EntityNotFoundException if snapshot is missing")
    void getSkillProgress_MissingSnapshot() {
        UserSkillProgress progress = new UserSkillProgress();
        progress.setSkillId(skillId1);
        progress.setTotalXpEarned(100);

        when(skillProgressRepository.findAllByUserId(userId)).thenReturn(List.of(progress));
        when(skillSnapshotRepository.findAllById(Set.of(skillId1))).thenReturn(List.of());

        assertThatThrownBy(() -> analyticsReadService.buildDashboard(userId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Skill not found");
    }

    @Test
    @DisplayName("getActiveGoalStatus should map active goals correctly")
    void getActiveGoalStatus_Success() {
        UserGoal goal1 = new UserGoal();
        goal1.setId(UUID.randomUUID());
        goal1.setGoalType(GoalType.TARGET_XP);
        goal1.setSkillName("Python");
        goal1.setTargetValue(2000);
        goal1.setInitialValue(1000);
        goal1.setCurrentValue(1500);
        goal1.setDeadline(today.plusDays(5));

        when(goalRepository.findByUserIdAndStatus(userId, GoalStatus.ACTIVE)).thenReturn(List.of(goal1));

        List<GoalStatusDTO> result = analyticsReadService.buildDashboard(userId).goalStatus();

        assertThat(result).hasSize(1);
        GoalStatusDTO dto = result.get(0);
        assertThat(dto.description()).isEqualTo("Reach 2000 XP in Python");
        assertThat(dto.progressPercentage()).isEqualTo(50.0);
        assertThat(dto.status()).isEqualTo("On Track");
    }

    @Test
    @DisplayName("getActiveGoalStatus should mark goal as Overdue")
    void getActiveGoalStatus_Overdue() {
        UserGoal goal1 = new UserGoal();
        goal1.setGoalType(GoalType.TARGET_XP);
        goal1.setTargetValue(2000);
        goal1.setInitialValue(1000);
        goal1.setCurrentValue(1500);
        goal1.setDeadline(today.minusDays(1));

        when(goalRepository.findByUserIdAndStatus(userId, GoalStatus.ACTIVE)).thenReturn(List.of(goal1));

        List<GoalStatusDTO> result = analyticsReadService.buildDashboard(userId).goalStatus();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo("Overdue");
    }

    @Test
    @DisplayName("calculateGoalProgressPercentage handles zero range safely")
    void calculateGoalProgressPercentage_ZeroRange() {
        UserGoal goal = new UserGoal();
        goal.setTargetValue(100);
        goal.setInitialValue(100);
        goal.setCurrentValue(100);

        try {
            java.lang.reflect.Method method = AnalyticsReadService.class.getDeclaredMethod("calculateGoalProgressPercentage", UserGoal.class);
            method.setAccessible(true);
            double result = (double) method.invoke(analyticsReadService, goal);
            assertThat(result).isEqualTo(0.0);
        } catch (Exception e) {
            fail("Reflection error", e);
        }
    }

    @Test
    @DisplayName("getSkillGaps should call JDBC and map results below threshold")
    void getSkillGaps_ReturnsGaps() {
        SkillGapDTO gap1 = new SkillGapDTO("Clarity", 60.0, "Weak performance in Clarity (Avg: 60.0/100)");
        SkillGapDTO gap2 = new SkillGapDTO("Efficiency", 65.0, "Weak performance in Efficiency (Avg: 65.0/100)");

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(userId), anyDouble()))
                .thenReturn(List.of(gap1, gap2));

        DashboardDTO dashboard = analyticsReadService.buildDashboard(userId);

        verify(jdbcTemplate).query(anyString(), any(RowMapper.class), eq(userId), eq(70.0));
        assertThat(dashboard.skillGaps()).hasSize(2).extracting(SkillGapDTO::rubric).containsExactly("Clarity", "Efficiency");
        assertThat(dashboard.recommendations()).hasSize(2);
        assertThat(dashboard.recommendations().get(0).recommendationText()).contains("focus on 'Clarity'");
        assertThat(dashboard.recommendations().get(1).recommendationText()).contains("focus on 'Efficiency'");
    }

    @Test
    @DisplayName("getRecommendations should return positive message when no gaps exist")
    void getRecommendations_NoGaps() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(userId), anyDouble()))
                .thenReturn(List.of());

        DashboardDTO dashboard = analyticsReadService.buildDashboard(userId);

        assertThat(dashboard.skillGaps()).isEmpty();
        assertThat(dashboard.recommendations()).hasSize(1);
        assertThat(dashboard.recommendations().get(0).recommendationText()).contains("Keep up the great work!");
        assertThat(dashboard.recommendations().get(0).relatedRubric()).isNull();
    }

    @Test
    @DisplayName("getSkillTrajectory should call queryDailyTrajectory for DAILY granularity")
    void getSkillTrajectory_Daily() {
        TrajectoryPointDTO point = new TrajectoryPointDTO(today, 100.0, 10);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(userId), eq(skillId1))).thenReturn(List.of(point));

        List<TrajectoryPointDTO> result = analyticsReadService.getSkillTrajectory(userId, skillId1, Granularity.DAILY);

        assertThat(result).hasSize(1);
        verify(jdbcTemplate, times(1)).query(contains("DISTINCT ON (snapshot_date::date)"), any(RowMapper.class), eq(userId), eq(skillId1));
    }

    @Test
    @DisplayName("getSkillTrajectory should call queryTrajectoryWithGranularity for MONTHLY")
    void getSkillTrajectory_Monthly() {
        TrajectoryPointDTO point = new TrajectoryPointDTO(today.withDayOfMonth(1), 150.0, 50);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(userId), eq(skillId1))).thenReturn(List.of(point));

        List<TrajectoryPointDTO> result = analyticsReadService.getSkillTrajectory(userId, skillId1, Granularity.MONTHLY);

        assertThat(result).hasSize(1);
        verify(jdbcTemplate, times(1)).query(contains("DATE_TRUNC('month'"), any(RowMapper.class), eq(userId), eq(skillId1));
    }

    @Test
    @DisplayName("getSkillTrajectory should call queryTrajectoryWithGranularity for WEEKLY")
    void getSkillTrajectory_Weekly() {
        TrajectoryPointDTO point = new TrajectoryPointDTO(today.minusDays(today.getDayOfWeek().getValue() - 1), 120.0, 30);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(userId), eq(skillId1))).thenReturn(List.of(point));

        List<TrajectoryPointDTO> result = analyticsReadService.getSkillTrajectory(userId, skillId1, Granularity.WEEKLY);

        assertThat(result).hasSize(1);
        verify(jdbcTemplate, times(1)).query(contains("DATE_TRUNC('week'"), any(RowMapper.class), eq(userId), eq(skillId1));
    }

    @Test
    @DisplayName("getAllPracticeDays returns distinct practice days from log repository")
    void getAllPracticeDays_Success() {
        List<LocalDate> expected = List.of(today.minusDays(2), today.minusDays(1), today);
        when(logRepository.findAllDistinctPracticeDaysByUserId(userId)).thenReturn(expected);

        List<LocalDate> result = analyticsReadService.getAllPracticeDays(userId);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("getCurrentStreakDays returns correct consecutive streak")
    void getCurrentStreakDays_ConsecutiveStreak() {
        List<LocalDate> allDays = List.of(today.minusDays(4), today.minusDays(2), today.minusDays(1), today);
        when(logRepository.findAllDistinctPracticeDaysByUserId(userId)).thenReturn(allDays);

        List<LocalDate> streak = analyticsReadService.getCurrentStreakDays(userId);

        assertThat(streak).hasSize(3).containsExactly(today.minusDays(2), today.minusDays(1), today);
    }

    @Test
    @DisplayName("getCurrentStreakDays returns empty list when no practice days exist")
    void getCurrentStreakDays_NoPracticeDays() {
        when(logRepository.findAllDistinctPracticeDaysByUserId(userId)).thenReturn(List.of());

        List<LocalDate> streak = analyticsReadService.getCurrentStreakDays(userId);

        assertThat(streak).isEmpty();
    }
}
