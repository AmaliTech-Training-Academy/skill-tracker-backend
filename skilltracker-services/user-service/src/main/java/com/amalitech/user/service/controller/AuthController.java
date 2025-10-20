package com.amalitech.user.service.controller;

import com.amalitech.user.service.dto.ApiResponse;

import com.amalitech.user.service.dto.response.UserDto;
import com.amalitech.user.service.dto.request.*;
import com.amalitech.user.service.dto.response.AuthResponse;
import com.amalitech.user.service.mapper.UserMapper;
import com.amalitech.user.service.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for authentication-related endpoints.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
    * Register the user and returns the username, email
    */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserDto>> register(@Valid @RequestBody RegisterRequest request, HttpServletResponse response) {
        return ResponseEntity.ok(ApiResponse.success(UserMapper.toDto(authService.register(request))));
    }

    /**
     * Authenticates the user and returns an access token, setting a secure refresh token cookie.
     */
    @Operation(summary = "Login with email and password")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        return ResponseEntity.ok(ApiResponse.success(authService.login(request)));
    }

    /**
     * Refreshes the access token using the refresh token from the cookie and rotates the refresh token.
     */
    @Operation(summary = "Refresh access token")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.refresh(request.token())));
    }

    @Operation(summary = "Forgot password - send reset link")
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.email());
        return ResponseEntity.ok(ApiResponse.success("Reset link sent"));
    }

    @Operation(summary = "Change password (authenticated)")
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<String>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        authService.changePassword(email, request.oldPassword(), request.newPassword());
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully"));
    }

    @Operation(summary = "Reset password using token")
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.token(), request.password());
        return ResponseEntity.ok("Password reset successfully.");
    }

    /**
     * Logs out the user by revoking the refresh token and clearing the cookie.
     */
    @Operation(summary = "Logout - revoke refresh token")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(@RequestHeader("Authorization") String authHeader, @RequestBody RefreshRequest request) {
        String accessToken = authHeader.replace("Bearer ", "");
        authService.logout(accessToken, request.token());
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
    }

}