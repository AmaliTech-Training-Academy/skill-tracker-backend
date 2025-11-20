package com.amalitech.user.service.service.impl;

import com.amalitech.user.service.dto.UserSkillDto;
import com.amalitech.user.service.model.Skill;
import com.amalitech.user.service.model.User;
import com.amalitech.user.service.model.UserSkill;
import com.amalitech.user.service.model.enums.DifficultyLevel;
import com.amalitech.user.service.repository.UserSkillRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SkillServiceImplTest {

    @Mock
    private UserSkillRepository userSkillRepository;

    @InjectMocks
    private SkillServiceImpl skillService;

    private UUID userId;
    private UUID skillId1;
    private UUID skillId2;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        skillId1 = UUID.randomUUID();
        skillId2 = UUID.randomUUID();
    }

    @Test
    void getUserSkills_ShouldReturnListOfUserSkillDtos() {
        User user = new User();
        user.setId(userId);

        Skill skill1 = new Skill();
        skill1.setId(skillId1);
        skill1.setName("Java");

        Skill skill2 = new Skill();
        skill2.setId(skillId2);
        skill2.setName("Python");

        UserSkill userSkill1 = new UserSkill();
        userSkill1.setUser(user);
        userSkill1.setSkill(skill1);
        userSkill1.setCurrentLevel(DifficultyLevel.INTERMEDIATE);
        userSkill1.setSelectedAt(LocalDateTime.now());

        UserSkill userSkill2 = new UserSkill();
        userSkill2.setUser(user);
        userSkill2.setSkill(skill2);
        userSkill2.setCurrentLevel(DifficultyLevel.BEGINNER);
        userSkill2.setSelectedAt(LocalDateTime.now().minusDays(1));

        when(userSkillRepository.findByUserId(userId)).thenReturn(List.of(userSkill1, userSkill2));

        List<UserSkillDto> result = skillService.getUserSkills(userId);

        assertEquals(2, result.size());
        assertEquals(skillId1, result.get(0).skillId());
        assertEquals("Java", result.get(0).skillName());
        assertEquals(DifficultyLevel.INTERMEDIATE, result.get(0).difficultyLevel());

        assertEquals(skillId2, result.get(1).skillId());
        assertEquals("Python", result.get(1).skillName());
        assertEquals(DifficultyLevel.BEGINNER, result.get(1).difficultyLevel());

        verify(userSkillRepository, times(1)).findByUserId(userId);
    }

    @Test
    void getUserSkills_ShouldReturnEmptyListWhenUserHasNoSkills() {
        when(userSkillRepository.findByUserId(userId)).thenReturn(Collections.emptyList());

        List<UserSkillDto> result = skillService.getUserSkills(userId);

        assertTrue(result.isEmpty());
        verify(userSkillRepository, times(1)).findByUserId(userId);
    }

    @Test
    void getUserSkills_ShouldMapUserSkillToDto() {
        User user = new User();
        user.setId(userId);

        Skill skill = new Skill();
        skill.setId(skillId1);
        skill.setName("TypeScript");

        LocalDateTime selectedAt = LocalDateTime.of(2024, 1, 15, 10, 30, 0);

        UserSkill userSkill = new UserSkill();
        userSkill.setUser(user);
        userSkill.setSkill(skill);
        userSkill.setCurrentLevel(DifficultyLevel.ADVANCED);
        userSkill.setSelectedAt(selectedAt);

        when(userSkillRepository.findByUserId(userId)).thenReturn(List.of(userSkill));

        List<UserSkillDto> result = skillService.getUserSkills(userId);

        assertEquals(1, result.size());
        UserSkillDto dto = result.get(0);
        assertEquals(skillId1, dto.skillId());
        assertEquals("TypeScript", dto.skillName());
        assertEquals(DifficultyLevel.ADVANCED, dto.difficultyLevel());
        assertEquals(selectedAt, dto.selectedAt());
    }

    @Test
    void getUserSkills_ShouldHandleMultipleSkillsWithDifferentLevels() {
        User user = new User();
        user.setId(userId);

        UserSkill skill1 = createUserSkill(user, "Java", DifficultyLevel.BEGINNER);
        UserSkill skill2 = createUserSkill(user, "Python", DifficultyLevel.INTERMEDIATE);
        UserSkill skill3 = createUserSkill(user, "JavaScript", DifficultyLevel.ADVANCED);

        when(userSkillRepository.findByUserId(userId)).thenReturn(List.of(skill1, skill2, skill3));

        List<UserSkillDto> result = skillService.getUserSkills(userId);

        assertEquals(3, result.size());
        assertEquals(DifficultyLevel.BEGINNER, result.get(0).difficultyLevel());
        assertEquals(DifficultyLevel.INTERMEDIATE, result.get(1).difficultyLevel());
        assertEquals(DifficultyLevel.ADVANCED, result.get(2).difficultyLevel());
    }

    @Test
    void getUserSkills_ShouldPreserveSkillDetails() {
        User user = new User();
        user.setId(userId);

        Skill skill = new Skill();
        skill.setId(skillId1);
        skill.setName("Go");

        LocalDateTime now = LocalDateTime.now();
        UserSkill userSkill = new UserSkill();
        userSkill.setUser(user);
        userSkill.setSkill(skill);
        userSkill.setCurrentLevel(DifficultyLevel.INTERMEDIATE);
        userSkill.setSelectedAt(now);

        when(userSkillRepository.findByUserId(userId)).thenReturn(List.of(userSkill));

        List<UserSkillDto> result = skillService.getUserSkills(userId);

        UserSkillDto dto = result.get(0);
        assertEquals(skill.getId(), dto.skillId());
        assertEquals(skill.getName(), dto.skillName());
        assertNotNull(dto.difficultyLevel());
        assertNotNull(dto.selectedAt());
    }

    private UserSkill createUserSkill(User user, String skillName, DifficultyLevel level) {
        Skill skill = new Skill();
        skill.setId(UUID.randomUUID());
        skill.setName(skillName);

        UserSkill userSkill = new UserSkill();
        userSkill.setUser(user);
        userSkill.setSkill(skill);
        userSkill.setCurrentLevel(level);
        userSkill.setSelectedAt(LocalDateTime.now());

        return userSkill;
    }
}
