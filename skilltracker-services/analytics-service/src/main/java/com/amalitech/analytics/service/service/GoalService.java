package com.amalitech.analytics.service.service;
import com.amalitech.analytics.service.dto.CreateGoalRequestDTO;
import com.amalitech.analytics.service.dto.UserGoalDTO;
import com.amalitech.analytics.service.exception.EntityNotFoundException;
import com.amalitech.analytics.service.exception.InvalidGoalArgumentException;
import com.amalitech.analytics.service.model.SkillSnapShot;
import com.amalitech.analytics.service.model.UserGoal;
import com.amalitech.analytics.service.model.UserSkillProgress;
import com.amalitech.analytics.service.repository.SkillSnapshotRepository;
import com.amalitech.analytics.service.repository.UserGoalRepository;
import com.amalitech.analytics.service.repository.UserSkillProgressRepository;
import com.amalitech.analytics.service.service.interfaces.GoalServiceInterface;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GoalService implements GoalServiceInterface {

    private final UserGoalRepository goalRepository;
    private final UserSkillProgressRepository progressRepository;
    private final SkillSnapshotRepository snapshotRepository;

    @Override
    @Transactional
    public UserGoalDTO createGoal(UUID userId, CreateGoalRequestDTO request) {
        SkillSnapShot snapshot = snapshotRepository.findById(request.skillId())
                .orElseThrow(() -> new EntityNotFoundException("Skill not found: ", request.skillId()));


        UserSkillProgress progress = getSkillProgress(request.skillId(), userId);

        int initialValue = 0;
        int targetValue = request.targetValue();

        initialValue = switch (request.goalType()) {
            case TARGET_XP, REACH_LEVEL ->
                    progress.getTotalXpEarned();
            case TASKS_COMPLETED -> progress.getTasksCompleted();
        };

        if (targetValue <= initialValue) {
            throw new InvalidGoalArgumentException("Target value must be greater than current value.");
        }

        UserGoal goal = new UserGoal(
                userId,
                request.skillId(),
                snapshot.getName(),
                request.goalType(),
                targetValue,
                initialValue,
                request.deadline(),
                Instant.now()
        );

        UserGoal savedGoal = goalRepository.save(goal);
        return UserGoalDTO.fromEntity(savedGoal);
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
        UserSkillProgress skillProgress = progressRepository
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

    @Override
    @Transactional(readOnly = true)
    public List<UserGoalDTO> listGoals(UUID userId) {
        return goalRepository.findByUserId(userId).stream()
                .map(UserGoalDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public UserGoalDTO getGoal(UUID userId, UUID goalId) {
        return goalRepository.findByIdAndUserId(goalId, userId)
                .map(UserGoalDTO::fromEntity)
                .orElseThrow(() -> new EntityNotFoundException("Goal not found", goalId));
    }

    @Override
    @Transactional
    public void deleteGoal(UUID userId, UUID goalId) {
        UserGoal goal = goalRepository.findByIdAndUserId(goalId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Goal not found", goalId));
        goalRepository.delete(goal);
    }
}
