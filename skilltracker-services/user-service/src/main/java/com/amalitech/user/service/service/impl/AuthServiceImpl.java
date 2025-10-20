package com.amalitech.user.service.service.impl;

import com.amalitech.user.service.dto.request.LoginRequest;
import com.amalitech.user.service.dto.request.RegisterRequest;
import com.amalitech.user.service.dto.response.AuthResponse;
import com.amalitech.user.service.exception.InvalidPasswordException;
import com.amalitech.user.service.exception.InvalidTokenException;
import com.amalitech.user.service.exception.RefreshTokenException;
import com.amalitech.user.service.exception.UserNotFoundException;
import com.amalitech.user.service.model.*;
import com.amalitech.user.service.model.enums.Role;
import com.amalitech.user.service.model.enums.UserState;
import com.amalitech.user.service.repository.UserProfileRepository;
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
 * Handles all authentication-related operations such as registration, login, token generation,
 * password resets, and logout. This service coordinates between the persistence layer, security
 * components (JWT, password encoding, authentication), and external services (Redis, email).
 *
 * <p>It ensures secure authentication by using JWT for stateless sessions and Redis for
 * short-lived token storage such as refresh and password reset tokens.</p>
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
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
    private final UserService userService;

    public AuthServiceImpl(
            UserRepository userRepository, UserProfileRepository userProfileRepository,
            JwtUtil jwtUtil,
            BCryptPasswordEncoder passwordEncoder,
            EmailService emailService,
            RedisUtil redisUtil,
            UserService userService,
            @Value("${jwt.refresh-expiration-ms}") long refreshExpiration,
            @Value("${app.reset-token-expiration-ms}") long resetExpiration,
            @Value("${app.refresh-token-prefix}") String refreshPrefix,
            @Value("${app.reset-token-prefix}") String resetPrefix,
            @Value("${app.base-url}") String appBaseUrl,
            AuthenticationManager authenticationManager
    ) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
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
        this.userService = userService;
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
            throw new RuntimeException("Email already exists");
        }

        User user = new User();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setUsername(request.username());
        user.setRole(Role.USER);

        UserProfile profile = new UserProfile();
        profile.setEmailNotifications(true);
        profile.setPushNotifications(true);
        user.setProfile(profile);

        User savedUser = userRepository.save(user);

        log.debug("User registered with profile: {}", savedUser.getEmail());

        return savedUser;
    }

    /**
     * Authenticates a user using email and password credentials.
     * <p>
     * Delegates authentication to Spring Security’s {@link AuthenticationManager},
     * then generates both access and refresh tokens for valid users.
     */
    @Override
    public AuthResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        User user = userDetails.getUser();

        return generateTokens(user);
    }

    /**
     * Generates a new access token and refresh token pair for a given user.
     * <p>
     * The refresh token is stored in Redis with an expiration time, allowing users
     * to obtain a new access token without re-authenticating.
     */
    @Override
    public AuthResponse generateTokens(User user) {
        String accessToken = jwtUtil.generateAccessToken(user.getEmail(), user.getRole(), user.getId());
        String refreshToken = UUID.randomUUID().toString();
        redisUtil.set(refreshPrefix + refreshToken, user.getEmail(), refreshExpiration / 1000);
        return new AuthResponse(accessToken, refreshToken);
    }

    /**
     * Refreshes a user’s access and refresh tokens using a valid refresh token.
     * <p>
     * Validates the provided refresh token from Redis, checks user status, then issues
     * a new access token and refresh token. Suspended users cannot refresh tokens.
     */
    @Override
    @Transactional
    public AuthResponse refresh(String refreshToken) {
        String key = refreshPrefix + refreshToken;
        String email = redisUtil.get(key);
        if (email == null) {
            throw new RefreshTokenException("Invalid refresh token");
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
        log.debug("Tokens refreshed for user: {}", email);
        return new AuthResponse(newAccessToken, newRefreshToken);
    }

    /**
     * Initiates the password reset process for a given email address.
     * <p>
     * Generates a temporary reset token stored in Redis and sends a password reset
     * email containing a secure link to the user.
     */
    @Override
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
     * Completes the password reset process.
     * <p>
     * Validates the provided reset token from Redis, ensures the user exists,
     * updates their password, and invalidates the used reset token.
     */
    @Override
    public void resetPassword(String token, String newPassword) {
        String key = resetPrefix + token;
        String email = redisUtil.get(key);

        if (email == null) {
            throw new InvalidTokenException("Invalid or expired token");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RefreshTokenException("User not found"));

        if (user == null) {
            throw new UserNotFoundException("User not found");
        }

        userService.updatePassword(user, newPassword);
        redisUtil.delete(key);
    }

    /**
     * Allows a logged-in user to change their password.
     * <p>
     * Verifies that the old password matches before updating to the new one.
     * Throws an exception if the old password is invalid or the user does not exist.
     */
    @Override
    @Transactional
    public void changePassword(String email, String oldPassword, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RefreshTokenException("User not found"));
        if (user == null || !passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new InvalidPasswordException("Invalid old password");
        }
        userService.updatePassword(user, newPassword);
    }

    /**
     * Logs out a user by invalidating both access and refresh tokens.
     * <p>
     * The refresh token is removed from Redis, and the access token is blacklisted
     * until its natural expiration time, preventing further use.
     */
    @Override
    public void logout(String accessToken, String refreshToken) {
        redisUtil.delete(refreshPrefix + refreshToken);
        long ttl = jwtUtil.getExpirationSeconds(accessToken);
        redisUtil.set("blacklist:" + accessToken, "revoked", ttl);
    }
}
