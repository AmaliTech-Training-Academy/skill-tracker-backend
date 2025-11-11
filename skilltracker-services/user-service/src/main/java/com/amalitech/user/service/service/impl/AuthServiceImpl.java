package com.amalitech.user.service.service.impl;

import com.amalitech.user.service.dto.UserRequestDTO;
import com.amalitech.user.service.dto.UserResponseDTO;
import com.amalitech.user.service.dto.request.CreateUserByAdminRequest;
import com.amalitech.user.service.dto.request.LoginRequest;
import com.amalitech.user.service.dto.response.AuthResponse;
import com.amalitech.user.service.dto.response.UserDto;
import com.amalitech.user.service.exception.*;
import com.amalitech.user.service.mapper.UserMapper;
import com.amalitech.user.service.model.*;
import com.amalitech.user.service.model.enums.GuidedTourStatus;
import com.amalitech.user.service.model.enums.UserState;
import com.amalitech.user.service.repository.UserRepository;
import com.amalitech.user.service.security.CustomUserDetails;
import com.amalitech.user.service.security.util.JwtUtil;
import com.amalitech.user.service.service.AuthService;
import com.amalitech.user.service.service.EmailService;
import com.amalitech.user.service.util.CookieUtil;
import com.amalitech.user.service.util.RedisUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service class for handling authentication operations including registration, login, token management,
 * and password recovery
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
    private Integer tempCode;
    private CookieUtil cookieUtil;
    @Value("${app.frontend-url}")
    private String frontendUrl;
    private final Map<Integer, VerificationObject> activeVerifications = new ConcurrentHashMap<>();


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
            AuthenticationManager authenticationManager,
            CookieUtil cookieUtil
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
        this.tempCode = 0;
        this.cookieUtil = cookieUtil;
    }

    /**
     * Registers a new user in the system and creates their profile.
     * <p>
     * Checks if the email already exists, encodes the password, assigns the default {@code USER}
     * role, persists the new user, and creates an associated user profile with default settings.
     */
    @Override
    @Transactional
    public UserResponseDTO createUser(UserRequestDTO userdto) throws IllegalStateException {
        if (userRepository.existsByEmail(userdto.email())) {
            throw new EmailAlreadyExistsException("A user already exists with this email.");
        }

        User user = UserMapper.toEntity(userdto);
        User savedUser = userRepository.save(user);

        int verificationCode = generateCode();
        createVerification(savedUser.getId(), verificationCode);

        notifyUser(
                savedUser.getEmail(),
                "Account created successfully!",
                "Enter this verification code to verify your identity: " + verificationCode);

        UserProfile profile = new UserProfile();
        profile.setEmailNotifications(true);
        profile.setPushNotifications(true);
        savedUser.setProfile(profile);

        return UserMapper.toDto(savedUser);
    }

    /**
     * Authenticates the user and generates access and refresh tokens.
     *
     * @param request the login request containing email and password
     * @return AuthTokens containing the access and refresh tokens
     * @throws RuntimeException if credentials are invalid or account not verified
     */
    public UserResponseDTO login(LoginRequest request, HttpServletResponse response) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        User user = userDetails.getUser();
        if (user.getIsVerified() == false) {
            throw new UnverifiedUserException("User not verified");
        }
        generateTokens(user, response);
        UserResponseDTO userResponseDTO = UserMapper.toDto(user);
        return userResponseDTO;
    }

    /**
     * Generate accessToken and RefreshTokens for the given user.
     *
     * @param user the user to generate tokens for
     * @return AuthTokens  access and rotated refresh tokens
     */
    public void generateTokens(User user, HttpServletResponse response) {
        String accessToken = jwtUtil.generateAccessToken(user.getEmail(), user.getRole(), user.getId());
        String refreshToken = UUID.randomUUID().toString();
        redisUtil.set(refreshPrefix + refreshToken, user.getEmail(), refreshExpiration / 1000);
        cookieUtil.setSecureCookie(response, "accessToken", accessToken,
                jwtUtil.getExpirationSeconds(accessToken));
        cookieUtil.setSecureCookie(response, "refreshToken", refreshToken,
                refreshExpiration / 1000);
    }

    /**
     * Refreshes the access token by validating and rotating the refresh token.
     *
     * @param request request to retrieve refresh token from
     * @param response response to set new access token and refresh token
     * @return {@link AuthResponse} with new access and rotated refresh tokens
     * @throws RefreshTokenException if refresh token is invalid, revoked, or expired.
     */
    @Transactional
    public AuthResponse refresh(HttpServletRequest request, HttpServletResponse response) {
        String key = refreshPrefix + cookieUtil.getCookieValue(request, "refreshToken");
        String email = redisUtil.get(key);
        if (email == null) {
            log.warn("Possible invalid or expired refresh token: {}", cookieUtil.getCookieValue(request, "refreshToken"));
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
        cookieUtil.setSecureCookie(response, "accessToken", newAccessToken,
                jwtUtil.getExpirationSeconds(newAccessToken));
        cookieUtil.setSecureCookie(response, "refreshToken", newRefreshToken,
                refreshExpiration / 1000);
        log.info("Access and refresh tokens rotated successfully for user: {}", email);
        return new AuthResponse("tokens refreshed successfully");
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
        userRepository.findByEmail(email).ifPresent(user -> {
            String resetToken = UUID.randomUUID().toString();;
            String key = resetPrefix + resetToken;
            redisUtil.set(key, email, resetExpiration / 1000);
            String resetLink = frontendUrl + "/reset-password?token=" + resetToken;
            emailService.sendResetEmail(email, resetLink);
        });
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
     * @param request,response get tokens from request and set tokens on response
     */
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        String accessToken = cookieUtil.getCookieValue(request, "accessToken");
        String refreshToken = cookieUtil.getCookieValue(request, "refreshToken");

        if (accessToken == null && refreshToken == null) {
            return;
        }

        if (refreshToken != null) {
            redisUtil.delete(refreshPrefix + refreshToken);
        }

        if (accessToken != null) {
            long ttl = jwtUtil.getExpirationSeconds(accessToken); // remaining lifetime
            redisUtil.set("blacklist:" + accessToken, "revoked", ttl);
        }

        cookieUtil.clearCookie(response, "accessToken");
        cookieUtil.clearCookie(response, "refreshToken");
    }

    @Override
    public Optional<UserResponseDTO> verifyCode(String code, String email, HttpServletResponse response) {
        int verificationCode = Integer.parseInt(code);
        User user = userRepository.findByEmail(email).orElseThrow(() -> new UserNotFoundException("User not found"));

        VerificationObject vo = activeVerifications.get(verificationCode);

        if (vo == null) {
            throw new InvalidVerificationCodeException("Invalid or expired verification code.");
        }

        if (vo.getUserId().equals(user.getId()) && !vo.isExpired()) {
            user.setIsVerified(true);
            vo.markAsValidated();
            activeVerifications.remove(verificationCode);
            userRepository.save(user);
            generateTokens(user, response);
            return userRepository.findByEmail(email)
                    .map(UserMapper::toDto);
        } else {
            throw new InvalidVerificationCodeException("The one-time password (OTP) provided is either expired or does not match the generated code for this user.");
        }
    }

    @Override
    public void sendVerificationCode(String toEmail) {
        int verificationCode = generateCode();
        User user = userRepository.findByEmail(toEmail).orElseThrow(() -> new UserNotFoundException("User not found"));

        createVerification(user.getId(), verificationCode);

        notifyUser(
                toEmail,
                "SkillBoost Verification Code",
                "Your verification code is: " + verificationCode);
    }

    public void notifyUser(String toEmail, String subject, String message) {
        emailService.sendEmail(
                toEmail,
                subject,
                message,
                System.getenv("MAIL_USERNAME")
        );
    }

    public void createVerification(UUID userId, int code) {
        activeVerifications.put(code, new VerificationObject(userId, code));
        System.out.println("Verification Map: " + activeVerifications.toString() );

        if(activeVerifications.get(code) == null){
            throw new RuntimeException("Verification object does not exist");
        }
    }

    public int generateCode() {
        SecureRandom random = new SecureRandom();
        tempCode = 100000 + random.nextInt(900000);
        return tempCode;
    }

    @Override
    @Transactional
    public UserResponseDTO createUserByAdmin(CreateUserByAdminRequest request, String adminEmail) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException("A user already exists with this email.");
        }

        String tempPassword = generateTemporaryPassword();

        User user = new User();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(tempPassword));
        user.setRole(request.role());
        user.setIsVerified(true);
        user.setState(UserState.REGISTERED);
        user.setPremiumTier(com.amalitech.user.service.model.enums.PremiumTier.FREE);
        user.setLanguage("en");
        user.setTimezone("UTC");
        user.setTourStatus(GuidedTourStatus.NOT_STARTED);

        User savedUser = userRepository.save(user);

        UserProfile userProfile = new UserProfile();
        savedUser.setProfile(userProfile);
        userRepository.save(savedUser);

        emailService.sendAdminCreatedUserEmail(request.email(), tempPassword, adminEmail);

        log.info("Admin {} created new {} user: {}", adminEmail, request.role(), request.email());
        return UserMapper.toDto(savedUser);
    }

    private String generateTemporaryPassword() {
        String upperCaseLetters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lowerCaseLetters = "abcdefghijklmnopqrstuvwxyz";
        String numbers = "0123456789";
        String specialCharacters = "@$!%*?&";
        String allChars = upperCaseLetters + lowerCaseLetters + numbers + specialCharacters;

        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder();

        password.append(upperCaseLetters.charAt(random.nextInt(upperCaseLetters.length())));
        password.append(lowerCaseLetters.charAt(random.nextInt(lowerCaseLetters.length())));
        password.append(numbers.charAt(random.nextInt(numbers.length())));
        password.append(specialCharacters.charAt(random.nextInt(specialCharacters.length())));

        for (int i = 4; i < 12; i++) {
            password.append(allChars.charAt(random.nextInt(allChars.length())));
        }

        return password.toString();
    }
}