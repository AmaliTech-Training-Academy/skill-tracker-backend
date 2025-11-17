package com.amalitech.task.service.repository;

import com.amalitech.task.service.dto.response.McqResponseDTO;
import com.amalitech.task.service.model.Task;
import com.amalitech.task.service.model.enums.TaskDifficulty;
import com.amalitech.task.service.model.enums.TaskType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID>,
        JpaSpecificationExecutor<Task> {

    List<Task> findByUserIdAndType(String userId, TaskType type);
    /**
     * Finds tasks by joining through TaskDefinition to get the skill.
     * This is the correct, normalized query.
     */
    @Query("SELECT t FROM Task t " +
            "JOIN FETCH t.taskDefinition td " +
            "WHERE td.skill.id = :skillId " +
            "AND t.difficulty = :difficulty " +
            "AND t.isPublished = :isPublished " +
            "ORDER BY t.createdAt DESC")
    List<Task> findBySkillAndDifficulty(
            @Param("skillId") UUID skillId,
            @Param("difficulty") TaskDifficulty difficulty,
            @Param("isPublished") Boolean isPublished,
            Pageable pageable
    );

    /**
     * Counts tasks by joining through TaskDefinition to get the skill.
     * This is the correct, normalized query.
     */
    @Query("SELECT COUNT(t) FROM Task t " +
            "JOIN t.taskDefinition td " +
            "WHERE td.skill.id = :skillId " +
            "AND t.difficulty = :difficulty " +
            "AND t.isPublished = :isPublished")
    long countBySkillAndDifficulty(
            @Param("skillId") UUID skillId,
            @Param("difficulty") TaskDifficulty difficulty,
            @Param("isPublished") Boolean isPublished
    );

    /**
    * Finds tasks by skill, difficulty, and type.
    */
    @Query("SELECT t FROM Task t " +
    "JOIN FETCH t.taskDefinition td " +
    "WHERE td.skill.id = :skillId " +
    "AND t.difficulty = :difficulty " +
    "AND t.type = :type " +
    "AND t.isPublished = :isPublished " +
    "ORDER BY t.createdAt DESC")
    List<Task> findBySkillIdAndDifficultyAndType(
    @Param("skillId") UUID skillId,
    @Param("difficulty") TaskDifficulty difficulty,
    @Param("type") TaskType type,
    @Param("isPublished") boolean isPublished,
    Pageable pageable
    );

    /**
     * Counts tasks by skill, difficulty, and type.
     */
    @Query("SELECT COUNT(t) FROM Task t " +
            "JOIN t.taskDefinition td " +
            "WHERE td.skill.id = :skillId " +
            "AND t.difficulty = :difficulty " +
            "AND t.type = :type " +
            "AND t.isPublished = :isPublished")
    long countBySkillAndDifficultyAndType(
            @Param("skillId") UUID skillId,
            @Param("difficulty") TaskDifficulty difficulty,
            @Param("type") TaskType type,
            @Param("isPublished") boolean isPublished
    );

    @Query("SELECT t FROM Task t " +
            "WHERE t.taskDefinition.skill.id IN :skillIds " +
            "AND t.isPublished = true " +
            "AND t.id NOT IN :excludeTaskIds")
    Page<Task> findPendingTasksBySkills(
            @Param("skillIds") Set<UUID> skillIds,
            @Param("excludeTaskIds") Set<UUID> excludeTaskIds,
            Pageable pageable
    );

    @Query("SELECT t FROM Task t " +
            "WHERE t.id IN :taskIds " +
            "AND t.isPublished = true")
    Page<Task> findCompletedTasksByIds(
            @Param("taskIds") Set<UUID> taskIds,
            Pageable pageable
    );


    @Query("SELECT t FROM Task t " +
            "WHERE t.taskDefinition.skill.id IN :skillIds " +
            "AND t.isPublished = true")
    Page<Task> findAllTasksBySkills(
            @Param("skillIds") Set<UUID> skillIds,
            Pageable pageable
    );
}