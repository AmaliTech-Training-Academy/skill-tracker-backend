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
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;



/**
 * REST controller that exposes analytics-related endpoints for user dashboards,
 * practice history, streaks, and task submissions.
 * <p>
 * This controller separates read-heavy operations (via {@link AnalyticsReadService})
 * from write operations (via {@link AnalyticsService}), improving scalability
 * and enabling cache-optimized dashboard delivery.
 * </p>
 *
 * <p>Each endpoint validates the {@code X-User-Id} header to ensure correct user
 * association, and many endpoints leverage caching in the read service layer
 * for performance and reduced latency.</p>
 *
 * @author
 * @since 1.0
 */
@RestController
@RequestMapping("/analytics")
public class AnalyticsController {

    private final AnalyticsReadService analyticsReadService;
    private final AnalyticsService analyticsService;
    private final CookieUtil cookieUtil;
    private final JwtUtil jwtUtil;

    public AnalyticsController(AnalyticsReadService analyticsReadService,
                               AnalyticsService analyticsService,
                               CookieUtil cookieUtil,
                               JwtUtil jwtUtil) {
        this.analyticsReadService = analyticsReadService;
        this.analyticsService = analyticsService;
        this.cookieUtil = cookieUtil;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Retrieves the main user dashboard containing performance metrics,
     * progress summaries, and key analytics data.
     * <p>
     * The response payload is typically cached in Redis or the configured cache manager,
     * and is invalidated by the write service when data changes.
     * </p>
     *
     * @param userID          the user UUID provided in the {@code X-User-Id} request header
     * @param userIdPrincipal the authenticated user principal from Spring Security
     * @return {@link ResponseEntity} containing the {@link DashboardDTO} data,
     *         or {@code 400 Bad Request} if the UUID is invalid
     */
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardDTO> getDashboard(
            @RequestHeader("X-User-Id") String userID,
            @AuthenticationPrincipal String userIdPrincipal) {
        try {
            UUID userId = UUID.fromString(userID);
            DashboardDTO dashboard = analyticsReadService.buildDashboard(userId);
            return ResponseEntity.ok(dashboard);
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid UUID in X-User-Id header: " + userID);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Retrieves the user’s skill trajectory over time with configurable granularity.
     * <p>
     * The endpoint supports daily, weekly, or monthly granularity and can be
     * cached using a composite key of user ID, skill ID, and granularity value.
     * </p>
     *
     * @param skillId     the unique identifier of the skill being analyzed
     * @param userID      the user UUID provided in the {@code X-User-Id} header
     * @param request     the incoming {@link HttpServletRequest}
     * @param granularity the data aggregation level (defaults to {@code DAILY})
     * @return {@link ResponseEntity} containing a list of {@link TrajectoryPointDTO},
     *         or {@code 400 Bad Request} if the UUID is invalid
     */
    @GetMapping("/dashboard/trajectory/{skillId}")
    public ResponseEntity<List<TrajectoryPointDTO>> getSkillTrajectory(
            @PathVariable UUID skillId,
            @RequestHeader("X-User-Id") String userID,
            HttpServletRequest request,
            @RequestParam(defaultValue = "DAILY") Granularity granularity) {

        try {
            UUID userId = UUID.fromString(userID);
            List<TrajectoryPointDTO> trajectory =
                    analyticsReadService.getSkillTrajectory(userId, skillId, granularity);
            return ResponseEntity.ok(trajectory);
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid UUID in X-User-Id header: " + userID);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Returns a list of dates representing the user's current active streak.
     * <p>
     * Useful for visualizing streak history or calculating progress trends
     * in gamified analytics dashboards.
     * </p>
     *
     * @param userID the user UUID from the {@code X-User-Id} request header
     * @return {@link ResponseEntity} containing a list of {@link LocalDate} values
     *         for streak days, or {@code 400 Bad Request} if the UUID is invalid
     */
    @GetMapping("/dashboard/current-streak-history")
    public ResponseEntity<List<LocalDate>> getCurrentStreakDays(
            @RequestHeader("X-User-Id") String userID) {
        try {
            UUID userId = UUID.fromString(userID);
            List<LocalDate> streakDates = analyticsReadService.getCurrentStreakDays(userId);
            return ResponseEntity.ok(streakDates);
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid UUID in X-User-Id header: " + userID);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Accepts and processes a direct task submission request.
     * <p>
     * Validates incoming data using {@code @Valid}, ensuring input constraints
     * such as normalized score values. The endpoint returns {@code 202 Accepted}
     * to indicate asynchronous task handling.
     * </p>
     *
     * @param request the {@link TaskSubmissionRequestDTO} containing task submission details
     * @return {@link ResponseEntity} with {@code 202 Accepted} status
     */
    @PostMapping("/submit-task")
    public ResponseEntity<Void> submitTaskDirectly(
            @Valid @RequestBody TaskSubmissionRequestDTO request) {
        analyticsService.submitTaskDirectly(request);
        return ResponseEntity.accepted().build();
    }

    /**
     * Retrieves all practice days for the authenticated user.
     * <p>
     * Commonly used for generating calendar-based visualizations of learning
     * activity or streak tracking.
     * </p>
     *
     * @param userIdPrincipal the authenticated user ID from Spring Security
     * @return {@link ResponseEntity} containing a list of {@link LocalDate} values
     *         representing practice history
     */
    @GetMapping("/dashboard/practice-history")
    public ResponseEntity<List<LocalDate>> getPracticeHistory(
            @AuthenticationPrincipal String userIdPrincipal) {
        UUID userId = UUID.fromString(userIdPrincipal);
        List<LocalDate> practiceDates = analyticsReadService.getAllPracticeDays(userId);
        return ResponseEntity.ok(practiceDates);
    }
}