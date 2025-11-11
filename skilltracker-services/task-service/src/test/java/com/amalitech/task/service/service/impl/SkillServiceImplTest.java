package com.amalitech.task.service.service.impl;

import com.amalitech.task.service.exception.ResourceNotFoundException;
import com.amalitech.task.service.model.view.SkillView;
import com.amalitech.task.service.repository.SkillViewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SkillServiceImplTest {

    @Mock
    private SkillViewRepository skillViewRepository;

    @InjectMocks
    private SkillServiceImpl skillService;

    private SkillView testSkill;
    private UUID skillId;

    @BeforeEach
    void setUp() {
        skillId = UUID.randomUUID();
        testSkill = new SkillView();
        testSkill.setId(skillId);
        testSkill.setName("PYTHON");
    }

    @Test
    void testGetSkillByName_Success() {
        when(skillViewRepository.findByName("PYTHON")).thenReturn(Optional.of(testSkill));

        SkillView result = skillService.getSkillByName("PYTHON");

        assertNotNull(result);
        assertEquals("PYTHON", result.getName());
        assertEquals(skillId, result.getId());
        verify(skillViewRepository, times(1)).findByName("PYTHON");
    }

    @Test
    void testGetSkillByName_NotFound() {
        when(skillViewRepository.findByName("NONEXISTENT")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            skillService.getSkillByName("NONEXISTENT");
        });

        verify(skillViewRepository, times(1)).findByName("NONEXISTENT");
    }

    @Test
    void testGetSkillByName_CacheabilityFirstCall() {
        testSkill.setName("JAVA");
        when(skillViewRepository.findByName("JAVA")).thenReturn(Optional.of(testSkill));

        SkillView result = skillService.getSkillByName("JAVA");

        assertNotNull(result);
        assertEquals("JAVA", result.getName());
        verify(skillViewRepository, times(1)).findByName("JAVA");
    }

    @Test
    void testGetSkillByName_NotFoundExceptionMessage() {
        when(skillViewRepository.findByName("UNKNOWN")).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            skillService.getSkillByName("UNKNOWN");
        });

        assertTrue(exception.getMessage().contains("UNKNOWN"));
    }
}
