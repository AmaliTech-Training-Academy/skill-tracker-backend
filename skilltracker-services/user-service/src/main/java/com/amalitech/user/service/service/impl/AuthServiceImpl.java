package com.amalitech.user.service.service.impl;

import com.amalitech.user.service.dto.request.LoginRequest;
import com.amalitech.user.service.dto.request.RegisterRequest;
import com.amalitech.user.service.dto.response.AuthResponse;
import com.amalitech.user.service.exception.*;
import com.amalitech.user.service.model.*;
import com.amalitech.user.service.model.enums.Role;
import com.amalitech.user.service.model.enums.UserState;
import com.amalitech.user.service.repository.UserRepository;
import com.amalitech.user.service.security.CustomUserDetails;
import com.amalitech.user.service.security.util.JwtUtil;
import com.amalitech.user.service.service.AuthService;
import com.amalitech.user.service.service.EmailService;
import com.amalitech.user.service.service.UserService;
import com.amalitech.user.service.util.RedisUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.UUID;

/**
 * Service class for handling authentication operations including registration, login, token management,
 * and password recovery.
 */
@Service
public class AuthServiceImpl implements AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final RedisUtil redisUtil;
    private final long refreshExpiration;
    private final long resetExpiration;
    private final String refreshPrefix;
    private final String resetPrefix;
    private final String appBaseUrl;


    public AuthServiceImpl(
            UserRepository userRepository,
            JwtUtil jwtUtil,
            BCryptPasswordEncoder passwordEncoder,
            EmailService emailService,
            RedisUtil redisUtil,
            @Value("${jwt.refresh-expiration-ms}") long refreshExpiration,
            @Value("${app.reset-token-expiration-ms}") long resetExpiration,
            @Value("${app.refresh-token-prefix}") String refreshPrefix,
            @Value("${app.reset-token-prefix}") String resetPrefix,
            @Value("${app.base-url}") String appBaseUrl,
            AuthenticationManager authenticationManager
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.redisUtil = redisUtil;
        this.refreshExpiration = refreshExpiration;
        this.resetExpiration = resetExpiration;
        this.refreshPrefix = refreshPrefix;
        this.resetPrefix = resetPrefix;
        this.appBaseUrl = appBaseUrl;
    }

    /**
     * Registers a new user in the system and creates their profile.
     * <p>
     * Checks if the email already exists, encodes the password, assigns the default {@code USER}
     * role, persists the new user, and creates an associated user profile with default settings.
     */
    @Override
    @Transactional
    public User register(RegisterRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new EmailAlreadyExistsException("Email already exists");
        }

        User user = new User();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.USER);

        UserProfile profile = new UserProfile();
        profile.setEmailNotifications(true);
        profile.setPushNotifications(true);
        user.setProfile(profile);

        return userRepository.save(user);
    }

    /**
     * Authenticates the user and generates access and refresh tokens.
     *
     * @param request the login request containing email and password
     * @return AuthTokens containing the access and refresh tokens
     * @throws RuntimeException if credentials are invalid or account not verified
     */
    public AuthResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        User user = userDetails.getUser();

        return generateTokens(user);
    }

    /**
     * Generate accessToken and RefreshTokens for the given user.
     *
     * @param user the user to generate tokens for
     * @return AuthTokens  access and rotated refresh tokens
     */
    public AuthResponse generateTokens(User user) {
        String accessToken = jwtUtil.generateAccessToken(user.getEmail(), user.getRole(), user.getId());
        String refreshToken = UUID.randomUUID().toString();
        redisUtil.set(refreshPrefix + refreshToken, user.getEmail(), refreshExpiration / 1000);
        return new AuthResponse(accessToken, refreshToken);
    }

    /**
     * Refreshes the access token by validating and rotating the refresh token.
     *
     * @param refreshToken the current refresh token
     * @return {@link AuthResponse} with new access and rotated refresh tokens
     * @throws RefreshTokenException if refresh token is invalid, revoked, or expired
     */
    @Transactional
    public AuthResponse refresh(String refreshToken) {
        String key = refreshPrefix + refreshToken;
        String email = redisUtil.get(key);
        if (email == null) {
            log.warn("Possible invalid or expired refresh token: {}", refreshToken);
            throw new RefreshTokenException("Authentication failed. Please log in again.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RefreshTokenException("User not found"));

        if (user.getState() == UserState.SUSPENDED) {
            redisUtil.delete(key);
            throw new RefreshTokenException("User is suspended");
        }

        redisUtil.delete(key);
        String newRefreshToken = UUID.randomUUID().toString();
        redisUtil.set(refreshPrefix + newRefreshToken, email, refreshExpiration / 1000);
        String newAccessToken = jwtUtil.generateAccessToken(email, user.getRole(), user.getId());
        log.info("Access and refresh tokens rotated successfully for user: {}", email);
        return new AuthResponse(newAccessToken, newRefreshToken);
    }

    /**
     * Initiates a password reset by generating a reset token,
     * saving it in Redis, and sending a reset email.
     *
     * This method does not reveal whether the email exists for security reasons.
     *
     * @param email the user's email address
     */
    @Transactional
    public void forgotPassword(String email) {
        userRepository.findByEmail(email)
                .orElseThrow(() -> new RefreshTokenException("User not found"));

        String resetToken = UUID.randomUUID().toString();
        String key = resetPrefix + resetToken;
        redisUtil.set(key, email, resetExpiration / 1000);
        String resetLink = appBaseUrl + "/api/v1/auth/reset-password?token=" + resetToken;
        emailService.sendResetEmail(email, resetLink);
    }

    /**
     * Completes password reset by validating the reset token
     * and updating the user's password.
     *
     * @param token the password reset token
     * @param newPassword the new password to set
     * @throws InvalidTokenException if token is invalid or expired
     * @throws UserNotFoundException if user associated with token does not exist
     */
    public void resetPassword(String token, String newPassword) {
        String key = resetPrefix + token;
        String email = redisUtil.get(key);
        if (email == null) {
            throw new InvalidTokenException("Invalid or expired token");
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RefreshTokenException("User not found"));

        updatePassword(user, newPassword);
        redisUtil.delete(key);

    }

    /**
     * Changes the user's password after verifying the old password.
     *
     * @param email the user's email
     * @param oldPassword the current password for verification
     * @param newPassword the new password to set
     * @throws InvalidPasswordException if old password is incorrect
     */
    @Transactional
    public void changePassword(String email, String oldPassword, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RefreshTokenException("User not found"));
        if (user == null || !passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new InvalidPasswordException("Invalid old password");
        }
        updatePassword(user, newPassword);
    }

    /**
     * update oldpassword in data base with new password.
     *
     * @param user, newPassword to replace the old password
     */
    public void updatePassword(User user, String newPassword) {
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    /**
     * Logs out the user by revoking the refresh token.
     *
     * @param refreshToken,accessToken the refresh token to delete
     */
    public void logout(String accessToken, String refreshToken) {
        redisUtil.delete(refreshPrefix + refreshToken);
        long ttl = jwtUtil.getExpirationSeconds(accessToken);
        redisUtil.set("blacklist:" + accessToken, "revoked", ttl);
    }


}