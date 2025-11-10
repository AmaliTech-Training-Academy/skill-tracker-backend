package com.amalitech.task.service.service;

import com.amalitech.common.event.events.UserOnboardingCompletedEvent;
import com.amalitech.task.service.dto.request.BatchGenerationRequest;
import com.amalitech.task.service.dto.request.GenerateTaskRequest;
import com.amalitech.task.service.model.enums.TaskDifficulty;
import com.amalitech.task.service.model.enums.TaskType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskGenerationListenerTest {

    @Mock
    private TaskGenerationService taskGenerationService;

    @InjectMocks
    private TaskGenerationListener taskGenerationListener;

    private UUID userId;
    private UUID skillId;
    private BatchGenerationRequest batchRequest;
    private GenerateTaskRequest adminRequest;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        skillId = UUID.randomUUID();

        batchRequest = new BatchGenerationRequest(
                userId,
                "PYTHON",
                TaskDifficulty.BEGINNER,
                5,
                TaskType.CODING
        );

        adminRequest = new GenerateTaskRequest(
                userId,
                TaskType.CODING,
                "PYTHON",
                TaskDifficulty.INTERMEDIATE,
                "String Manipulation",
                "Python"
        );
    }

    @Test
    void testHandleBatchGenerationRequest() {
        taskGenerationListener.handleBatchGenerationRequest(batchRequest);

        verify(taskGenerationService, times(1)).processBatchGeneration(batchRequest);
    }

    @Test
    void testHandleAdminGenerationRequest() {
        taskGenerationListener.handleAdminGenerationRequest(adminRequest);

        verify(taskGenerationService, times(1)).processAdminGeneration(adminRequest);
    }

    @Test
    void testGenerateTasksAfterOnboarding() {
        List<UserOnboardingCompletedEvent.SkillSelectionData> skills = new ArrayList<>();
        UserOnboardingCompletedEvent.SkillSelectionData skillData = 
                UserOnboardingCompletedEvent.SkillSelectionData.builder()
                        .skillId(skillId)
                        .skillName("PYTHON")
                        .difficultyLevel("BEGINNER")
                        .supportedTaskTypes(new HashSet<>(List.of("CODING")))
                        .build();
        skills.add(skillData);

        UserOnboardingCompletedEvent event = UserOnboardingCompletedEvent.builder()
                .userId(userId)
                .selectedSkills(skills)
                .build();

        taskGenerationListener.generateTasksAfterOnboarding(event);

        verify(taskGenerationService, times(1)).generateTasksAfterOnboarding(event);
    }

    @Test
    void testHandleBatchGenerationRequest_EssayTask() {
        BatchGenerationRequest essayRequest = new BatchGenerationRequest(
                userId,
                "JAVA",
                TaskDifficulty.INTERMEDIATE,
                3,
                TaskType.ESSAY
        );

        taskGenerationListener.handleBatchGenerationRequest(essayRequest);

        verify(taskGenerationService, times(1)).processBatchGeneration(essayRequest);
    }

    @Test
    void testHandleAdminGenerationRequest_MultipleTypes() {
        taskGenerationListener.handleAdminGenerationRequest(adminRequest);

        verify(taskGenerationService, times(1)).processAdminGeneration(adminRequest);
    }

    @Test
    void testGenerateTasksAfterOnboarding_MultipleSkills() {
        List<UserOnboardingCompletedEvent.SkillSelectionData> skills = new ArrayList<>();
        skills.add(UserOnboardingCompletedEvent.SkillSelectionData.builder()
                .skillId(UUID.randomUUID())
                .skillName("PYTHON")
                .difficultyLevel("BEGINNER")
                .supportedTaskTypes(new HashSet<>(List.of("CODING", "ESSAY")))
                .build());
        skills.add(UserOnboardingCompletedEvent.SkillSelectionData.builder()
                .skillId(UUID.randomUUID())
                .skillName("JAVA")
                .difficultyLevel("INTERMEDIATE")
                .supportedTaskTypes(new HashSet<>(List.of("CODING")))
                .build());

        UserOnboardingCompletedEvent event = UserOnboardingCompletedEvent.builder()
                .userId(userId)
                .selectedSkills(skills)
                .build();

        taskGenerationListener.generateTasksAfterOnboarding(event);

        verify(taskGenerationService, times(1)).generateTasksAfterOnboarding(event);
    }

    @Test
    void testHandleBatchGenerationRequest_VerifysDelegation() {
        taskGenerationListener.handleBatchGenerationRequest(batchRequest);

        verify(taskGenerationService, times(1)).processBatchGeneration(eq(batchRequest));
    }

    @Test
    void testHandleAdminGenerationRequest_VerifiesDelegation() {
        taskGenerationListener.handleAdminGenerationRequest(adminRequest);

        verify(taskGenerationService, times(1)).processAdminGeneration(eq(adminRequest));
    }
}
