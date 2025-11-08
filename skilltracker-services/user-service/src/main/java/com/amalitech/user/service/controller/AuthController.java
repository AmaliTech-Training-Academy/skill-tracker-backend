package com.amalitech.user.service.controller;

import com.amalitech.common.security.dto.response.ApiResponse;
import com.amalitech.user.service.dto.UserRequestDTO;
import com.amalitech.user.service.dto.UserResponseDTO;
import com.amalitech.user.service.dto.request.*;
import com.amalitech.user.service.dto.response.AuthResponse;
import com.amalitech.user.service.service.AuthService;
import com.amalitech.user.service.service.impl.AuthServiceImpl;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<ApiResponse<UserResponseDTO>> register(@Valid @RequestBody UserRequestDTO userdto, HttpServletResponse response) {

        UserResponseDTO user = authService.createUser(userdto);

        return ResponseEntity.ok(ApiResponse.success("User successfully created", user, null));
    }

    @PostMapping("/verify-email-otp")
    public ResponseEntity<ApiResponse<UserResponseDTO>> verifyCode(
            @RequestParam("code") String code,
            @RequestParam("email") String email,
            HttpServletResponse response) {

        UserResponseDTO user = authService.verifyCode(code, email, response).orElseThrow(() ->
                new RuntimeException("Invalid verification code"));

        return ResponseEntity.ok(ApiResponse.success("Verification is Successful", user, null));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<String>> resendVerification(
            @RequestParam("email") String email
    ) {
        authService.sendVerificationCode(email);
        return ResponseEntity.ok(ApiResponse.success("Verification sent", null, null)) ;
    }

        /** Authenticates the user and returns an access token */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserResponseDTO>> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        UserResponseDTO userResponseDTO = authService.login(request, response);
        return ResponseEntity.ok(ApiResponse.success("Login successful.", userResponseDTO, null));
    }

    /** Refreshes the access token */
    @PostMapping("/token/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(HttpServletRequest request, HttpServletResponse response) {
        authService.refresh(request, response);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", null, null));
    }

    /** Forgot password - send reset link .*/
    @PostMapping("/password/forgot")
    public ResponseEntity<ApiResponse<String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.email());
        return ResponseEntity.ok(ApiResponse.success("Reset link sent", null, null));
    }

    /** Change password (authenticated) */
    @PostMapping("/password/change-password")
    public ResponseEntity<ApiResponse<String>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        authService.changePassword(email, request.oldPassword(), request.newPassword());
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null, null));
    }

    /** Reset password using token */
    @PostMapping("/password/reset")
    public ResponseEntity<ApiResponse<String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.token(), request.password());
        return ResponseEntity.ok(ApiResponse.success("Password reset successfully", null, null));
    }


    /** Logout - revoke refresh token */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(
            HttpServletRequest request,
            HttpServletResponse response) {
        authService.logout(request, response);
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null, null));
    }
}

