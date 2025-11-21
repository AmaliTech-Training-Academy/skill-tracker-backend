package com.amalitech.user.service.controller;

import com.amalitech.common.security.dto.response.ApiResponse;
import com.amalitech.user.service.dto.UserSkillDto;
import com.amalitech.user.service.model.enums.DifficultyLevel;
import com.amalitech.user.service.service.SkillService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SkillControllerTest {

    @Mock
    private SkillService skillService;

    @InjectMocks
    private SkillController skillController;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    @Test
    void getMySkills_ShouldReturnUserSkillsWithOkStatus() {
        UserSkillDto skill1 = new UserSkillDto(
                UUID.randomUUID(), "Java", DifficultyLevel.INTERMEDIATE, LocalDateTime.now()
        );
        UserSkillDto skill2 = new UserSkillDto(
                UUID.randomUUID(), "Python", DifficultyLevel.BEGINNER, LocalDateTime.now()
        );

        when(skillService.getUserSkills(userId)).thenReturn(List.of(skill1, skill2));

        ResponseEntity<ApiResponse<List<UserSkillDto>>> response = skillController.getMySkills(userId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().getData().size());
        assertEquals("Java", response.getBody().getData().get(0).skillName());
        assertEquals("Python", response.getBody().getData().get(1).skillName());
        verify(skillService, times(1)).getUserSkills(userId);
    }

    @Test
    void getMySkills_ShouldReturnEmptyListWhenUserHasNoSkills() {
        when(skillService.getUserSkills(userId)).thenReturn(Collections.emptyList());

        ResponseEntity<ApiResponse<List<UserSkillDto>>> response = skillController.getMySkills(userId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getData().isEmpty());
        verify(skillService, times(1)).getUserSkills(userId);
    }

    @Test
    void getMySkills_ShouldUseProvidedUserId() {
        UUID expectedUserId = UUID.randomUUID();

        when(skillService.getUserSkills(expectedUserId)).thenReturn(Collections.emptyList());

        skillController.getMySkills(expectedUserId);

        verify(skillService).getUserSkills(expectedUserId);
    }

    @Test
    void getMySkills_ShouldReturnCorrectSkillDetails() {
        UUID skillId = UUID.randomUUID();
        LocalDateTime selectedAt = LocalDateTime.now();
        UserSkillDto skill = new UserSkillDto(skillId, "JavaScript", DifficultyLevel.ADVANCED, selectedAt);

        when(skillService.getUserSkills(userId)).thenReturn(List.of(skill));

        ResponseEntity<ApiResponse<List<UserSkillDto>>> response = skillController.getMySkills(userId);

        UserSkillDto result = response.getBody().getData().get(0);
        assertEquals(skillId, result.skillId());
        assertEquals("JavaScript", result.skillName());
        assertEquals(DifficultyLevel.ADVANCED, result.difficultyLevel());
        assertEquals(selectedAt, result.selectedAt());
    }
}
