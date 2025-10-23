package com.amalitech.user.service.controller;

import com.amalitech.common.security.dto.response.ApiResponse;
import com.amalitech.user.service.dto.response.UserDto;
import com.amalitech.user.service.dto.request.*;
import com.amalitech.user.service.dto.response.AuthResponse;
import com.amalitech.user.service.mapper.UserMapper;
import com.amalitech.user.service.service.AuthService;
import com.amalitech.user.service.service.impl.AuthServiceImpl;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

/**
 * Controller for authentication-related endpoints.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthServiceImpl authService) {
        this.authService = authService;
    }

    /** Register the user and returns the username, email */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserDto>> register(@Valid @RequestBody RegisterRequest request) {
        UserDto userDto = UserMapper.toDto(authService.register(request));
        return ResponseEntity.ok(ApiResponse.success("User registered successfully", userDto, null));
    }

    /** Authenticates the user and returns an access token */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse authResponse = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", authResponse, null));
    }

    /** Refreshes the access token */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshRequest request) {
        AuthResponse authResponse = authService.refresh(request.token());
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", authResponse, null));
    }

    /** Forgot password - send reset link */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.email());
        return ResponseEntity.ok(ApiResponse.success("Reset link sent", "Check your email", null));
    }

    /** Change password (authenticated) */
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<String>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        authService.changePassword(email, request.oldPassword(), request.newPassword());
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null, null));
    }

    /** Reset password using token */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.token(), request.password());
        return ResponseEntity.ok(ApiResponse.success("Password reset successfully", null, null));
    }

    /** Logout - revoke refresh token */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody RefreshRequest request) {
        String accessToken = authHeader.replace("Bearer ", "");
        authService.logout(accessToken, request.token());
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null, null));
    }
}
