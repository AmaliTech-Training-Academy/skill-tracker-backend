package com.amalitech.notification.service.repository;

import com.amalitech.notification.service.document.NotificationDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends MongoRepository<NotificationDocument, String> {
    
    Page<NotificationDocument> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    
    List<NotificationDocument> findByUserIdAndReadFalseOrderByCreatedAtDesc(UUID userId);
    
    Page<NotificationDocument> findByUserIdAndReadFalseOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    
    List<NotificationDocument> findBySubmissionId(UUID submissionId);
    
    Page<NotificationDocument> findByUserIdAndTypeOrderByCreatedAtDesc(UUID userId, String type, Pageable pageable);
}
