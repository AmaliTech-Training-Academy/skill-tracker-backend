package com.amalitech.user.service.service;

import com.amalitech.user.service.dto.UserRequestDTO;
import com.amalitech.user.service.dto.UserResponseDTO;
import com.amalitech.user.service.dto.request.LoginRequest;
import com.amalitech.user.service.dto.response.AuthResponse;
import com.amalitech.user.service.exception.*;
import com.amalitech.user.service.model.User;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Interface defining the public contract for authentication operations.
 * This includes user registration, login, token management, and password recovery.
 */
public interface AuthService {

    /**
     * Registers a new user with the provided details.
     *
     * @param request the registration request containing user details
     * @return The newly created User entity
     * @throws RuntimeException if email already exists
     */

    /**
     * Authenticates the user and generates access and refresh tokens.
     *
     * @param request the login request containing email and password
     * @return AuthResponse containing the access and refresh tokens
     */
    UserResponseDTO login(LoginRequest request, HttpServletResponse response);

    /**
     * Generate accessToken and RefreshTokens for the given user.
     *
     * @param user the user to generate tokens for
     * @return AuthResponse with access and refresh tokens
     */
    void generateTokens(User user, HttpServletResponse response);

    /**
     * Refreshes the access token by validating and rotating the refresh token.
     *
     * @param request the current refresh token
     * @param response the current refresh token
     * @return AuthResponse with new access and rotated refresh tokens.
     * @throws RefreshTokenException if token is invalid.
     */
    @Transactional
    AuthResponse refresh(HttpServletRequest request, HttpServletResponse response);

    /**
     * Initiates a password reset by generating and sending a reset token.
     *
     * @param email the user's email address
     */
    @Transactional
    void forgotPassword(String email);

    /**
     * Completes password reset by validating the token and updating the password.
     *
     * @param token       the password reset token
     * @param newPassword the new password to set
     * @throws InvalidTokenException if token is invalid
     * @throws UserNotFoundException if user doesn't exist
     */
    void resetPassword(String token, String newPassword);

    /**
     * Changes the user's password after verifying the old password.
     *
     * @param email       the user's email
     * @param oldPassword the current password for verification
     * @param newPassword the new password to set
     * @throws InvalidPasswordException if old password is incorrect
     */
    @Transactional
    void changePassword(String email, String oldPassword, String newPassword);

    /**
     * Logs out the user by revoking the refresh token and blacklisting the access token.
     *
     * @param request  retrieve tokens from request header set tokens on response header
     * @param response set tokens on response header
     */
    void logout(HttpServletRequest request, HttpServletResponse response);

    UserResponseDTO createUser(@Valid UserRequestDTO userdto);
    Optional<UserResponseDTO> verifyCode(String code, String email);
    void sendVerificationCode(String toEmail);
}