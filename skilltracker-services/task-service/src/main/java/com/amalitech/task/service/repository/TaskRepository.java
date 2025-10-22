package com.amalitech.task.service.repository;

import com.amalitech.task.service.model.Task;
import com.amalitech.task.service.model.enums.TaskDifficulty;
import com.amalitech.task.service.model.enums.TaskType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {

//    /**
//     * Random tasks for personalized learning
//     */
//    @Query(value = "SELECT * FROM tasks t " +
//            "WHERE t.skill_id = :skillId " +
//            "AND t.difficulty = CAST(:difficulty AS VARCHAR) " +
//            "AND t.is_published = true " +
//            "ORDER BY RANDOM() " +
//            "LIMIT :limit",
//            nativeQuery = true)
//    List<Task> findRandomTasksBySkillAndDifficulty(
//            @Param("skillId") UUID skillId,
//            @Param("difficulty") String difficulty,
//            @Param("limit") int limit
//    );
//
//    /**
//     * Find tasks by skill, type, and published status
//     */
//    List<Task> findBySkillIdAndTypeAndIsPublished(
//            UUID skillId, TaskType type, Boolean isPublished
//    );
//
//    /**
//     * Find task by ID with task definition
//     */
//    @Query("SELECT t FROM Task t " +
//            "LEFT JOIN FETCH t.taskDefinition td " +
//            "WHERE t.id = :taskId")
//    Optional<Task> findByIdWithDefinition(@Param("taskId") UUID taskId);
//
//    /**
//     * Find all tasks for a task definition, ordered by version
//     */
//    @Query("SELECT t FROM Task t " +
//            "WHERE t.taskDefinition.id = :definitionId " +
//            "ORDER BY t.version DESC")
//    List<Task> findByTaskDefinitionIdOrderByVersionDesc(@Param("definitionId") UUID definitionId);
//
//    /**
//     * Find specific version of a task
//     */
//    Optional<Task> findByTaskDefinitionIdAndVersion(UUID definitionId, int version);
//
//    /**
//     * Find all published tasks
//     */
//    @Query("SELECT t FROM Task t WHERE t.isPublished = true ORDER BY t.createdAt DESC")
//    List<Task> findAllPublishedTasks();
//
//    /**
//     * Count published tasks
//     */
//    long countByIsPublished(Boolean isPublished);
//
//
//    /**
//     * Count tasks by type and published status
//     */
//    long countByTypeAndIsPublished(TaskType type, Boolean isPublished);
//
//    /**
//     * Count tasks by skill ID
//     */
//    long countByTaskDefinition_Skill_IdAndIsPublished(UUID skillId, Boolean isPublished);

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
}