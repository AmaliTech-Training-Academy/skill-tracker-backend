package com.amalitech.task.service.repository;

import com.amalitech.task.service.model.UserLearningPath;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Repository
public interface UserLearningPathRepository extends JpaRepository<UserLearningPath, UUID> {
    @Transactional
    void deleteByCurrentSkill(String userId);
    UserLearningPath findByCurrentSkill(String skill);
}