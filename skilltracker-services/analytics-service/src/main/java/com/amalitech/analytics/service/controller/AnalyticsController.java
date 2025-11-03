package com.amalitech.analytics.service.controller;

import com.amalitech.analytics.service.dto.response.DashboardResponse;
import com.amalitech.analytics.service.dto.response.UserGoalDto;
import com.amalitech.analytics.service.model.UserGoal;
import com.amalitech.analytics.service.security.util.JwtUtil;
import com.amalitech.analytics.service.service.AnalyticsService;
import com.amalitech.analytics.service.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/analytics/progress")

public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final CookieUtil cookieUtil;
    private final JwtUtil jwtUtil;

    public AnalyticsController(AnalyticsService analyticsService, CookieUtil cookieUtil, JwtUtil jwtUtil) {
        this.analyticsService = analyticsService;
        this.cookieUtil = cookieUtil;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResponse> getProgressDashboard(
            HttpServletRequest request,
            @RequestParam(required = false) UUID skillId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        Optional<UUID> optSkillId = Optional.ofNullable(skillId);
        Optional<LocalDate> optStartDate = Optional.ofNullable(startDate);
        Optional<LocalDate> optEndDate = Optional.ofNullable(endDate);
        UUID userId = extractUserId(request);
        DashboardResponse dashboard = analyticsService.getDashboardData(userId, optSkillId, optStartDate, optEndDate);
        return ResponseEntity.ok(dashboard);
    }

    @PostMapping("/goals")
    public ResponseEntity<UserGoal> createGoal(
            HttpServletRequest request,
            @RequestBody UserGoalDto goalDto
    ) {
        UUID userId = extractUserId(request);
        UserGoal createdGoal = analyticsService.createUserGoal(userId, goalDto);
        return new ResponseEntity<>(createdGoal, HttpStatus.CREATED);
    }

    @GetMapping("/goals")
    public ResponseEntity<List<UserGoal>> getActiveGoals(HttpServletRequest request) {
        UUID userId = extractUserId(request);
        return ResponseEntity.ok(analyticsService.getActiveGoals(userId));
    }


    private UUID extractUserId(HttpServletRequest request) {
        String token = cookieUtil.getCookieValue(request, "accessToken");
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Missing access token in cookies");
        }

        String userIdStr = jwtUtil.extractUserId(token);
        if (userIdStr == null || userIdStr.isBlank()) {
            throw new IllegalArgumentException("User ID missing in JWT token");
        }
        return UUID.fromString(userIdStr);
    }


}
