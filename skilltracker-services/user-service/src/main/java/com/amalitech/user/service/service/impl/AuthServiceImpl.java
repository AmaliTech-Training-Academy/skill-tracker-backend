package com.amalitech.user.service.service.impl;

import com.amalitech.user.service.config.PasswordConfig;
import com.amalitech.user.service.dto.UserRequestDTO;
import com.amalitech.user.service.dto.UserResponseDTO;
import com.amalitech.user.service.dto.request.CreateUserByAdminRequest;
import com.amalitech.user.service.dto.request.LoginRequest;
import com.amalitech.user.service.dto.response.AuthResponse;
import com.amalitech.user.service.events.AdminCreatedUserEvent;
import com.amalitech.user.service.exception.*;
import com.amalitech.user.service.mapper.UserMapper;
import com.amalitech.user.service.model.User;
import com.amalitech.user.service.model.UserProfile;
import com.amalitech.user.service.model.VerificationObject;
import com.amalitech.user.service.model.enums.GuidedTourStatus;
import com.amalitech.user.service.model.enums.PremiumTier;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Implementation of {@link AuthService} providing comprehensive authentication and authorization operations.
 * <p>
 * This service handles:
 * <ul>
 *   <li>User registration and email verification</li>
 *   <li>Login/logout with JWT token management</li>
 *   <li>Access and refresh token generation and rotation</li>
 *   <li>Password reset and change operations</li>
 *   <li>Admin-initiated user creation with temporary passwords</li>
 *   <li>One-time password (OTP) verification</li>
 * </ul>
 * <p>
 * Security features include:
 * <ul>
 *   <li>BCrypt password hashing</li>
 *   <li>Secure token storage in Redis with expiration</li>
 *   <li>Token blacklisting for logout</li>
 *   <li>Cryptographically secure random password generation</li>
 *   <li>HTTP-only secure cookies for token storage</li>
 * </ul>
 *
 * @see AuthService
 * @see JwtUtil
 * @see RedisUtil
 */
