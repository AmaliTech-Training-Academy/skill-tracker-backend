package com.amalitech.task.service.mapper;

import com.amalitech.task.service.dto.TaskDTO;
import com.amalitech.task.service.dto.response.AdminTaskDetailResponse;
import com.amalitech.task.service.dto.response.AdminTaskSummaryResponse;
import com.amalitech.task.service.mapper.impl.TaskMapperImpl;
import com.amalitech.task.service.model.Task;
import com.amalitech.task.service.model.TaskDefinition;
import com.amalitech.task.service.model.content.impl.CodingTaskContent;
import com.amalitech.task.service.model.enums.TaskDifficulty;
import com.amalitech.task.service.model.enums.TaskType;
import com.amalitech.task.service.model.view.SkillView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TaskMapperTest {

    private TaskMapper taskMapper;

    private UUID taskId;
    private UUID skillId;
    private UUID taskDefId;
    private Task testTask;

    @BeforeEach
    void setUp() {
        taskMapper = new TaskMapperImpl();

        taskId = UUID.randomUUID();
        skillId = UUID.randomUUID();
        taskDefId = UUID.randomUUID();

        SkillView skill = new SkillView();
        skill.setId(skillId);
        skill.setName("Java Programming");

        TaskDefinition definition = new TaskDefinition();
        definition.setId(taskDefId);
        definition.setSkill(skill);

        testTask = Task.builder()
                .id(taskId)
                .title("Implement Fibonacci")
                .description("Write a function to calculate Fibonacci numbers")
                .type(TaskType.CODING)
                .difficulty(TaskDifficulty.INTERMEDIATE)
                .xpReward(50)
                .estimatedDurationInMinutes(30)
                .isPublished(true)
                .version(1)
                .taskDefinition(definition)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        CodingTaskContent content = new CodingTaskContent();
        testTask.setContent(content);
    }

    @Test
    void testToDTO_Success() {
        TaskDTO result = taskMapper.toDTO(testTask);

        assertNotNull(result);
        assertEquals(taskId, result.getId());
        assertEquals("Implement Fibonacci", result.getTitle());
        assertEquals("Write a function to calculate Fibonacci numbers", result.getDescription());
        assertEquals(TaskType.CODING, result.getType());
        assertEquals(TaskDifficulty.INTERMEDIATE, result.getDifficulty());
        assertEquals(50, result.getXpReward());
        assertEquals(30, result.getEstimatedDuration());
        assertEquals("Java Programming", result.getSkillName());
        assertEquals(1, result.getVersion());
        assertNotNull(result.getContent());
    }

    @Test
    void testToDTO_Null() {
        TaskDTO result = taskMapper.toDTO(null);

        assertNull(result);
    }

    @Test
    void testToDTO_WithoutSkill() {
        testTask.setTaskDefinition(null);

        TaskDTO result = taskMapper.toDTO(testTask);

        assertNotNull(result);
        assertEquals(taskId, result.getId());
        assertNull(result.getSkillName());
    }

    @Test
    void testToDTO_WithoutTaskDefinition() {
        testTask.getTaskDefinition().setSkill(null);

        TaskDTO result = taskMapper.toDTO(testTask);

        assertNotNull(result);
        assertEquals(taskId, result.getId());
        assertNull(result.getSkillName());
    }

    @Test
    void testToAdminSummaryDTO_Success() {
        AdminTaskSummaryResponse result = taskMapper.toAdminSummaryDTO(testTask);

        assertNotNull(result);
        assertEquals(taskId, result.taskId());
        assertEquals("Implement Fibonacci", result.title());
        assertEquals("CODING", result.type());
        assertEquals("INTERMEDIATE", result.difficulty());
        assertEquals(1, result.version());
        assertTrue(result.isPublished());
        assertNotNull(result.createdAt());
    }

    @Test
    void testToAdminSummaryDTO_Null() {
        AdminTaskSummaryResponse result = taskMapper.toAdminSummaryDTO(null);

        assertNull(result);
    }

    @Test
    void testToAdminSummaryDTO_NotPublished() {
        testTask.setIsPublished(false);

        AdminTaskSummaryResponse result = taskMapper.toAdminSummaryDTO(testTask);

        assertNotNull(result);
        assertFalse(result.isPublished());
    }

    @Test
    void testToAdminDetailDTO_Success() {
        AdminTaskDetailResponse result = taskMapper.toAdminDetailDTO(testTask);

        assertNotNull(result);
        assertEquals(taskId, result.taskId());
        assertEquals(taskDefId, result.taskDefinitionId());
        assertEquals("Implement Fibonacci", result.title());
        assertEquals("Write a function to calculate Fibonacci numbers", result.description());
        assertEquals("Java Programming", result.skillName());
        assertEquals("CODING", result.type());
        assertEquals("INTERMEDIATE", result.difficulty());
        assertEquals(1, result.version());
        assertTrue(result.isPublished());
        assertEquals(50, result.xpReward());
        assertEquals(30, result.estimatedDurationInMinutes());
        assertNotNull(result.content());
        assertNotNull(result.createdAt());
        assertNotNull(result.updatedAt());
    }

    @Test
    void testToAdminDetailDTO_Null() {
        AdminTaskDetailResponse result = taskMapper.toAdminDetailDTO(null);

        assertNull(result);
    }

    @Test
    void testToAdminDetailDTO_WithoutTaskDefinition() {
        testTask.setTaskDefinition(null);

        AdminTaskDetailResponse result = taskMapper.toAdminDetailDTO(testTask);

        assertNotNull(result);
        assertNull(result.taskDefinitionId());
        assertNull(result.skillName());
    }

    @Test
    void testToAdminDetailDTO_WithoutSkill() {
        testTask.getTaskDefinition().setSkill(null);

        AdminTaskDetailResponse result = taskMapper.toAdminDetailDTO(testTask);

        assertNotNull(result);
        assertEquals(taskDefId, result.taskDefinitionId());
        assertNull(result.skillName());
    }

    @Test
    void testToAdminDetailDTO_EssayTask() {
        testTask.setType(TaskType.ESSAY);
        testTask.setDifficulty(TaskDifficulty.BEGINNER);

        AdminTaskDetailResponse result = taskMapper.toAdminDetailDTO(testTask);

        assertNotNull(result);
        assertEquals("ESSAY", result.type());
        assertEquals("BEGINNER", result.difficulty());
    }

    @Test
    void testToAdminDetailDTO_WithLowXPReward() {
        testTask.setXpReward(10);

        AdminTaskDetailResponse result = taskMapper.toAdminDetailDTO(testTask);

        assertNotNull(result);
        assertEquals(10, result.xpReward());
    }

    @Test
    void testToAdminDetailDTO_WithLongDuration() {
        testTask.setEstimatedDurationInMinutes(120);

        AdminTaskDetailResponse result = taskMapper.toAdminDetailDTO(testTask);

        assertNotNull(result);
        assertEquals(120, result.estimatedDurationInMinutes());
    }
}
