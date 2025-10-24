package com.amalitech.task.service.repository;

import com.amalitech.task.service.model.TaskDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaskDefinitionRepository extends JpaRepository<TaskDefinition, UUID> {

    /**
     * Find task definition by skill and title
     */
    @Query("SELECT td FROM TaskDefinition td " +
            "LEFT JOIN FETCH td.skill " +
            "WHERE td.skill.id = :skillId " +
            "AND LOWER(td.title) = LOWER(:title)")
    Optional<TaskDefinition> findBySkillIdAndTitle(
            @Param("skillId") UUID skillId,
            @Param("title") String title
    );

    /**
     * Find all task definitions for a specific skill
     */
    @Query("SELECT td FROM TaskDefinition td " +
            "WHERE td.skill.id = :skillId " +
            "ORDER BY td.createdAt DESC")
    List<TaskDefinition> findBySkillId(@Param("skillId") UUID skillId);

    /**
     * Find task definitions with latest version info
     */
    @Query("SELECT td FROM TaskDefinition td " +
            "LEFT JOIN FETCH td.skill " +
            "WHERE td.id = :definitionId")
    Optional<TaskDefinition> findByIdWithSkill(@Param("definitionId") UUID definitionId);

    /**
     * Check if task definition exists for skill and title
     */
    @Query("SELECT CASE WHEN COUNT(td) > 0 THEN true ELSE false END " +
            "FROM TaskDefinition td " +
            "WHERE td.skill.id = :skillId AND LOWER(td.title) = LOWER(:title)")
    boolean existsBySkillIdAndTitle(
            @Param("skillId") UUID skillId,
            @Param("title") String title
    );
}