package com.amalitech.task.service.repository;

import com.amalitech.task.service.model.ProgrammingLanguage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProgrammingLanguageRepository extends JpaRepository<ProgrammingLanguage, Long> {

    /**
     * Find programming language by name (case-insensitive)
     */
    @Query("SELECT pl FROM ProgrammingLanguage pl WHERE LOWER(pl.name) = LOWER(:name)")
    Optional<ProgrammingLanguage> findByName(String name);

    /**
     * Check if language exists by name
     */
    @Query("SELECT CASE WHEN COUNT(pl) > 0 THEN true ELSE false END FROM ProgrammingLanguage pl WHERE LOWER(pl.name) = LOWER(:name)")
    boolean existsByName(String name);

    /**
     * Get all languages ordered by name
     */
    @Query("SELECT pl FROM ProgrammingLanguage pl ORDER BY pl.name")
    List<ProgrammingLanguage> findAllOrderByName();
}