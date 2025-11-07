package com.amalitech.analytics.service.controller;

import com.amalitech.analytics.service.dto.DashboardDTO;
import com.amalitech.analytics.service.dto.TaskSubmissionRequestDTO;
import com.amalitech.analytics.service.dto.TrajectoryPointDTO;
import com.amalitech.analytics.service.model.enums.Granularity;
import com.amalitech.analytics.service.security.util.JwtUtil;
import com.amalitech.analytics.service.service.AnalyticsReadService;
import com.amalitech.analytics.service.service.AnalyticsService;
import com.amalitech.analytics.service.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsReadService analyticsReadService;
    private final AnalyticsService analyticsService;
    private final CookieUtil cookieUtil;
    private final JwtUtil jwtUtil;

    // --- 1. Main Dashboard Endpoint (Highest Traffic, Heavily Cached) ---

    @GetMapping("/dashboard")
    // Caches the entire DashboardDTO payload in Redis/Cache Manager.
    // Cache is only invalidated by the Write Service (AnalyticsService).
    public ResponseEntity<DashboardDTO> getDashboard(@RequestHeader("X-User-Id") String userID
            ,  @AuthenticationPrincipal String userIdPrincipal) {
        try {
            UUID userId = UUID.fromString(userID);
            DashboardDTO dashboard = analyticsReadService.buildDashboard(userId);
            return ResponseEntity.ok(dashboard);
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid UUID in X-User-Id header: " + userID);
            return ResponseEntity.badRequest().build();
        }

    }

    // --- 2. Trajectory Endpoint with Granularity Filter ---

    @GetMapping("/dashboard/trajectory/{skillId}")
    // Key uses all input parameters for unique caching: user, skill, and granularity (DAILY/WEEKLY/MONTHLY).
    public ResponseEntity<List<TrajectoryPointDTO>> getSkillTrajectory(
            @PathVariable UUID skillId,
            @RequestHeader("X-User-Id") String userID,
            HttpServletRequest request,
            @RequestParam(defaultValue = "DAILY") Granularity granularity) {

        try {
            UUID userId = UUID.fromString(userID);
            List<TrajectoryPointDTO> trajectory = analyticsReadService
                    .getSkillTrajectory(userId, skillId, granularity);
            return ResponseEntity.ok(trajectory);
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid UUID in X-User-Id header: " + userID);
            return ResponseEntity.badRequest().build();
        }


    }

    // --- 3. Current Streak History Endpoint ---

    @GetMapping("/dashboard/current-streak-history")
    public ResponseEntity<List<LocalDate>> getCurrentStreakDays(@RequestHeader("X-User-Id") String userID) {
        try {
            UUID userId = UUID.fromString(userID);
            List<LocalDate> streakDates = analyticsReadService.getCurrentStreakDays(userId);
            return ResponseEntity.ok(streakDates);
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid UUID in X-User-Id header: " + userID);
            return ResponseEntity.badRequest().build();
        }

    }

    @PostMapping("/submit-task")
    public ResponseEntity<Void> submitTaskDirectly(
            @Valid @RequestBody TaskSubmissionRequestDTO request) {
        // @Valid ensures input data conforms to DTO constraints (e.g., score between 0.0 and 1.0)
        analyticsService.submitTaskDirectly(request);

        // Return 202 Accepted to signal that processing has started
        return ResponseEntity.accepted().build();
    }

    // --- 4. Full Practice History Endpoint (Calendar View) ---

    @GetMapping("/dashboard/practice-history")

    public ResponseEntity<List<LocalDate>> getPracticeHistory(
            @AuthenticationPrincipal String userIdPrincipal) {
        UUID userId = UUID.fromString(userIdPrincipal);
        List<LocalDate> practiceDates = analyticsReadService.getAllPracticeDays(userId);
        return ResponseEntity.ok(practiceDates);
    }


}