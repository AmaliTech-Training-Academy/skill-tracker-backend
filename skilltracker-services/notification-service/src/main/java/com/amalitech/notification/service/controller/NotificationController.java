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
 * REST API controller for managing historical notifications.
 * 
 * <p>Provides endpoints for:
 * <ul>
 *   <li>Retrieving paginated notifications for authenticated users</li>
 *   <li>Fetching unread notification counts and details</li>
 *   <li>Marking notifications as read (individually or in bulk)</li>
 *   <li>Deleting notifications</li>
 * </ul>
 * 
 * <p>Real-time notifications are delivered via WebSocket connections, while this
 * controller handles retrieval and management of historical notification data stored
 * in MongoDB.
 * 
 * <p>All endpoints require authentication and operate on the authenticated user's notifications.
 * User authorization is enforced via Spring Security context.
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationPersistenceService notificationPersistenceService;
    private final SecurityUtils securityUtils;

    /**
     * Retrieves all notifications for the authenticated user with pagination.
     * 
     * @param page the page number (zero-indexed, default: 0)
     * @param size the page size (default: 20)
     * @param authentication the Spring Security authentication context
     * @return a paginated response containing {@link NotificationDocument} objects
     */
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
     * Deprecated endpoint for retrieving all unread notifications without pagination.
     * 
     * <p>This endpoint is dangerous as it can load unlimited documents and cause
     * memory issues. Use {@link #getUnreadNotificationsPaginated} instead.
     * 
     * @return a 410 Gone status with deprecation notice
     * @deprecated This endpoint will be removed in v2. Use {@link #getUnreadNotificationsPaginated} instead.
     */
    @Deprecated
    @GetMapping("/unread")
    public ResponseEntity<?> getUnreadNotifications() {
        log.warn("/unread endpoint was called. This is deprecated.");
        return ResponseEntity.status(HttpStatus.GONE)
                .body("This endpoint is deprecated. Please use /api/v1/notifications/unread/paginated");
    }

    /**
     * Retrieves unread notifications for the authenticated user with pagination.
     * 
     * <p>Recommended alternative to the deprecated {@link #getUnreadNotifications} endpoint.
     * 
     * @param page the page number (zero-indexed, default: 0)
     * @param size the page size (default: 20)
     * @param authentication the Spring Security authentication context
     * @return a paginated response containing only unread {@link NotificationDocument} objects
     */
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
     * Retrieves the count of unread notifications for the authenticated user.
     * 
     * <p>Provides a lightweight endpoint for UI badge display and notification indicators.
     * Does not return the full notification documents, only the count.
     * 
     * @param authentication the Spring Security authentication context
     * @return the count of unread notifications for the authenticated user
     */
    @GetMapping("/unread/count")
    public ResponseEntity<Long> getUnreadNotificationCount(Authentication authentication) {
        UUID userId = securityUtils.extractUserIdFromAuth(authentication);
        long count = notificationPersistenceService.getUnreadNotificationCountForUser(userId);
        return ResponseEntity.ok(count);
    }

    /**
     * Retrieves a specific notification by ID for the authenticated user.
     * 
     * @param id the notification document ID
     * @param authentication the Spring Security authentication context
     * @return the {@link NotificationDocument} if found and authorized
     */
    @GetMapping("/{id}")
    public ResponseEntity<NotificationDocument> getNotification(
            @PathVariable String id,
            Authentication authentication) {

        UUID userId = securityUtils.extractUserIdFromAuth(authentication);
        NotificationDocument notification = notificationPersistenceService.getNotification(id, userId);

        return ResponseEntity.ok(notification);
    }

    /**
     * Marks a specific notification as read for the authenticated user.
     * 
     * @param id the notification document ID to mark as read
     * @param authentication the Spring Security authentication context
     * @return the updated {@link NotificationDocument} with read status set to true
     */
    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationDocument> markAsRead(
            @PathVariable String id,
            Authentication authentication) {

        UUID userId = securityUtils.extractUserIdFromAuth(authentication);
        NotificationDocument updated = notificationPersistenceService.markNotificationAsRead(id, userId);

        return ResponseEntity.ok(updated);
    }

    /**
     * Deletes a specific notification for the authenticated user.
     * 
     * @param id the notification document ID to delete
     * @param authentication the Spring Security authentication context
     * @return a 204 No Content response on successful deletion
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(
            @PathVariable String id,
            Authentication authentication) {

        UUID userId = securityUtils.extractUserIdFromAuth(authentication);
        notificationPersistenceService.deleteNotification(id, userId);

        return ResponseEntity.noContent().build();
    }

    /**
     * Marks all unread notifications for the authenticated user as read in bulk.
     * 
     * <p>This is an efficient operation for bulk updating all unread notifications
     * for a user in a single operation.
     * 
     * @param authentication the Spring Security authentication context
     * @return the count of notifications that were marked as read
     */
    @PatchMapping("/read-all")
    public ResponseEntity<Long> markAllAsRead(Authentication authentication) {
        UUID userId = securityUtils.extractUserIdFromAuth(authentication);
        long count = notificationPersistenceService.markAllNotificationsAsRead(userId);
        return ResponseEntity.ok(count);
    }
}