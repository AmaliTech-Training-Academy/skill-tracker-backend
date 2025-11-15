package com.amalitech.analytics.service.controller;

import com.amalitech.analytics.service.dto.DashboardDTO;
import com.amalitech.analytics.service.dto.TaskSubmissionRequestDTO;
import com.amalitech.analytics.service.dto.TrajectoryPointDTO;
import com.amalitech.analytics.service.model.enums.Granularity;
import com.amalitech.analytics.service.security.util.JwtUtil;
import com.amalitech.analytics.service.service.AnalyticsReadService;
import com.amalitech.analytics.service.service.AnalyticsService;
import com.amalitech.analytics.service.util.CookieUtil;
import com.amalitech.common.security.dto.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final AnalyticsReadService analyticsReadService;
    private final AnalyticsService analyticsService;
    private static final String API_TRACE_ID = "....";

    public AnalyticsController(AnalyticsReadService analyticsReadService,
                               AnalyticsService analyticsService,
                               CookieUtil cookieUtil,
                               JwtUtil jwtUtil) {
        this.analyticsReadService = analyticsReadService;
        this.analyticsService = analyticsService;
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
    public ResponseEntity<ApiResponse<DashboardDTO>> getDashboard(
            @RequestHeader("X-User-Id") String userID,
            @AuthenticationPrincipal String userIdPrincipal) {
        UUID userId = UUID.fromString(userID);
        DashboardDTO dashboard = analyticsReadService.buildDashboard(userId);

        ApiResponse<DashboardDTO> response = ApiResponse.success("Dashboard successfully retrieved.", dashboard, API_TRACE_ID);
        return ResponseEntity.ok(response);
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
    public ResponseEntity<ApiResponse<List<TrajectoryPointDTO>>> getSkillTrajectory(
            @PathVariable UUID skillId,
            @RequestHeader("X-User-Id") String userID,
            HttpServletRequest request,
            @RequestParam(defaultValue = "DAILY") Granularity granularity) {

        UUID userId = UUID.fromString(userID);
        List<TrajectoryPointDTO> trajectory =
                analyticsReadService.getSkillTrajectory(userId, skillId, granularity);

        ApiResponse<List<TrajectoryPointDTO>> response = ApiResponse.success("Skill trajectory retrieved.", trajectory, API_TRACE_ID);
        return ResponseEntity.ok(response);
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
    public ResponseEntity<ApiResponse<List<LocalDate>>> getCurrentStreakDays(
            @RequestHeader("X-User-Id") String userID) {
        UUID userId = UUID.fromString(userID);
        List<LocalDate> streakDates = analyticsReadService.getCurrentStreakDays(userId);

        ApiResponse<List<LocalDate>> response = ApiResponse.success("Current streak dates retrieved.", streakDates, API_TRACE_ID);
        return ResponseEntity.ok(response);
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
    public  ResponseEntity<ApiResponse<Void>> submitTaskDirectly(
            @Valid @RequestBody TaskSubmissionRequestDTO request) {
        analyticsService.submitTaskDirectly(request);
        ApiResponse<Void> response = ApiResponse.success("Task submission accepted for processing.", null, API_TRACE_ID);
        return new ResponseEntity<>(response, HttpStatus.ACCEPTED);
    }

    /**
     * Retrieves all practice days for the authenticated user.
     * <p>
     * Commonly used for generating calendar-based visualizations of learning
     * activity or streak tracking.
     * </p>
     *
     * @param userID the authenticated user ID from Spring Security
     * @return {@link ResponseEntity} containing a list of {@link LocalDate} values
     *         representing practice history
     */
    @GetMapping("/dashboard/practice-history")
    public ResponseEntity<ApiResponse<List<LocalDate>>> getPracticeHistory(
            @RequestHeader("X-User-Id") String userID) {
        UUID userId = UUID.fromString(userID);
        List<LocalDate> practiceDates = analyticsReadService.getAllPracticeDays(userId);
        ApiResponse<List<LocalDate>> response = ApiResponse.success("Practice history dates retrieved.", practiceDates, API_TRACE_ID);
        return ResponseEntity.ok(response);
    }
}