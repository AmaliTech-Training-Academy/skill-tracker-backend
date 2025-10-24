package com.amalitech.task.service.repository;

import com.amalitech.task.service.model.view.SkillView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SkillViewRepository extends JpaRepository<SkillView, UUID> {

    /**
     * Find skill by name (case-insensitive)
     */
    @Query("SELECT s FROM SkillView s WHERE LOWER(s.name) = LOWER(:name)")
    Optional<SkillView> findByName(String name);

    /**
     * Find all skills (for admin/listing purposes)
     */
    @Query("SELECT s FROM SkillView s ORDER BY s.name")
    List<SkillView> findAllOrderByName();

    /**
     * Check if skill exists by name
     */
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM SkillView s WHERE LOWER(s.name) = LOWER(:name)")
    boolean existsByName(String name);
}