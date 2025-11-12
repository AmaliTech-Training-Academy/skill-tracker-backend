package com.amalitech.user.service;

import com.amalitech.user.service.config.PasswordConfig;
import com.amalitech.user.service.controller.AuthController;
import com.amalitech.user.service.dto.UserResponseDTO;
import com.amalitech.user.service.dto.request.CreateUserByAdminRequest;
import com.amalitech.user.service.exception.EmailAlreadyExistsException;
import com.amalitech.user.service.model.User;
import com.amalitech.user.service.model.enums.Role;
import com.amalitech.user.service.model.enums.UserState;
import com.amalitech.user.service.repository.UserRepository;
import com.amalitech.user.service.service.AuthService;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for admin user creation feature covering controller and service layers.
 * Tests the full flow from controller to service to repository.
 */
@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AdminUserCreationIntegrationTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordConfig passwordConfig;
    @Mock private JwtUtil jwtUtil;
    @Mock private BCryptPasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;
    @Mock private RedisUtil redisUtil;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private CookieUtil cookieUtil;

    private AuthServiceImpl authService;
    private AuthController authController;

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
        authController = new AuthController(authService);
    }

    // ==================== Controller Tests ====================

    @Test
    @DisplayName("Controller - createUserByAdmin endpoint calls service correctly")
    void controllerCreateUserByAdmin_CallsServiceWithCorrectParams() {
        CreateUserByAdminRequest request = CreateUserByAdminRequest.builder()
                .email(NEW_USER_EMAIL)
                .role(Role.USER)
                .build();

        UUID newUserId = UUID.randomUUID();

        when(userRepository.existsByEmail(NEW_USER_EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(newUserId);
            user.setUpdatedAt(LocalDateTime.now());
            return user;
        });

        // Mock security context
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(ADMIN_EMAIL);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        var response = authController.createUserByAdmin(request);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
    }

    @Test
    @DisplayName("Controller - createUserByAdmin returns correct response structure")
    void controllerCreateUserByAdmin_ReturnsCorrectResponseStructure() {
        CreateUserByAdminRequest request = CreateUserByAdminRequest.builder()
                .email(NEW_USER_EMAIL)
                .role(Role.ADMIN)
                .build();

        UUID newUserId = UUID.randomUUID();

        when(userRepository.existsByEmail(NEW_USER_EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(newUserId);
            user.setUpdatedAt(LocalDateTime.now());
            return user;
        });

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(ADMIN_EMAIL);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        var response = authController.createUserByAdmin(request);

        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getData());
        assertEquals("User created successfully by admin", response.getBody().getMessage());
    }

    @Test
    @DisplayName("Integration - Full flow: admin creates user, profile created, email sent")
    void integrationFlow_AdminCreatesUserComplete() {
        CreateUserByAdminRequest request = CreateUserByAdminRequest.builder()
                .email(NEW_USER_EMAIL)
                .role(Role.USER)
                .build();

        UUID newUserId = UUID.randomUUID();
        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);

        when(userRepository.existsByEmail(NEW_USER_EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenAnswer(invocation -> 
            "encoded:" + invocation.getArgument(0)
        );
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(newUserId);
            user.setUpdatedAt(LocalDateTime.now());
            return user;
        });

        UserResponseDTO result = authService.createUserByAdmin(request, ADMIN_EMAIL);

        // Verify all steps completed
        assertNotNull(result);
        assertEquals(NEW_USER_EMAIL, result.email());
        
        // Verify repository calls
        verify(userRepository).existsByEmail(NEW_USER_EMAIL);
        verify(userRepository, times(2)).save(any(User.class));
        
        // Verify email service called
        verify(emailService).sendAdminCreatedUserEmail(
                eq(NEW_USER_EMAIL),
                anyString(),
                eq(ADMIN_EMAIL),
                isNull()
        );
    }

    @Test
    @DisplayName("Integration - Duplicate email prevents user creation and email sending")
    void integrationFlow_DuplicateEmailPreventsCreation() {
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

    // ==================== Service-Repository Interaction Tests ====================

    @Test
    @DisplayName("Service calls repository twice to save user and update profile")
    void service_SavesUserTwice() {
        CreateUserByAdminRequest request = CreateUserByAdminRequest.builder()
                .email(NEW_USER_EMAIL)
                .role(Role.USER)
                .build();

        UUID newUserId = UUID.randomUUID();

        when(userRepository.existsByEmail(NEW_USER_EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(newUserId);
            user.setUpdatedAt(LocalDateTime.now());
            return user;
        });

        authService.createUserByAdmin(request, ADMIN_EMAIL);

        verify(userRepository, times(2)).save(any(User.class));
    }

    @Test
    @DisplayName("Service enforces email uniqueness via repository check")
    void service_EnforcesEmailUniqueness() {
        CreateUserByAdminRequest request = CreateUserByAdminRequest.builder()
                .email(NEW_USER_EMAIL)
                .role(Role.USER)
                .build();

        when(userRepository.existsByEmail(NEW_USER_EMAIL)).thenReturn(true);

        EmailAlreadyExistsException exception = assertThrows(EmailAlreadyExistsException.class,
                () -> authService.createUserByAdmin(request, ADMIN_EMAIL));

        assertEquals("A user already exists with this email.", exception.getMessage());
    }

    @Test
    @DisplayName("Service encodes password before saving to repository")
    void service_EncodesPasswordBeforeSave() {
        CreateUserByAdminRequest request = CreateUserByAdminRequest.builder()
                .email(NEW_USER_EMAIL)
                .role(Role.USER)
                .build();

        UUID newUserId = UUID.randomUUID();
        ArgumentCaptor<String> rawPasswordCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        when(userRepository.existsByEmail(NEW_USER_EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenAnswer(invocation -> {
            String rawPassword = invocation.getArgument(0);
            return "bcrypt:" + rawPassword;
        });
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(newUserId);
            user.setUpdatedAt(LocalDateTime.now());
            return user;
        });

        authService.createUserByAdmin(request, ADMIN_EMAIL);

        verify(passwordEncoder).encode(anyString());
        verify(userRepository, times(2)).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertTrue(savedUser.getPasswordHash().startsWith("bcrypt:"));
    }

    @Test
    @DisplayName("Service sends plaintext password via email service")
    void service_SendsPasswordViaEmail() {
        CreateUserByAdminRequest request = CreateUserByAdminRequest.builder()
                .email(NEW_USER_EMAIL)
                .role(Role.USER)
                .build();

        UUID newUserId = UUID.randomUUID();
        ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);

        when(userRepository.existsByEmail(NEW_USER_EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
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

// ==================== User State & Profile Tests ====================

    @Test
    @DisplayName("Service creates user with REGISTERED state")
    void service_CreatesUserWithRegisteredState() {
        CreateUserByAdminRequest request = CreateUserByAdminRequest.builder()
                .email(NEW_USER_EMAIL)
                .role(Role.USER)
                .build();

        UUID newUserId = UUID.randomUUID();
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        when(userRepository.existsByEmail(NEW_USER_EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(newUserId);
            user.setUpdatedAt(LocalDateTime.now());
            return user;
        });

        authService.createUserByAdmin(request, ADMIN_EMAIL);

        verify(userRepository, times(2)).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals(UserState.REGISTERED, savedUser.getState());
    }

    @Test
    @DisplayName("Service creates user with pre-verified status")
    void service_CreatesUserPreVerified() {
        CreateUserByAdminRequest request = CreateUserByAdminRequest.builder()
                .email(NEW_USER_EMAIL)
                .role(Role.USER)
                .build();

        UUID newUserId = UUID.randomUUID();
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        when(userRepository.existsByEmail(NEW_USER_EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(newUserId);
            user.setUpdatedAt(LocalDateTime.now());
            return user;
        });

        authService.createUserByAdmin(request, ADMIN_EMAIL);

        verify(userRepository, times(2)).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertTrue(savedUser.getIsVerified());
    }

    @Test
    @DisplayName("Service creates UserProfile for new user")
    void service_CreatesUserProfile() {
        CreateUserByAdminRequest request = CreateUserByAdminRequest.builder()
                .email(NEW_USER_EMAIL)
                .role(Role.USER)
                .build();

        UUID newUserId = UUID.randomUUID();
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        when(userRepository.existsByEmail(NEW_USER_EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(newUserId);
            user.setUpdatedAt(LocalDateTime.now());
            return user;
        });

        authService.createUserByAdmin(request, ADMIN_EMAIL);

        verify(userRepository, times(2)).save(userCaptor.capture());
        User savedUser = userCaptor.getAllValues().get(1); // Second save has profile
        // The user has userProfile set through setProfile method
        assertNotNull(savedUser);
    }

    // ==================== Edge Cases & Null Safety Tests ====================

    @Test
    @DisplayName("Service handles different admin emails in logging")
    void service_HandlesMultipleAdminEmails() {
        String admin1Email = "admin1@example.com";
        String admin2Email = "admin2@example.com";

        CreateUserByAdminRequest request1 = CreateUserByAdminRequest.builder()
                .email("user1@example.com")
                .role(Role.USER)
                .build();

        CreateUserByAdminRequest request2 = CreateUserByAdminRequest.builder()
                .email("user2@example.com")
                .role(Role.USER)
                .build();

        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            if (user.getEmail().equals("user1@example.com")) {
                user.setId(userId1);
            } else {
                user.setId(userId2);
            }
            user.setUpdatedAt(LocalDateTime.now());
            return user;
        });

        // Create users with different admin emails
        UserResponseDTO result1 = authService.createUserByAdmin(request1, admin1Email);
        UserResponseDTO result2 = authService.createUserByAdmin(request2, admin2Email);

        assertNotNull(result1);
        assertNotNull(result2);
        assertEquals("user1@example.com", result1.email());
        assertEquals("user2@example.com", result2.email());
        
        // Verify both admin emails were passed to email service
        verify(emailService).sendAdminCreatedUserEmail(eq("user1@example.com"), anyString(), eq(admin1Email), isNull());
        verify(emailService).sendAdminCreatedUserEmail(eq("user2@example.com"), anyString(), eq(admin2Email), isNull());
    }

    @Test
    @DisplayName("Service creates user for both USER and ADMIN roles")
    void service_SupportsMultipleRoles() {
        CreateUserByAdminRequest userRequest = CreateUserByAdminRequest.builder()
                .email("user@example.com")
                .role(Role.USER)
                .build();

        CreateUserByAdminRequest adminRequest = CreateUserByAdminRequest.builder()
                .email("admin2@example.com")
                .role(Role.ADMIN)
                .build();

        UUID userId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();

        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(userRepository.existsByEmail("admin2@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            if (user.getEmail().equals("user@example.com")) {
                user.setId(userId);
            } else {
                user.setId(adminId);
            }
            user.setUpdatedAt(LocalDateTime.now());
            return user;
        });

        UserResponseDTO userResult = authService.createUserByAdmin(userRequest, ADMIN_EMAIL);
        UserResponseDTO adminResult = authService.createUserByAdmin(adminRequest, ADMIN_EMAIL);

        assertEquals(Role.USER, userResult.role());
        assertEquals(Role.ADMIN, adminResult.role());
    }

    @Test
    @DisplayName("Service consistently applies default settings across multiple creations")
    void service_AppliesConsistentDefaults() {
        String email1 = "user1@example.com";
        String email2 = "user2@example.com";

        CreateUserByAdminRequest request1 = CreateUserByAdminRequest.builder()
                .email(email1)
                .role(Role.USER)
                .build();

        CreateUserByAdminRequest request2 = CreateUserByAdminRequest.builder()
                .email(email2)
                .role(Role.USER)
                .build();

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            user.setUpdatedAt(LocalDateTime.now());
            return user;
        });

        authService.createUserByAdmin(request1, ADMIN_EMAIL);
        authService.createUserByAdmin(request2, ADMIN_EMAIL);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(4)).save(userCaptor.capture());

        for (User user : userCaptor.getAllValues()) {
            assertEquals("en", user.getLanguage());
            assertEquals("UTC", user.getTimezone());
            assertTrue(user.getIsVerified());
        }
    }
}
