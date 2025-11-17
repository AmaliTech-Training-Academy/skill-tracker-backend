package com.amalitech.notification.service.controller;

import com.amalitech.notification.service.document.NotificationDocument;
import com.amalitech.notification.service.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationRepository notificationRepository;

    @GetMapping
    public ResponseEntity<Page<NotificationDocument>> getAllNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        
        UUID userId = extractUserIdFromAuth(authentication);
        Pageable pageable = PageRequest.of(page, size);
        Page<NotificationDocument> notifications = 
                notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/unread")
    public ResponseEntity<List<NotificationDocument>> getUnreadNotifications(
            Authentication authentication) {
        
        UUID userId = extractUserIdFromAuth(authentication);
        List<NotificationDocument> unreadNotifications = 
                notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId);
        
        return ResponseEntity.ok(unreadNotifications);
    }

    @GetMapping("/unread/paginated")
    public ResponseEntity<Page<NotificationDocument>> getUnreadNotificationsPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        
        UUID userId = extractUserIdFromAuth(authentication);
        Pageable pageable = PageRequest.of(page, size);
        Page<NotificationDocument> unreadNotifications = 
                notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId, pageable);
        
        return ResponseEntity.ok(unreadNotifications);
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotificationDocument> getNotification(
            @PathVariable String id,
            Authentication authentication) {
        
        Optional<NotificationDocument> notification = notificationRepository.findById(id);
        
        if (notification.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        UUID userId = extractUserIdFromAuth(authentication);
        if (!notification.get().getUserId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        return ResponseEntity.ok(notification.get());
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationDocument> markAsRead(
            @PathVariable String id,
            Authentication authentication) {
        
        Optional<NotificationDocument> notification = notificationRepository.findById(id);
        
        if (notification.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        UUID userId = extractUserIdFromAuth(authentication);
        if (!notification.get().getUserId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        NotificationDocument doc = notification.get();
        doc.setRead(true);
        NotificationDocument updated = notificationRepository.save(doc);
        
        log.info("Marked notification {} as read for user: {}", id, userId);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(
            @PathVariable String id,
            Authentication authentication) {
        
        Optional<NotificationDocument> notification = notificationRepository.findById(id);
        
        if (notification.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        UUID userId = extractUserIdFromAuth(authentication);
        if (!notification.get().getUserId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        notificationRepository.deleteById(id);
        log.info("Deleted notification {} for user: {}", id, userId);
        return ResponseEntity.noContent().build();
    }

    private UUID extractUserIdFromAuth(Authentication authentication) {
        String userIdStr = (String) authentication.getPrincipal();
        return UUID.fromString(userIdStr);
    }
}
