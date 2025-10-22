package com.amalitech.task.service.repository;

import com.amalitech.task.service.model.TaskStarterCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaskStarterCodeRepository extends JpaRepository<TaskStarterCode, UUID> {

    /**
     * Find all starter codes for a specific task
     */
    @Query("SELECT sc FROM TaskStarterCode sc " +
            "LEFT JOIN FETCH sc.programmingLanguage " +
            "WHERE sc.task.id = :taskId " +
            "ORDER BY sc.programmingLanguage.name")
    List<TaskStarterCode> findByTaskId(@Param("taskId") UUID taskId);

    /**
     * Find starter code for a specific task and programming language
     */
    @Query("SELECT sc FROM TaskStarterCode sc " +
            "LEFT JOIN FETCH sc.programmingLanguage " +
            "WHERE sc.task.id = :taskId " +
            "AND sc.programmingLanguage.id = :languageId")
    Optional<TaskStarterCode> findByTaskIdAndLanguageId(
            @Param("taskId") UUID taskId,
            @Param("languageId") Long languageId
    );

    /**
     * Check if starter code exists for task and language combination
     */
    @Query("SELECT CASE WHEN COUNT(sc) > 0 THEN true ELSE false END " +
            "FROM TaskStarterCode sc " +
            "WHERE sc.task.id = :taskId AND sc.programmingLanguage.id = :languageId")
    boolean existsByTaskIdAndLanguageId(
            @Param("taskId") UUID taskId,
            @Param("languageId") Long languageId
    );

    /**
     * Delete all starter codes for a specific task
     */
    void deleteByTaskId(UUID taskId);

    /**
     * Find all starter codes for a specific programming language
     */
    @Query("SELECT sc FROM TaskStarterCode sc WHERE sc.programmingLanguage.id = :languageId")
    List<TaskStarterCode> findByLanguageId(@Param("languageId") Long languageId);
}