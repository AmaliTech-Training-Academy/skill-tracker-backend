package com.amalitech.analytics.service;

import com.amalitech.analytics.service.dto.CreateGoalRequestDTO;
import com.amalitech.analytics.service.dto.UserGoalDTO;
import com.amalitech.analytics.service.exception.EntityNotFoundException;
import com.amalitech.analytics.service.exception.InvalidGoalArgumentException;
import com.amalitech.analytics.service.model.SkillSnapShot;
import com.amalitech.analytics.service.model.UserGoal;
import com.amalitech.analytics.service.model.UserSkillProgress;
import com.amalitech.analytics.service.model.enums.GoalType;
import com.amalitech.analytics.service.repository.SkillSnapshotRepository;
import com.amalitech.analytics.service.repository.UserGoalRepository;
import com.amalitech.analytics.service.repository.UserSkillProgressRepository;
import com.amalitech.analytics.service.service.GoalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GoalServiceTest {

    @Mock private UserGoalRepository goalRepository;
    @Mock private UserSkillProgressRepository progressRepository;
    @Mock private SkillSnapshotRepository snapshotRepository;

    @InjectMocks
    private GoalService goalService;

    private UUID userId;
    private UUID skillId;
    private SkillSnapShot snapshot;
    private LocalDate deadline;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        skillId = UUID.randomUUID();
        deadline = LocalDate.now().plusDays(7);

        snapshot = SkillSnapShot.builder()
                .id(skillId)
                .name("Testing")
                .category("DevOps")
                .levelXpMap(Map.of("INTERMEDIATE", 1000L))
                .lastSyncedAt(LocalDateTime.now())
                .build();

        when(snapshotRepository.findById(skillId)).thenReturn(Optional.of(snapshot));
    }

    @Test
    @DisplayName("createGoal should initialize TARGET_XP goal correctly")
    void createGoal_TargetXpSuccess() {
        UserSkillProgress progress = new UserSkillProgress();
        progress.setTotalXpEarned(500);
        progress.setTasksCompleted(10);
        when(progressRepository.findByUserIdAndSkillId(userId, skillId)).thenReturn(Optional.of(progress));

        CreateGoalRequestDTO request = new CreateGoalRequestDTO(skillId, GoalType.TARGET_XP, 1500, deadline);

        UserGoal savedGoal = new UserGoal(userId, skillId, snapshot.getName(), GoalType.TARGET_XP, 1500, 500, deadline, null);
        savedGoal.setId(UUID.randomUUID());
        when(goalRepository.save(any(UserGoal.class))).thenReturn(savedGoal);

        UserGoalDTO result = goalService.createGoal(userId, request);

        assertThat(result.targetValue()).isEqualTo(1500);
        assertThat(result.initialValue()).isEqualTo(500);
        assertThat(result.currentValue()).isEqualTo(500);
        assertThat(result.skillName()).isEqualTo("Testing");
        verify(goalRepository).save(any(UserGoal.class));
    }

    @Test
    @DisplayName("createGoal should initialize TASKS_COMPLETED goal correctly")
    void createGoal_TasksCompletedSuccess() {
        UserSkillProgress progress = new UserSkillProgress();
        progress.setTotalXpEarned(500);
        progress.setTasksCompleted(10);
        when(progressRepository.findByUserIdAndSkillId(userId, skillId)).thenReturn(Optional.of(progress));

        CreateGoalRequestDTO request = new CreateGoalRequestDTO(skillId, GoalType.TASKS_COMPLETED, 50, deadline);

        UserGoal savedGoal = new UserGoal(
                userId, skillId, snapshot.getName(), GoalType.TASKS_COMPLETED, 50, 10, deadline, null
        );
        savedGoal.setId(UUID.randomUUID());
        savedGoal.setInitialValue(10);
        savedGoal.setCurrentValue(10);
        savedGoal.setTargetValue(50);

        when(goalRepository.save(any(UserGoal.class))).thenReturn(savedGoal);

        UserGoalDTO result = goalService.createGoal(userId, request);

        assertThat(result.targetValue()).isEqualTo(50);
        assertThat(result.initialValue()).isEqualTo(10);
        assertThat(result.currentValue()).isEqualTo(10);
    }

    @Test
    @DisplayName("createGoal should initialize goal with 0 initial value if progress is new")
    void createGoal_NewProgress() {
        when(progressRepository.findByUserIdAndSkillId(userId, skillId)).thenReturn(Optional.empty());

        CreateGoalRequestDTO request = new CreateGoalRequestDTO(skillId, GoalType.TARGET_XP, 1000, deadline);

        UserGoal savedGoal = new UserGoal(
                userId, skillId, snapshot.getName(), GoalType.TARGET_XP, 1000, 0, deadline, null
        );
        savedGoal.setId(UUID.randomUUID());
        savedGoal.setInitialValue(0);
        savedGoal.setCurrentValue(0);
        savedGoal.setTargetValue(1000);

        when(goalRepository.save(any(UserGoal.class))).thenReturn(savedGoal);

        UserGoalDTO result = goalService.createGoal(userId, request);

        assertThat(result.initialValue()).isEqualTo(0);
        assertThat(result.currentValue()).isEqualTo(0);
        verify(goalRepository).save(any(UserGoal.class));
    }

    @Test
    @DisplayName("createGoal should throw EntityNotFoundException if SkillSnapshot is missing")
    void createGoal_MissingSnapshot() {
        when(snapshotRepository.findById(skillId)).thenReturn(Optional.empty());
        CreateGoalRequestDTO request = new CreateGoalRequestDTO(skillId, GoalType.TARGET_XP, 1000, deadline);

        assertThatThrownBy(() -> goalService.createGoal(userId, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Skill not found");
    }

    @Test
    @DisplayName("createGoal should throw InvalidGoalArgumentException if target is not greater than current")
    void createGoal_InvalidTargetValue() {
        UserSkillProgress progress = new UserSkillProgress();
        progress.setTotalXpEarned(500);
        when(progressRepository.findByUserIdAndSkillId(userId, skillId)).thenReturn(Optional.of(progress));

        CreateGoalRequestDTO request = new CreateGoalRequestDTO(skillId, GoalType.TARGET_XP, 500, deadline);

        assertThatThrownBy(() -> goalService.createGoal(userId, request))
                .isInstanceOf(InvalidGoalArgumentException.class)
                .hasMessageContaining("Target value must be greater than current value");
    }

    @Test
    @DisplayName("listGoals should return all goals for a user")
    void listGoals_Success() {
        UserGoal goal1 = new UserGoal();
        goal1.setId(UUID.randomUUID());
        goal1.setTargetValue(100);
        goal1.setInitialValue(10);
        goal1.setCurrentValue(10);
        goal1.setGoalType(GoalType.TARGET_XP);
        goal1.setSkillName("Mock Skill A");
        goal1.setUserId(userId);
        goal1.setSkillId(UUID.randomUUID());

        UserGoal goal2 = new UserGoal();
        goal2.setId(UUID.randomUUID());
        goal2.setTargetValue(50);
        goal2.setInitialValue(5);
        goal2.setCurrentValue(5);
        goal2.setGoalType(GoalType.TASKS_COMPLETED);
        goal2.setSkillName("Mock Skill B");
        goal2.setUserId(userId);
        goal2.setSkillId(UUID.randomUUID());

        when(goalRepository.findByUserId(userId)).thenReturn(List.of(goal1, goal2));

        List<UserGoalDTO> result = goalService.listGoals(userId);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).targetValue()).isEqualTo(100);
        verify(goalRepository).findByUserId(userId);
    }

    @Test
    @DisplayName("getGoal should return DTO when goal exists for user")
    void getGoal_Success() {
        UUID goalId = UUID.randomUUID();
        UserGoal goal = new UserGoal();
        goal.setId(goalId);

        goal.setTargetValue(100);
        goal.setInitialValue(0);
        goal.setCurrentValue(0);
        goal.setGoalType(GoalType.TARGET_XP);
        goal.setSkillName("Mock Skill");
        goal.setUserId(userId);
        goal.setSkillId(skillId);

        when(goalRepository.findByIdAndUserId(goalId, userId)).thenReturn(Optional.of(goal));

        UserGoalDTO result = goalService.getGoal(userId, goalId);

        assertThat(result.id()).isEqualTo(goalId);
        assertThat(result.targetValue()).isEqualTo(100);
    }

    @Test
    @DisplayName("getGoal should throw EntityNotFoundException when goal does not exist")
    void getGoal_NotFound() {
        UUID goalId = UUID.randomUUID();
        when(goalRepository.findByIdAndUserId(goalId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> goalService.getGoal(userId, goalId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Goal not found");
    }

    @Test
    @DisplayName("deleteGoal should delete the goal when it exists for the user")
    void deleteGoal_Success() {
        UUID goalId = UUID.randomUUID();
        UserGoal goal = new UserGoal();
        goal.setId(goalId);
        when(goalRepository.findByIdAndUserId(goalId, userId)).thenReturn(Optional.of(goal));

        goalService.deleteGoal(userId, goalId);

        verify(goalRepository).delete(goal);
    }

    @Test
    @DisplayName("deleteGoal should throw EntityNotFoundException when goal does not exist")
    void deleteGoal_NotFound() {
        UUID goalId = UUID.randomUUID();
        when(goalRepository.findByIdAndUserId(goalId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> goalService.deleteGoal(userId, goalId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Goal not found");
        verify(goalRepository, never()).delete(any());
    }
}
