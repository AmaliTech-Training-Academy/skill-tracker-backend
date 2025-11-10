package com.amalitech.analytics.service.service.interfaces;
import com.amalitech.analytics.service.dto.CreateGoalRequestDTO;
import com.amalitech.analytics.service.dto.UserGoalDTO;

import java.util.List;
import java.util.UUID;

public interface GoalServiceInterface {
    UserGoalDTO createGoal(UUID userId, CreateGoalRequestDTO request);
    List<UserGoalDTO> listGoals(UUID userId);
    UserGoalDTO getGoal(UUID userId, UUID goalId);
    void deleteGoal(UUID userId, UUID goalId);
}
