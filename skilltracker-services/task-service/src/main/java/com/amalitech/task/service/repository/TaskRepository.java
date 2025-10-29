package com.amalitech.task.service.repository;

import com.amalitech.task.service.model.Task;
import com.amalitech.task.service.model.enums.TaskDifficulty;
import com.amalitech.task.service.model.enums.TaskType;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {
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

    List<Task> findBySkillIdAndDifficultyAndType(UUID id, TaskDifficulty difficulty, TaskType neededType, boolean b, PageRequest of);
}