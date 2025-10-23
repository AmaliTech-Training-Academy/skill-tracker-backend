package com.amalitech.user.service;
import com.amalitech.user.service.dto.UserRequestDTO;
import com.amalitech.user.service.dto.UserResponseDTO;
import com.amalitech.user.service.dto.request.LoginRequest;
import com.amalitech.user.service.dto.request.RegisterRequest;
import com.amalitech.user.service.dto.response.AuthResponse;
import com.amalitech.user.service.exception.*;
import com.amalitech.user.service.mapper.UserMapper;
import com.amalitech.user.service.model.User;
import com.amalitech.user.service.model.enums.GuidedTourStatus;
import com.amalitech.user.service.model.enums.PremiumTier;
import com.amalitech.user.service.model.enums.Role;
import com.amalitech.user.service.model.enums.UserState;
import com.amalitech.user.service.repository.UserRepository;
import com.amalitech.user.service.security.CustomUserDetails;
import com.amalitech.user.service.security.util.JwtUtil;
import com.amalitech.user.service.service.EmailService;
import com.amalitech.user.service.service.impl.AuthServiceImpl;
import com.amalitech.user.service.util.RedisUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private RedisUtil redisUtil;

    @Mock
    private Authentication authentication;

    private AuthServiceImpl authService;

    private User testUser;
    private UUID userId;
    private final long refreshExpiration = 86400000L;
    private final long resetExpiration = 3600000L;
    private final String refreshPrefix = "refresh:";
    private final String resetPrefix = "reset:";
    private final String appBaseUrl = "http://localhost:8080";

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
                userRepository, jwtUtil, passwordEncoder, emailService, redisUtil,
                refreshExpiration, resetExpiration, refreshPrefix,
                resetPrefix, appBaseUrl, authenticationManager
        );

        userId = UUID.randomUUID();
        testUser = new User();
        testUser.setId(userId);
        testUser.setEmail("test@example.com");
        testUser.setUsername("testuser");
        testUser.setPasswordHash("encodedPassword");
        testUser.setRole(Role.USER);
        testUser.setState(UserState.REGISTERED);
        testUser.setPremiumTier(PremiumTier.FREE);
        testUser.setTourStatus(GuidedTourStatus.NOT_STARTED);
        testUser.setIsVerified(false);
        testUser.setLanguage("en");
        testUser.setTimezone("UTC");
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void createUser_ShouldCreateUser_WhenEmailNotExists() {
        UserRequestDTO request = new UserRequestDTO("test@example.com", "password123");
        User mappedUser = new User();
        mappedUser.setEmail("test@example.com");
        mappedUser.setPasswordHash("mappedEncodedPassword");

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        try (MockedStatic<UserMapper> userMapperMock = mockStatic(UserMapper.class)) {

            userMapperMock.when(() -> UserMapper.toEntity(request)).thenReturn(mappedUser);
            userMapperMock.when(() -> UserMapper.toDto(testUser)).thenReturn(
                    UserResponseDTO.builder()
                            .id(userId)
                            .email("test@example.com")
                            .username("testuser")
                            .role(Role.USER)
                            .state(UserState.REGISTERED)
                            .is_verified(false)
                            .premiumTier(PremiumTier.FREE)
                            .language("en")
                            .timezone("UTC")
                            .updatedAt(testUser.getUpdatedAt())
                            .lastLoginAt(testUser.getLastLoginAt())
                            .build()
            );

            UserResponseDTO result = authService.createUser(request);

            assertNotNull(result);
            assertEquals("test@example.com", result.email());
            verify(userRepository).existsByEmail(request.email());
            verify(userRepository).save(mappedUser);
            verify(emailService).sendEmail(eq("test@example.com"), anyString(), anyString(), isNull());
        }
    }

    @Test
    void createUser_ShouldThrowEmailAlreadyExistsException_WhenEmailAlreadyExists() {
        UserRequestDTO request = new UserRequestDTO("existing@example.com", "password123");
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        EmailAlreadyExistsException exception = assertThrows(
                EmailAlreadyExistsException.class,
                () -> authService.createUser(request)
        );
        assertEquals("A user already exists with this email.", exception.getMessage());
        verify(userRepository).existsByEmail(request.email());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_ShouldReturnAuthResponse_WhenCredentialsValid() {
        LoginRequest request = new LoginRequest("test@example.com", "password");
        CustomUserDetails userDetails = new CustomUserDetails(testUser);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(jwtUtil.generateAccessToken(anyString(), any(Role.class), any(UUID.class)))
                .thenReturn("access-token");

        AuthResponse result = authService.login(request);

        assertNotNull(result);
        assertEquals("access-token", result.accessToken());
        assertNotNull(result.refreshToken());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(redisUtil).set(anyString(), eq("test@example.com"), eq(refreshExpiration / 1000));
    }

    @Test
    void login_ShouldThrowException_WhenAuthenticationFails() {
        LoginRequest request = new LoginRequest("test@example.com", "wrong-password");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(redisUtil, never()).set(anyString(), anyString(), anyLong());
    }

    @Test
    void generateTokens_ShouldCreateValidTokens() {
        when(jwtUtil.generateAccessToken(anyString(), any(Role.class), any(UUID.class)))
                .thenReturn("access-token");

        AuthResponse result = authService.generateTokens(testUser);

        assertNotNull(result);
        assertEquals("access-token", result.accessToken());
        assertNotNull(result.refreshToken());
        verify(jwtUtil).generateAccessToken("test@example.com", Role.USER, userId);
        verify(redisUtil).set(anyString(), eq("test@example.com"), eq(refreshExpiration / 1000));
    }

    @Test
    void refresh_ShouldReturnNewTokens_WhenRefreshTokenValid() {
        String refreshToken = "valid-refresh-token";
        String newAccessToken = "new-access-token";

        when(redisUtil.get(refreshPrefix + refreshToken)).thenReturn("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(jwtUtil.generateAccessToken(anyString(), any(Role.class), any(UUID.class)))
                .thenReturn(newAccessToken);

        AuthResponse result = authService.refresh(refreshToken);

        assertNotNull(result);
        assertEquals(newAccessToken, result.accessToken());
        assertNotNull(result.refreshToken());
        verify(redisUtil).delete(refreshPrefix + refreshToken);
        verify(redisUtil).set(anyString(), eq("test@example.com"), eq(refreshExpiration / 1000));
        verify(jwtUtil).generateAccessToken("test@example.com", Role.USER, userId);
    }

    @Test
    void refresh_ShouldThrowRefreshTokenException_WhenRefreshTokenInvalid() {
        String invalidToken = "invalid-token";
        when(redisUtil.get(refreshPrefix + invalidToken)).thenReturn(null);

        RefreshTokenException exception = assertThrows(
                RefreshTokenException.class,
                () -> authService.refresh(invalidToken)
        );
        assertEquals("Authentication failed. Please log in again.", exception.getMessage());
        verify(redisUtil, never()).delete(anyString());
    }

    @Test
    void refresh_ShouldThrowRefreshTokenException_WhenUserNotFound() {
        String refreshToken = "valid-token";
        when(redisUtil.get(refreshPrefix + refreshToken)).thenReturn("nonexistent@example.com");
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        RefreshTokenException exception = assertThrows(
                RefreshTokenException.class,
                () -> authService.refresh(refreshToken)
        );
        assertEquals("User not found", exception.getMessage());
        verify(redisUtil, never()).delete(anyString());
    }

    @Test
    void refresh_ShouldThrowRefreshTokenException_WhenUserSuspended() {
        String refreshToken = "valid-token";
        testUser.setState(UserState.SUSPENDED);

        when(redisUtil.get(refreshPrefix + refreshToken)).thenReturn("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        RefreshTokenException exception = assertThrows(
                RefreshTokenException.class,
                () -> authService.refresh(refreshToken)
        );
        assertEquals("User is suspended", exception.getMessage());
        verify(redisUtil).delete(refreshPrefix + refreshToken);
    }

    @Test
    void forgotPassword_ShouldSendResetEmail_WhenUserExists() {
        String email = "test@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));

        authService.forgotPassword(email);

        verify(userRepository).findByEmail(email);
        verify(redisUtil).set(anyString(), eq(email), eq(resetExpiration / 1000));
        verify(emailService).sendResetEmail(eq(email), anyString());
    }

    @Test
    void forgotPassword_ShouldThrowRefreshTokenException_WhenUserNotFound() {
        String email = "nonexistent@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        RefreshTokenException exception = assertThrows(
                RefreshTokenException.class,
                () -> authService.forgotPassword(email)
        );
        assertEquals("User not found", exception.getMessage());
        verify(redisUtil, never()).set(anyString(), anyString(), anyLong());
        verify(emailService, never()).sendResetEmail(anyString(), anyString());
    }

    @Test
    void resetPassword_ShouldUpdatePassword_WhenTokenValid() {
        String token = "valid-token";
        String newPassword = "newPassword123";

        when(redisUtil.get(resetPrefix + token)).thenReturn("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode(newPassword)).thenReturn("newEncodedPassword");

        authService.resetPassword(token, newPassword);

        verify(redisUtil).get(resetPrefix + token);
        verify(passwordEncoder).encode(newPassword);
        verify(userRepository).save(testUser);
        verify(redisUtil).delete(resetPrefix + token);
    }

    @Test
    void resetPassword_ShouldThrowInvalidTokenException_WhenTokenInvalid() {
        String token = "invalid-token";
        String newPassword = "newPassword123";

        when(redisUtil.get(resetPrefix + token)).thenReturn(null);

        InvalidTokenException exception = assertThrows(
                InvalidTokenException.class,
                () -> authService.resetPassword(token, newPassword)
        );
        assertEquals("Invalid or expired token", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void resetPassword_ShouldThrowRefreshTokenException_WhenUserNotFound() {
        String token = "valid-token";
        String newPassword = "newPassword123";

        when(redisUtil.get(resetPrefix + token)).thenReturn("nonexistent@example.com");
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        RefreshTokenException exception = assertThrows(
                RefreshTokenException.class,
                () -> authService.resetPassword(token, newPassword)
        );
        assertEquals("User not found", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }



    @Test
    void changePassword_ShouldThrowRefreshTokenException_WhenUserNotFound() {
        String email = "nonexistent@example.com";
        String oldPassword = "oldPassword123";
        String newPassword = "newPassword123";

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        RefreshTokenException exception = assertThrows(
                RefreshTokenException.class,
                () -> authService.changePassword(email, oldPassword, newPassword)
        );
        assertEquals("User not found", exception.getMessage());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void changePassword_ShouldThrowInvalidPasswordException_WhenOldPasswordIncorrect() {
        String email = "test@example.com";
        String oldPassword = "wrongPassword123";
        String newPassword = "newPassword123";

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(oldPassword, testUser.getPasswordHash())).thenReturn(false);

        InvalidPasswordException exception = assertThrows(
                InvalidPasswordException.class,
                () -> authService.changePassword(email, oldPassword, newPassword)
        );
        assertEquals("Invalid old password", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void logout_ShouldRevokeTokens() {
        String accessToken = "access-token";
        String refreshToken = "refresh-token";
        when(jwtUtil.getExpirationSeconds(accessToken)).thenReturn(3600L);

        authService.logout(accessToken, refreshToken);

        verify(redisUtil).delete(refreshPrefix + refreshToken);
        verify(redisUtil).set(eq("blacklist:" + accessToken), eq("revoked"), eq(3600L));
    }

    @Test
    void logout_ShouldHandleZeroExpirationGracefully() {
        String accessToken = "expired-token";
        String refreshToken = "refresh-token";
        when(jwtUtil.getExpirationSeconds(accessToken)).thenReturn(0L);

        authService.logout(accessToken, refreshToken);

        verify(redisUtil).delete(refreshPrefix + refreshToken);
        verify(redisUtil).set(eq("blacklist:" + accessToken), eq("revoked"), eq(0L));
    }

    @Test
    void updatePassword_ShouldEncodeAndSaveNewPassword() {
        String newPassword = "newPassword123";
        when(passwordEncoder.encode(newPassword)).thenReturn("encodedNewPassword");

        authService.updatePassword(testUser, newPassword);

        verify(passwordEncoder).encode(newPassword);
        verify(userRepository).save(testUser);
        assertEquals("encodedNewPassword", testUser.getPasswordHash());
    }

    @Test
    void verifyCode_ShouldReturnUser_WhenCodeIsValid() throws Exception {
        String code = "123456";
        String email = "test@example.com";

        Field tempCodeField = AuthServiceImpl.class.getDeclaredField("tempCode");
        tempCodeField.setAccessible(true);
        tempCodeField.set(authService, 123456);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));

        try (MockedStatic<UserMapper> userMapperMock = mockStatic(UserMapper.class)) {
            userMapperMock.when(() -> UserMapper.toDto(testUser)).thenReturn(
                    UserResponseDTO.builder()
                            .id(userId)
                            .email("test@example.com")
                            .username("testuser")
                            .role(Role.USER)
                            .state(UserState.REGISTERED)
                            .is_verified(false)
                            .premiumTier(PremiumTier.FREE)
                            .language("en")
                            .timezone("UTC")
                            .updatedAt(testUser.getUpdatedAt())
                            .lastLoginAt(testUser.getLastLoginAt())
                            .build()
            );

            Optional<UserResponseDTO> result = authService.verifyCode(code, email);

            assertTrue(result.isPresent());
            assertEquals("test@example.com", result.get().email());
        }
    }

    @Test
    void verifyCode_ShouldThrowException_WhenCodeIsInvalid() throws Exception {
        String code = "wrongCode";
        String email = "test@example.com";

        Field tempCodeField = AuthServiceImpl.class.getDeclaredField("tempCode");
        tempCodeField.setAccessible(true);
        tempCodeField.set(authService, 123456);

        assertThrows(InvalidVerificationCodeException.class,
                () -> authService.verifyCode(code, email));
    }

    @Test
    void generateCode_ShouldReturnSixDigitNumber() {
        Integer result = authService.generateCode();

        assertNotNull(result);
        assertTrue(result >= 100000 && result <= 999999);
    }
}