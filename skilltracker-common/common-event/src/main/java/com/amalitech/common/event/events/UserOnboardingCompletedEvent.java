package com.amalitech.common.event.events;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.UUID;

/**
 * Event published when a user completes onboarding with skill selections.
 * Triggers personalized task generation based on selected skills and their supported task types.
 */
@Data
@Builder
public class UserOnboardingCompletedEvent {

    /**
     * Unique identifier of the user who completed onboarding.
     */
    private UUID userId;

    /**
     * List of skills selected by the user during onboarding.
     */
    private List<SkillSelectionData> selectedSkills;

    @Data
    @Builder
    public static class SkillSelectionData {
        private UUID skillId;
        private String skillName;
        private String difficultyLevel;
        private List<String> supportedTaskTypes;
    }
}
