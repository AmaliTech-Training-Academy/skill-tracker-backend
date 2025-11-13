package com.amalitech.user.service;

import com.amalitech.user.service.config.PasswordConfig;
import com.amalitech.user.service.dto.request.CreateUserByAdminRequest;
import com.amalitech.user.service.dto.UserResponseDTO;
import com.amalitech.user.service.exception.EmailAlreadyExistsException;
import com.amalitech.user.service.model.User;
import com.amalitech.user.service.model.UserProfile;
import com.amalitech.user.service.model.enums.GuidedTourStatus;
import com.amalitech.user.service.model.enums.PremiumTier;
import com.amalitech.user.service.model.enums.Role;
import com.amalitech.user.service.model.enums.UserState;
import com.amalitech.user.service.repository.UserRepository;
import com.amalitech.user.service.service.EmailService;
import com.amalitech.user.service.service.impl.AuthServiceImpl;
import com.amalitech.user.service.util.CookieUtil;
import com.amalitech.user.service.util.RedisUtil;
import com.amalitech.user.service.security.util.JwtUtil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for admin user creation feature.
 * Tests cover service layer, DTO validation, password generation,
 * email sending, and error handling.
 */
@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AdminUserCreationTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordConfig passwordConfig;
    @Mock private JwtUtil jwtUtil;
    @Mock private BCryptPasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;
    @Mock private RedisUtil redisUtil;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private CookieUtil cookieUtil;

    private AuthServiceImpl authService;

    private static final String ADMIN_EMAIL = "admin@example.com";
    private static final String NEW_USER_EMAIL = "newuser@example.com";
    private static final long REFRESH_EXPIRATION = 604800000L;
    private static final long RESET_EXPIRATION = 3600000L;
    private static final String REFRESH_PREFIX = "refresh:";
    private static final String RESET_PREFIX = "reset:";
    private static final String APP_BASE_URL = "http://localhost:8080";
    private static final String LOGIN_URL = "http://localhost:3000/login";

    @BeforeEach
    void setUp() {
        // Configure password config mock
        String uppercase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lowercase = "abcdefghijklmnopqrstuvwxyz";
        String numbers = "0123456789";
        String special = "@$!%*?&";
        lenient().when(passwordConfig.getUppercaseLetters()).thenReturn(uppercase);
        lenient().when(passwordConfig.getLowercaseLetters()).thenReturn(lowercase);
        lenient().when(passwordConfig.getNumbers()).thenReturn(numbers);
        lenient().when(passwordConfig.getSpecialCharacters()).thenReturn(special);
        lenient().when(passwordConfig.getLength()).thenReturn(12);
        lenient().when(passwordConfig.getAllCharacters()).thenReturn(uppercase + lowercase + numbers + special);
        lenient().doNothing().when(emailService).sendAdminCreatedUserEmail(anyString(), anyString(), anyString(), anyString());

        authService = new AuthServiceImpl(
                userRepository,
                passwordConfig,
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
    }

    // ==================== DTO Validation Tests ====================

    @Test
    @DisplayName("CreateUserByAdminRequest - Valid USER role")
    void createUserByAdminRequest_ValidUserRole() {
        CreateUserByAdminRequest request = CreateUserByAdminRequest.builder()
                .email(NEW_USER_EMAIL)
                .role(Role.USER)
                .build();

        assertEquals(NEW_USER_EMAIL, request.email());
        assertEquals(Role.USER, request.role());
    }

    @Test
    @DisplayName("CreateUserByAdminRequest - Valid ADMIN role")
    void createUserByAdminRequest_ValidAdminRole() {
        CreateUserByAdminRequest request = CreateUserByAdminRequest.builder()
                .email(NEW_USER_EMAIL)
                .role(Role.ADMIN)
                .build();

        assertEquals(NEW_USER_EMAIL, request.email());
        assertEquals(Role.ADMIN, request.role());
    }

    // ==================== Service Layer Tests ====================

    @Test
    @DisplayName("createUserByAdmin - Successfully creates USER")
    void createUserByAdmin_SuccessfullyCreatesUser() {
        CreateUserByAdminRequest request = CreateUserByAdminRequest.builder()
                .email(NEW_USER_EMAIL)
                .role(Role.USER)
                .build();

        UUID newUserId = UUID.randomUUID();

        when(userRepository.existsByEmail(NEW_USER_EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-temp-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(newUserId);
            user.setUpdatedAt(LocalDateTime.now());
            return user;
        });

        UserResponseDTO result = authService.createUserByAdmin(request, ADMIN_EMAIL);

        assertNotNull(result);
        assertEquals(NEW_USER_EMAIL, result.email());
        assertEquals(Role.USER, result.role());
        assertTrue(result.is_verified());
        assertEquals(UserState.REGISTERED, result.state());

        verify(userRepository).existsByEmail(NEW_USER_EMAIL);
        verify(passwordEncoder).encode(anyString());
        verify(userRepository, times(2)).save(any(User.class));
        verify(emailService).sendAdminCreatedUserEmail(eq(NEW_USER_EMAIL), anyString(), eq(ADMIN_EMAIL), isNull());
    }

    @Test
    @DisplayName("createUserByAdmin - Successfully creates ADMIN")
    void createUserByAdmin_SuccessfullyCreatesAdmin() {
        CreateUserByAdminRequest request = CreateUserByAdminRequest.builder()
                .email(NEW_USER_EMAIL)
                .role(Role.ADMIN)
                .build();

        UUID newAdminId = UUID.randomUUID();

        when(userRepository.existsByEmail(NEW_USER_EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-temp-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(newAdminId);
            user.setUpdatedAt(LocalDateTime.now());
            return user;
        });

        UserResponseDTO result = authService.createUserByAdmin(request, ADMIN_EMAIL);

        assertNotNull(result);
        assertEquals(NEW_USER_EMAIL, result.email());
        assertEquals(Role.ADMIN, result.role());
        assertTrue(result.is_verified());

        verify(userRepository).existsByEmail(NEW_USER_EMAIL);
        verify(userRepository, times(2)).save(any(User.class));
    }

    @Test
    @DisplayName("createUserByAdmin - User with duplicate email throws exception")
    void createUserByAdmin_DuplicateEmail_ThrowsException() {
        CreateUserByAdminRequest request = CreateUserByAdminRequest.builder()
                .email(NEW_USER_EMAIL)
                .role(Role.USER)
                .build();

        when(userRepository.existsByEmail(NEW_USER_EMAIL)).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class,
                () -> authService.createUserByAdmin(request, ADMIN_EMAIL));

        verify(userRepository).existsByEmail(NEW_USER_EMAIL);
        verify(userRepository, never()).save(any(User.class));
        verify(emailService, never()).sendAdminCreatedUserEmail(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("createUserByAdmin - Sets correct user properties")
    void createUserByAdmin_SetsCorrectProperties() {
        CreateUserByAdminRequest request = CreateUserByAdminRequest.builder()
                .email(NEW_USER_EMAIL)
                .role(Role.USER)
                .build();

        UUID newUserId = UUID.randomUUID();

        when(userRepository.existsByEmail(NEW_USER_EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(newUserId);
            user.setUpdatedAt(LocalDateTime.now());
            return user;
        });

        authService.createUserByAdmin(request, ADMIN_EMAIL);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(2)).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertEquals(NEW_USER_EMAIL, savedUser.getEmail());
        assertEquals(Role.USER, savedUser.getRole());
        assertTrue(savedUser.getIsVerified());
        assertEquals(UserState.REGISTERED, savedUser.getState());
        assertEquals(PremiumTier.FREE, savedUser.getPremiumTier());
        assertEquals("en", savedUser.getLanguage());
        assertEquals("UTC", savedUser.getTimezone());
        assertEquals(GuidedTourStatus.NOT_STARTED, savedUser.getTourStatus());
        assertNull(savedUser.getTaskGenerationStatus());
    }

    @Test
    @DisplayName("createUserByAdmin - Creates UserProfile")
    void createUserByAdmin_CreatesUserProfile() {
        CreateUserByAdminRequest request = CreateUserByAdminRequest.builder()
                .email(NEW_USER_EMAIL)
                .role(Role.USER)
                .build();

        UUID newUserId = UUID.randomUUID();

        when(userRepository.existsByEmail(NEW_USER_EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(newUserId);
            user.setUpdatedAt(LocalDateTime.now());
            return user;
        });

        authService.createUserByAdmin(request, ADMIN_EMAIL);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(2)).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertNotNull(savedUser.getUserProfile());
    }

    @Test
    @DisplayName("createUserByAdmin - Sends email with correct parameters")
    void createUserByAdmin_SendsEmailWithCorrectParameters() {
        CreateUserByAdminRequest request = CreateUserByAdminRequest.builder()
                .email(NEW_USER_EMAIL)
                .role(Role.USER)
                .build();

        UUID newUserId = UUID.randomUUID();

        when(userRepository.existsByEmail(NEW_USER_EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(newUserId);
            user.setUpdatedAt(LocalDateTime.now());
            return user;
        });

        authService.createUserByAdmin(request, ADMIN_EMAIL);

        verify(emailService).sendAdminCreatedUserEmail(
                eq(NEW_USER_EMAIL),
                anyString(),
                eq(ADMIN_EMAIL),
                isNull()
        );
    }

    // ==================== Password Generation Tests ====================

    @Test
    @DisplayName("Password generation - Generates secure 12-character password")
    void passwordGeneration_LengthRequirement() {
        CreateUserByAdminRequest request = CreateUserByAdminRequest.builder()
                .email(NEW_USER_EMAIL)
                .role(Role.USER)
                .build();

        UUID newUserId = UUID.randomUUID();
        ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);

        when(userRepository.existsByEmail(NEW_USER_EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(newUserId);
            user.setUpdatedAt(LocalDateTime.now());
            return user;
        });

        authService.createUserByAdmin(request, ADMIN_EMAIL);

        verify(passwordEncoder).encode(passwordCaptor.capture());
        String generatedPassword = passwordCaptor.getValue();
        assertEquals(12, generatedPassword.length());
    }

    @Test
    @DisplayName("Password generation - Contains uppercase letter")
    void passwordGeneration_ContainsUppercase() {
        CreateUserByAdminRequest request = CreateUserByAdminRequest.builder()
                .email(NEW_USER_EMAIL)
                .role(Role.USER)
                .build();

        UUID newUserId = UUID.randomUUID();
        ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);

        when(userRepository.existsByEmail(NEW_USER_EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(newUserId);
            user.setUpdatedAt(LocalDateTime.now());
            return user;
        });

        authService.createUserByAdmin(request, ADMIN_EMAIL);

        verify(passwordEncoder).encode(passwordCaptor.capture());
        String generatedPassword = passwordCaptor.getValue();
        assertTrue(generatedPassword.matches(".*[A-Z].*"), "Password must contain uppercase letter");
    }

    @Test
    @DisplayName("Password generation - Contains lowercase letter")
    void passwordGeneration_ContainsLowercase() {
        CreateUserByAdminRequest request = CreateUserByAdminRequest.builder()
                .email(NEW_USER_EMAIL)
                .role(Role.USER)
                .build();

        UUID newUserId = UUID.randomUUID();
        ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);

        when(userRepository.existsByEmail(NEW_USER_EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(newUserId);
            user.setUpdatedAt(LocalDateTime.now());
            return user;
        });

        authService.createUserByAdmin(request, ADMIN_EMAIL);

        verify(passwordEncoder).encode(passwordCaptor.capture());
        String generatedPassword = passwordCaptor.getValue();
        assertTrue(generatedPassword.matches(".*[a-z].*"), "Password must contain lowercase letter");
    }

    @Test
    @DisplayName("Password generation - Contains digit")
    void passwordGeneration_ContainsDigit() {
        CreateUserByAdminRequest request = CreateUserByAdminRequest.builder()
                .email(NEW_USER_EMAIL)
                .role(Role.USER)
                .build();

        UUID newUserId = UUID.randomUUID();
        ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);

        when(userRepository.existsByEmail(NEW_USER_EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(newUserId);
            user.setUpdatedAt(LocalDateTime.now());
            return user;
        });

        authService.createUserByAdmin(request, ADMIN_EMAIL);

        verify(passwordEncoder).encode(passwordCaptor.capture());
        String generatedPassword = passwordCaptor.getValue();
        assertTrue(generatedPassword.matches(".*\\d.*"), "Password must contain digit");
    }

    @Test
    @DisplayName("Password generation - Contains special character")
    void passwordGeneration_ContainsSpecialCharacter() {
        CreateUserByAdminRequest request = CreateUserByAdminRequest.builder()
                .email(NEW_USER_EMAIL)
                .role(Role.USER)
                .build();

        UUID newUserId = UUID.randomUUID();
        ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);

        when(userRepository.existsByEmail(NEW_USER_EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(newUserId);
            user.setUpdatedAt(LocalDateTime.now());
            return user;
        });

        authService.createUserByAdmin(request, ADMIN_EMAIL);

        verify(passwordEncoder).encode(passwordCaptor.capture());
        String generatedPassword = passwordCaptor.getValue();
        assertTrue(generatedPassword.matches(".*[@$!%*?&].*"), "Password must contain special character");
    }

    // ==================== Edge Cases & Error Handling ====================

    @Test
    @DisplayName("createUserByAdmin - Multiple calls generate different passwords")
    void createUserByAdmin_MultipleCallsGenerateDifferentPasswords() {
        CreateUserByAdminRequest request1 = CreateUserByAdminRequest.builder()
                .email("user1@example.com")
                .role(Role.USER)
                .build();

        CreateUserByAdminRequest request2 = CreateUserByAdminRequest.builder()
                .email("user2@example.com")
                .role(Role.USER)
                .build();

        ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            user.setUpdatedAt(LocalDateTime.now());
            return user;
        });

        authService.createUserByAdmin(request1, ADMIN_EMAIL);
        authService.createUserByAdmin(request2, ADMIN_EMAIL);

        verify(passwordEncoder, times(2)).encode(passwordCaptor.capture());
        java.util.List<String> passwords = passwordCaptor.getAllValues();
        
        // Passwords should be different (extremely high probability with random generation)
        assertNotEquals(passwords.get(0), passwords.get(1));
    }

    @Test
    @DisplayName("createUserByAdmin - Password is not in plaintext in User entity")
    void createUserByAdmin_PasswordIsEncoded() {
        CreateUserByAdminRequest request = CreateUserByAdminRequest.builder()
                .email(NEW_USER_EMAIL)
                .role(Role.USER)
                .build();

        UUID newUserId = UUID.randomUUID();

        when(userRepository.existsByEmail(NEW_USER_EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(newUserId);
            user.setUpdatedAt(LocalDateTime.now());
            return user;
        });

        authService.createUserByAdmin(request, ADMIN_EMAIL);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(2)).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertEquals("$2a$12$encoded", savedUser.getPasswordHash());
    }

    @Test
    @DisplayName("createUserByAdmin - Admin email is logged for audit trail")
    void createUserByAdmin_AdminEmailIsUsed() {
        String specificAdminEmail = "specific.admin@example.com";
        CreateUserByAdminRequest request = CreateUserByAdminRequest.builder()
                .email(NEW_USER_EMAIL)
                .role(Role.USER)
                .build();

        UUID newUserId = UUID.randomUUID();

        when(userRepository.existsByEmail(NEW_USER_EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(newUserId);
            user.setUpdatedAt(LocalDateTime.now());
            return user;
        });

        authService.createUserByAdmin(request, specificAdminEmail);

        verify(emailService).sendAdminCreatedUserEmail(
                eq(NEW_USER_EMAIL),
                anyString(),
                eq(specificAdminEmail),
                isNull()
        );
    }
}
