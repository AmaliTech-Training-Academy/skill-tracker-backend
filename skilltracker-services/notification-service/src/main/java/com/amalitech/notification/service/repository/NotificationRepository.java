package com.amalitech.notification.service.repository;

import com.amalitech.notification.service.document.NotificationDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends MongoRepository<NotificationDocument, String> {

    Page<NotificationDocument> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<NotificationDocument> findByUserIdAndReadFalseOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<NotificationDocument> findByUserIdAndTypeOrderByCreatedAtDesc(UUID userId, String type, Pageable pageable);

    @Deprecated
    List<NotificationDocument> findByUserIdAndReadFalseOrderByCreatedAtDesc(UUID userId);

    Optional<NotificationDocument> findByIdAndUserId(String id, UUID userId);

    long countByUserIdAndReadFalse(UUID userId);

    List<NotificationDocument> findBySubmissionId(UUID submissionId);

    @Query("{ 'userId': ?0, 'read': false }")
    @Update("{ '$set': { 'read': true } }")
    long updateAllUnreadToReadByUserId(UUID userId);
}