package com.amalitech.user.service;

import com.amalitech.user.service.dto.UserRequestDTO;
import com.amalitech.user.service.dto.UserResponseDTO;
import com.amalitech.user.service.dto.request.LoginRequest;
import com.amalitech.user.service.dto.response.AuthResponse;
import com.amalitech.user.service.exception.EmailAlreadyExistsException;
import com.amalitech.user.service.exception.InvalidVerificationCodeException;
import com.amalitech.user.service.model.User;
import com.amalitech.user.service.model.enums.Role;
import com.amalitech.user.service.model.enums.UserState;
import com.amalitech.user.service.repository.UserRepository;
import com.amalitech.user.service.security.CustomUserDetails;
import com.amalitech.user.service.security.util.JwtUtil;
import com.amalitech.user.service.service.EmailService;
import com.amalitech.user.service.service.impl.AuthServiceImpl;
import com.amalitech.user.service.util.CookieUtil;
import com.amalitech.user.service.util.RedisUtil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private JwtUtil jwtUtil;
    @Mock private BCryptPasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;
    @Mock private RedisUtil redisUtil;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private CookieUtil cookieUtil;


    private AuthServiceImpl authService;

    private User testUser;
    private UserRequestDTO userRequestDTO;
    private LoginRequest loginRequest;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    private static final String EMAIL = "test@example.com";
    private static final String PASSWORD = "password123";
    private static final String ENCODED_PASSWORD = "$2a$12$encodedhash";
    private static final UUID USER_ID = UUID.randomUUID();
    private static final long REFRESH_EXPIRATION = 604800000L;
    private static final long RESET_EXPIRATION = 3600000L;
    private static final String REFRESH_PREFIX = "refresh:";
    private static final String RESET_PREFIX = "reset:";
    private static final String APP_BASE_URL = "http://localhost:8080";

    @BeforeEach
    void setUp() {
        // Manually construct AuthServiceImpl with all 12 required args
        authService = new AuthServiceImpl(
                userRepository,
                jwtUtil,
                passwordEncoder,
                emailService,
                redisUtil,
                REFRESH_EXPIRATION,
                RESET_EXPIRATION,
                REFRESH_PREFIX,
                RESET_PREFIX,
                APP_BASE_URL,
                authenticationManager,
                cookieUtil
        );

        // Reset tempCode for verifyCode tests
        setPrivateField(authService, "tempCode", 0);

        // Test data
        userRequestDTO = new UserRequestDTO(EMAIL, PASSWORD);
        loginRequest = new LoginRequest(EMAIL, PASSWORD);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();

        testUser = new User();
        testUser.setId(USER_ID);
        testUser.setEmail(EMAIL);
        testUser.setPasswordHash(ENCODED_PASSWORD);
        testUser.setRole(Role.USER);
        testUser.setState(UserState.REGISTERED);
        testUser.setIsVerified(false);
    }

    // Helper to set private field
    private void setPrivateField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field: " + fieldName, e);
        }
    }

    @Test
    void createUser_Success() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setId(USER_ID);
            u.setUpdatedAt(LocalDateTime.now());
            return u;
        });

        doNothing().when(emailService).sendEmail(
                anyString(),
                anyString(),
                anyString(),
                nullable(String.class)
        );

        UserResponseDTO result = authService.createUser(userRequestDTO);

        assertNotNull(result);
        assertEquals(EMAIL, result.email());
        verify(userRepository).save(any(User.class));
        verify(emailService).sendEmail(
                eq(EMAIL),
                eq("Account created successfully!"),
                contains("verification code"),
                isNull()
        );
    }

    @Test
    void createUser_EmailExists_ThrowsException() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(true);
        assertThrows(EmailAlreadyExistsException.class, () -> authService.createUser(userRequestDTO));
    }

    @Test
    void login_Success() {
        Authentication auth = mock(Authentication.class);
        CustomUserDetails userDetails = new CustomUserDetails(testUser);

        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(auth.getPrincipal()).thenReturn(userDetails);
        when(jwtUtil.generateAccessToken(EMAIL, Role.USER, USER_ID)).thenReturn("access-jwt");
        when(jwtUtil.getExpirationSeconds("access-jwt")).thenReturn(900L);

        UUID refreshToken = UUID.fromString("00000000-0000-0000-0000-000000000001");

        try (MockedStatic<UUID> mockedUuid = mockStatic(UUID.class)) {
            mockedUuid.when(UUID::randomUUID).thenReturn(refreshToken);

            AuthResponse result = authService.login(loginRequest, response);

            assertEquals("tokens generated and set in httpOnly cookie", result.message());

            verify(redisUtil).set(
                    eq(REFRESH_PREFIX + refreshToken.toString()),
                    eq(EMAIL),
                    anyLong()
            );

            verify(cookieUtil).setSecureCookie(response, "accessToken", "access-jwt", 900L);
            verify(cookieUtil).setSecureCookie(
                    response,
                    "refreshToken",
                    refreshToken.toString(),
                    REFRESH_EXPIRATION / 1000
            );
        }
    }


    @Test
    void refresh_ValidToken_Success() {
        String oldToken = "old-refresh";
        String newAccess = "new-access";

        when(cookieUtil.getCookieValue(request, "refreshToken")).thenReturn(oldToken);
        when(redisUtil.get(REFRESH_PREFIX + oldToken)).thenReturn(EMAIL);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(testUser));
        when(jwtUtil.generateAccessToken(EMAIL, Role.USER, USER_ID)).thenReturn(newAccess);
        when(jwtUtil.getExpirationSeconds(newAccess)).thenReturn(900L);

        AuthResponse result = authService.refresh(request, response);

        assertEquals("tokens refreshed successfully", result.message());
        verify(redisUtil).delete(REFRESH_PREFIX + oldToken);
        verify(redisUtil).set(startsWith(REFRESH_PREFIX), eq(EMAIL), anyLong());
        verify(cookieUtil).setSecureCookie(response, "accessToken", newAccess, 900L);
    }

    @Test
    void forgotPassword_Success() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(testUser));
        doNothing().when(emailService).sendResetEmail(anyString(), anyString());

        UUID resetToken = UUID.fromString("11111111-1111-1111-1111-111111111111");

        try (MockedStatic<UUID> mockedUuid = mockStatic(UUID.class)) {
            mockedUuid.when(UUID::randomUUID).thenReturn(resetToken);

            authService.forgotPassword(EMAIL);

            verify(redisUtil).set(
                    eq(RESET_PREFIX + resetToken.toString()),
                    eq(EMAIL),
                    eq(RESET_EXPIRATION / 1000)
            );

            verify(emailService).sendResetEmail(
                    eq(EMAIL),
                    eq(APP_BASE_URL + "/api/v1/auth/reset-password?token=" + resetToken)
            );
        }
    }


    @Test
    void resetPassword_ValidToken_Success() {
        String token = "valid-token";
        when(redisUtil.get(RESET_PREFIX + token)).thenReturn(EMAIL);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("newPass")).thenReturn("encoded");

        authService.resetPassword(token, "newPass");

        verify(userRepository).save(testUser);
        assertEquals("encoded", testUser.getPasswordHash());
        verify(redisUtil).delete(RESET_PREFIX + token);
    }

    @Test
    void changePassword_Success() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
        when(passwordEncoder.encode("newPass")).thenReturn("newHash");

        authService.changePassword(EMAIL, PASSWORD, "newPass");

        verify(userRepository).save(testUser);
        assertEquals("newHash", testUser.getPasswordHash());
    }

    @Test
    void logout_WithTokens_ClearsAll() {
        when(cookieUtil.getCookieValue(request, "accessToken")).thenReturn("access-jwt");
        when(cookieUtil.getCookieValue(request, "refreshToken")).thenReturn("refresh-token");
        when(jwtUtil.getExpirationSeconds("access-jwt")).thenReturn(100L);

        authService.logout(request, response);

        verify(redisUtil).delete(REFRESH_PREFIX + "refresh-token");
        verify(redisUtil).set("blacklist:access-jwt", "revoked", 100L);
        verify(cookieUtil).clearCookie(response, "accessToken");
        verify(cookieUtil).clearCookie(response, "refreshToken");
    }

    @Test
    void verifyCode_ValidCode_ReturnsUser() {
        Integer code = authService.generateCode();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(testUser));

        Optional<UserResponseDTO> result = authService.verifyCode(code.toString(), EMAIL);

        assertTrue(result.isPresent());
        assertEquals(EMAIL, result.get().email());
    }

    @Test
    void verifyCode_InvalidCode_ThrowsException() {
        authService.generateCode();

        assertThrows(InvalidVerificationCodeException.class, () -> authService.verifyCode("000000", EMAIL));
    }

    @Test
    void generateCode_ReturnsSixDigits() {
        Integer code = authService.generateCode();
        assertTrue(code >= 100000 && code <= 999999);
    }
}