@Service
public class AuthServiceImpl implements AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final ApplicationEventPublisher eventPublisher;

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final PasswordConfig passwordConfig;
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

    /**
     * Minimum number of required character categories in generated passwords
     * (uppercase, lowercase, number, special character).
     */
    private static final int MIN_REQUIRED_CHARS = 4;

    /**
     * Thread-safe map storing active verification codes and their associated metadata.
     * Keys are verification codes, values are {@link VerificationObject} instances.
     */
    private final Map<Integer, VerificationObject> activeVerifications = new ConcurrentHashMap<>();

    @Value("${app.frontend-url}")
    private String frontendUrl;


    public AuthServiceImpl(
            ApplicationEventPublisher eventPublisher, UserRepository userRepository, PasswordConfig passwordConfig,
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
        this.eventPublisher = eventPublisher;
        this.userRepository = userRepository;
        this.passwordConfig = passwordConfig;
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
        
        UserProfile profile = new UserProfile();
        profile.setEmailNotifications(true);
        profile.setPushNotifications(true);
        user.setProfile(profile);
        
        User savedUser = userRepository.save(user);

        int verificationCode = generateCode();
        createVerification(savedUser.getId(), verificationCode);

        notifyUser(
                savedUser.getEmail(),
                "Account created successfully!",
                "Enter this verification code to verify your identity: " + verificationCode);

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
     */
    public void generateTokens(User user, HttpServletResponse response) {
        String accessToken = jwtUtil.generateAccessToken(user.getEmail(), user.getRole(), user.getId());
        String refreshToken = UUID.randomUUID().toString();
        redisUtil.set(refreshPrefix + refreshToken, user.getEmail(), refreshExpiration / 1000);
        cookieUtil.setSecureCookie(response, "accessToken", accessToken,
                jwtUtil.getExpirationSeconds(accessToken));
        cookieUtil.setSecureCookie(response, "refreshToken", refreshToken,
                refreshExpiration / 1000);
        log.info("tokens successfully generated for user: {}", user.getEmail());
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
        generateTokens(user, response);
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
            if (user.getIsVerified() == false) {
                throw new UnverifiedUserException("User not verified");

            }
            if (user.getState() == UserState.SUSPENDED) {
                throw new UserSuspendedException("User is suspended");
            }
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

        if (user.getState() == UserState.SUSPENDED) {
            throw new UserSuspendedException("User is suspended");
        }
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
        activeVerifications.entrySet().removeIf(entry ->
                entry.getValue().getUserId().equals(userId)
        );

        activeVerifications.put(code, new VerificationObject(userId, code));

        if(activeVerifications.get(code) == null){
            throw new RuntimeException("Verification object does not exist");
        }
    }

    public int generateCode() {
        SecureRandom random = new SecureRandom();
        tempCode = 100000 + random.nextInt(900000);
        return tempCode;
    }

    /**
     * Creates a new user account by an administrator with a temporary password.
     * <p>
     * This method performs the following operations:
     * <ul>
     *   <li>Validates that the email is not already registered</li>
     *   <li>Generates a temporary password for the new user</li>
     *   <li>Creates a user with default settings (verified, FREE tier, English language)</li>
     *   <li>Sends a welcome email with login credentials to the new user</li>
     *   <li>Logs the user creation event</li>
     * </ul>
     *
     * @param request the user creation request containing email and role information
     * @param adminEmail the email address of the administrator creating the user
     * @return a {@link UserResponseDTO} containing the created user's information
     * @throws EmailAlreadyExistsException if a user with the given email already exists
     * @see CreateUserByAdminRequest
     * @see UserResponseDTO
     */
    @Override
    @Transactional
    public UserResponseDTO createUserByAdmin(CreateUserByAdminRequest request, String adminEmail) {
        String loginUrl = frontendUrl + "/login";
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException("A user already exists with this email.");
        }

        String tempPassword = generateTemporaryPassword();

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(tempPassword))
                .role(request.role())
                .isVerified(true)
                .state(UserState.REGISTERED)
                .premiumTier(PremiumTier.FREE)
                .language("en")
                .timezone("UTC")
                .tourStatus(GuidedTourStatus.NOT_STARTED)
                .build();

        UserProfile userProfile = new UserProfile();
        user.setProfile(userProfile);

        User savedUser = userRepository.save(user);

        eventPublisher.publishEvent(new AdminCreatedUserEvent(
                savedUser.getId(),
                savedUser.getEmail(),
                tempPassword,
                adminEmail,
                loginUrl
        ));

        log.info("Admin {} created new {} user: {}", adminEmail, request.role(), request.email());
        return UserMapper.toDto(savedUser);
    }

    /**
     * Generates a cryptographically secure temporary password for new user accounts.
     * <p>
     * The generated password meets the following security requirements:
     * <ul>
     *   <li>Contains at least one uppercase letter (A-Z)</li>
     *   <li>Contains at least one lowercase letter (a-z)</li>
     *   <li>Contains at least one numeric digit (0-9)</li>
     *   <li>Contains at least one special character</li>
     *   <li>Total length configured via {@link PasswordConfig}</li>
     *   <li>Characters are randomly shuffled to prevent predictable patterns</li>
     * </ul>
     * <p>
     * Uses {@link SecureRandom} for cryptographic strength randomness.
     * Users should be prompted to change this password upon first login.
     *
     * @return a randomly generated temporary password meeting all security requirements
     * @see PasswordConfig
     */
    private String generateTemporaryPassword() {
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder();

        password.append(getRandomChar(passwordConfig.getUppercaseLetters(), random));
        password.append(getRandomChar(passwordConfig.getLowercaseLetters(), random));
        password.append(getRandomChar(passwordConfig.getNumbers(), random));
        password.append(getRandomChar(passwordConfig.getSpecialCharacters(), random));

        String allChars = passwordConfig.getAllCharacters();
        for (int i = MIN_REQUIRED_CHARS; i < passwordConfig.getLength(); i++) {
            password.append(getRandomChar(allChars, random));
        }

        List<Character> passwordChars = password.chars()
                .mapToObj(c -> (char) c)
                .collect(Collectors.toList());
        Collections.shuffle(passwordChars, random);

        return passwordChars.stream()
                .map(String::valueOf)
                .collect(Collectors.joining());
    }


    /**
     * Selects a random character from the given character set using secure randomness.
     * <p>
     * This is a helper method used by password generation to ensure cryptographic
     * randomness in character selection.
     *
     * @param characters the character set to choose from
     * @param random the {@link SecureRandom} instance for cryptographic randomness
     * @return a randomly selected character from the provided set
     */
    private char getRandomChar(String characters, SecureRandom random) {
        return characters.charAt(random.nextInt(characters.length()));
    }
}