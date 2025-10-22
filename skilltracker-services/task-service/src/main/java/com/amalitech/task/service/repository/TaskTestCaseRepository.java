package com.amalitech.task.service.repository;

import com.amalitech.task.service.model.TaskTestCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TaskTestCaseRepository extends JpaRepository<TaskTestCase, UUID> {

    /**
     * Find all test cases for a specific task
     */
    @Query("SELECT tc FROM TaskTestCase tc " +
            "WHERE tc.task.id = :taskId " +
            "ORDER BY tc.isHidden ASC, tc.weight DESC")
    List<TaskTestCase> findByTaskId(@Param("taskId") UUID taskId);

    /**
     * Find only visible (non-hidden) test cases for a task
     */
    @Query("SELECT tc FROM TaskTestCase tc " +
            "WHERE tc.task.id = :taskId " +
            "AND tc.isHidden = false " +
            "ORDER BY tc.weight DESC")
    List<TaskTestCase> findVisibleTestCasesByTaskId(@Param("taskId") UUID taskId);

    /**
     * Find only hidden test cases for a task
     */
    @Query("SELECT tc FROM TaskTestCase tc " +
            "WHERE tc.task.id = :taskId " +
            "AND tc.isHidden = true " +
            "ORDER BY tc.weight DESC")
    List<TaskTestCase> findHiddenTestCasesByTaskId(@Param("taskId") UUID taskId);

    /**
     * Find all test cases ordered by visibility (visible first)
     */
    @Query("SELECT tc FROM TaskTestCase tc " +
            "WHERE tc.task.id = :taskId " +
            "ORDER BY tc.isHidden ASC, tc.weight DESC, tc.createdAt ASC")
    List<TaskTestCase> findByTaskIdOrderByVisibility(@Param("taskId") UUID taskId);

    /**
     * Count total test cases for a task
     */
    @Query("SELECT COUNT(tc) FROM TaskTestCase tc WHERE tc.task.id = :taskId")
    long countByTaskId(@Param("taskId") UUID taskId);

    /**
     * Count visible test cases for a task
     */
    @Query("SELECT COUNT(tc) FROM TaskTestCase tc " +
            "WHERE tc.task.id = :taskId AND tc.isHidden = false")
    long countVisibleByTaskId(@Param("taskId") UUID taskId);

    /**
     * Count hidden test cases for a task
     */
    @Query("SELECT COUNT(tc) FROM TaskTestCase tc " +
            "WHERE tc.task.id = :taskId AND tc.isHidden = true")
    long countHiddenByTaskId(@Param("taskId") UUID taskId);

    /**
     * Get total weight of all test cases for a task
     */
    @Query("SELECT SUM(tc.weight) FROM TaskTestCase tc WHERE tc.task.id = :taskId")
    Integer getTotalWeightByTaskId(@Param("taskId") UUID taskId);

    /**
     * Delete all test cases for a specific task
     */
    void deleteByTaskId(UUID taskId);

    /**
     * Check if test cases exist for a task
     */
    @Query("SELECT CASE WHEN COUNT(tc) > 0 THEN true ELSE false END " +
            "FROM TaskTestCase tc WHERE tc.task.id = :taskId")
    boolean existsByTaskId(@Param("taskId") UUID taskId);
}