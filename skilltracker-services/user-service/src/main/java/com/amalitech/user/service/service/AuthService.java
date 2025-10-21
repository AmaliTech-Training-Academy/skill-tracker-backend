package com.amalitech.user.service.service;

import com.amalitech.user.service.dto.request.LoginRequest;
import com.amalitech.user.service.dto.request.RegisterRequest;
import com.amalitech.user.service.dto.response.AuthResponse;
import com.amalitech.user.service.model.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service class for handling authentication operations including registration, login, token management,
 * and password recovery.
 */
@Service
public interface AuthService {

    /**
     * Registers a new user with the provided details.
     *
     * @param request the registration request
     * @return the newly created user
     */
    @Transactional
    User register(RegisterRequest request);

    /**
     * Authenticates the user and generates tokens.
     *
     * @param request login credentials
     * @return access and refresh tokens
     */
    AuthResponse login(LoginRequest request);

    /**
     * Generates access and refresh tokens for a user.
     *
     * @param user the user entity
     * @return the generated tokens
     */
    AuthResponse generateTokens(User user);

    /**
     * Rotates the refresh token and issues a new access token.
     *
     * @param refreshToken the current refresh token
     * @return refreshed tokens
     */
    @Transactional
    AuthResponse refresh(String refreshToken);

    /**
     * Initiates a password reset process.
     *
     * @param email the user's email
     */
    @Transactional
    void forgotPassword(String email);

    /**
     * Completes password reset using a valid token.
     *
     * @param token       the reset token
     * @param newPassword the new password
     */
    void resetPassword(String token, String newPassword);

    /**
     * Changes a user's password after verifying the old password.
     *
     * @param email       user's email
     * @param oldPassword current password
     * @param newPassword new password
     */
    @Transactional
    void changePassword(String email, String oldPassword, String newPassword);

    /**
     * Logs out a user by revoking tokens.
     *
     * @param accessToken  the access token
     * @param refreshToken the refresh token
     */
    void logout(String accessToken, String refreshToken);
}