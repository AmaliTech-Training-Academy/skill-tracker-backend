package com.amalitech.analytics.service.controller;

import com.amalitech.analytics.service.dto.CreateGoalRequestDTO;
import com.amalitech.analytics.service.dto.UserGoalDTO;
import com.amalitech.analytics.service.service.interfaces.GoalServiceInterface;
import com.amalitech.common.security.dto.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/analytics/goals")
@RequiredArgsConstructor
/**
 * Controller for managing user goals (CRUD operations).
 * All responses are wrapped in ApiResponse<T> and controlled by ResponseEntity.
 * All exceptions are handled by GlobalExceptionHandler.
 */
public class GoalController {

    private final GoalServiceInterface goalService;
    private static final String API_TRACE_ID = "...."; // Placeholder


    @PostMapping
    public ResponseEntity<ApiResponse<UserGoalDTO>> createGoal(
            @RequestHeader("X-User-Id") String userID,
            @Valid @RequestBody CreateGoalRequestDTO request
    ) {
        UUID userId = UUID.fromString(userID);
        UserGoalDTO goal = goalService.createGoal(userId, request);

        ApiResponse<UserGoalDTO> response = ApiResponse.success("Goal successfully created.", goal, API_TRACE_ID);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserGoalDTO>>> listUserGoals(
            @RequestHeader("X-User-Id") String userID
    ) {
        UUID userId = UUID.fromString(userID);
        List<UserGoalDTO> goals = goalService.listGoals(userId);

        ApiResponse<List<UserGoalDTO>> response = ApiResponse.success("User goals retrieved.", goals, API_TRACE_ID);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{goalId}")
    public ResponseEntity<ApiResponse<UserGoalDTO>> getGoal(
            @RequestHeader("X-User-Id") String userID,
            @PathVariable UUID goalId
    ) {
        UUID userId = UUID.fromString(userID);
        UserGoalDTO goal = goalService.getGoal(userId, goalId);

        ApiResponse<UserGoalDTO> response = ApiResponse.success("Goal details retrieved.", goal, API_TRACE_ID);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{goalId}")
    public ResponseEntity<ApiResponse<Void>> deleteGoal(
            @RequestHeader("X-User-Id") String userID,
            @PathVariable UUID goalId
    ) {
        UUID userId = UUID.fromString(userID);
        goalService.deleteGoal(userId, goalId);

        ApiResponse<Void> response = ApiResponse.success("Goal successfully deleted.", null, API_TRACE_ID);
        return ResponseEntity.ok(response);
    }
}