package com.amalitech.notification.service.controller;

import com.amalitech.notification.service.document.NotificationDocument;
import com.amalitech.notification.service.service.NotificationPersistenceService;
import com.amalitech.notification.service.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller for handling REST API requests for *historical* notifications.
 * Real-time notifications are pushed via WebSocket.
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationPersistenceService notificationPersistenceService;
    private final SecurityUtils securityUtils;

    @GetMapping
    public ResponseEntity<Page<NotificationDocument>> getAllNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        UUID userId = securityUtils.extractUserIdFromAuth(authentication);
        Pageable pageable = PageRequest.of(page, size);
        Page<NotificationDocument> notifications =
                notificationPersistenceService.getNotificationsForUser(userId, pageable);

        return ResponseEntity.ok(notifications);
    }

    /**
     * @deprecated This endpoint is dangerous as it can load unlimited documents.
     * It will be removed in v2. Use /unread/paginated.
     */
    @Deprecated
    @GetMapping("/unread")
    public ResponseEntity<?> getUnreadNotifications() {
        log.warn("/unread endpoint was called. This is deprecated.");
        return ResponseEntity.status(HttpStatus.GONE)
                .body("This endpoint is deprecated. Please use /api/v1/notifications/unread/paginated");
    }

    @GetMapping("/unread/paginated")
    public ResponseEntity<Page<NotificationDocument>> getUnreadNotificationsPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        UUID userId = securityUtils.extractUserIdFromAuth(authentication);
        Pageable pageable = PageRequest.of(page, size);
        Page<NotificationDocument> unreadNotifications =
                notificationPersistenceService.getUnreadNotificationsForUser(userId, pageable);

        return ResponseEntity.ok(unreadNotifications);
    }

    /**
     * Provides a lightweight count of unread notifications for a UI badge.
     */
    @GetMapping("/unread/count")
    public ResponseEntity<Long> getUnreadNotificationCount(Authentication authentication) {
        UUID userId = securityUtils.extractUserIdFromAuth(authentication);
        long count = notificationPersistenceService.getUnreadNotificationCountForUser(userId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotificationDocument> getNotification(
            @PathVariable String id,
            Authentication authentication) {

        UUID userId = securityUtils.extractUserIdFromAuth(authentication);
        NotificationDocument notification = notificationPersistenceService.getNotification(id, userId);

        return ResponseEntity.ok(notification);
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationDocument> markAsRead(
            @PathVariable String id,
            Authentication authentication) {

        UUID userId = securityUtils.extractUserIdFromAuth(authentication);
        NotificationDocument updated = notificationPersistenceService.markNotificationAsRead(id, userId);

        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(
            @PathVariable String id,
            Authentication authentication) {

        UUID userId = securityUtils.extractUserIdFromAuth(authentication);
        notificationPersistenceService.deleteNotification(id, userId);

        return ResponseEntity.noContent().build();
    }

    /**
     * Marks all unread notifications for the authenticated user as read.
     */
    @PatchMapping("/read-all")
    public ResponseEntity<Long> markAllAsRead(Authentication authentication) {
        UUID userId = securityUtils.extractUserIdFromAuth(authentication);
        long count = notificationPersistenceService.markAllNotificationsAsRead(userId);
        return ResponseEntity.ok(count);
    }
}