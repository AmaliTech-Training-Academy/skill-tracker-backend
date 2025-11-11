package com.amalitech.user.service;

import com.amalitech.common.security.dto.response.ApiResponse;
import com.amalitech.user.service.controller.AuthController;
import com.amalitech.user.service.dto.UserResponseDTO;
import com.amalitech.user.service.dto.request.CreateUserByAdminRequest;
import com.amalitech.user.service.exception.EmailAlreadyExistsException;
import com.amalitech.user.service.model.enums.Role;
import com.amalitech.user.service.model.enums.UserState;
import com.amalitech.user.service.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthController admin user creation endpoint.
 * Tests focus on controller responsibilities: request mapping, security context handling,
 * and response formatting.
 */
@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AdminUserCreationControllerTest {

    @Mock
    private AuthServiceImpl authServiceImpl;

    private AuthController authController;

    private static final String ADMIN_EMAIL = "admin@example.com";
    private static final String NEW_USER_EMAIL = "newuser@example.com";

    @BeforeEach
    void setUp() {
        authController = new AuthController(authServiceImpl);
    }

    private AuthServiceImpl mockAuthService() {
        return authServiceImpl;
    }

    // ==================== Happy Path Tests ====================

    @Test
    @DisplayName("Controller - POST /api/v1/auth/admin/create-user with valid request")
    void createUserByAdmin_ValidRequest_Returns200() {
        CreateUserByAdminRequest request = CreateUserByAdminRequest.builder()
                .email(NEW_USER_EMAIL)
                .role(Role.USER)
                .build();

        UserResponseDTO userDto = createUserResponseDTO(NEW_USER_EMAIL, Role.USER);

        when(authServiceImpl.createUserByAdmin(eq(request), eq(ADMIN_EMAIL)))
                .thenReturn(userDto);

        setupSecurityContext(ADMIN_EMAIL);

        ResponseEntity<ApiResponse<UserResponseDTO>> response = authController.createUserByAdmin(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("Controller - Extracts admin email from SecurityContext")
    void createUserByAdmin_ExtractsAdminEmailFromContext() {
        CreateUserByAdminRequest request = CreateUserByAdminRequest.builder()
                .email(NEW_USER_EMAIL)
                .role(Role.USER)
                .build();

        UserResponseDTO userDto = createUserResponseDTO(NEW_USER_EMAIL, Role.USER);

        when(authServiceImpl.createUserByAdmin(eq(request), eq(ADMIN_EMAIL)))
                .thenReturn(userDto);

        setupSecurityContext(ADMIN_EMAIL);

        authController.createUserByAdmin(request);

        ArgumentCaptor<String> adminEmailCaptor = ArgumentCaptor.forClass(String.class);
        verify(authServiceImpl).createUserByAdmin(eq(request), adminEmailCaptor.capture());
        assertEquals(ADMIN_EMAIL, adminEmailCaptor.getValue());
    }

    @Test
    @DisplayName("Controller - Returns ApiResponse with success message")
    void createUserByAdmin_ReturnsSuccessMessage() {
        CreateUserByAdminRequest request = CreateUserByAdminRequest.builder()
                .email(NEW_USER_EMAIL)
                .role(Role.USER)
                .build();

        UserResponseDTO userDto = createUserResponseDTO(NEW_USER_EMAIL, Role.USER);

        when(authServiceImpl.createUserByAdmin(eq(request), eq(ADMIN_EMAIL)))
                .thenReturn(userDto);

        setupSecurityContext(ADMIN_EMAIL);

        ResponseEntity<ApiResponse<UserResponseDTO>> response = authController.createUserByAdmin(request);

        assertTrue(response.getBody().isSuccess());
        assertEquals("User created successfully by admin", response.getBody().getMessage());
    }

    @Test
    @DisplayName("Controller - Returns user data in response")
    void createUserByAdmin_ReturnsUserData() {
        CreateUserByAdminRequest request = CreateUserByAdminRequest.builder()
                .email(NEW_USER_EMAIL)
                .role(Role.USER)
                .build();

        UserResponseDTO userDto = createUserResponseDTO(NEW_USER_EMAIL, Role.USER);

        when(authServiceImpl.createUserByAdmin(eq(request), eq(ADMIN_EMAIL)))
                .thenReturn(userDto);

        setupSecurityContext(ADMIN_EMAIL);

        ResponseEntity<ApiResponse<UserResponseDTO>> response = authController.createUserByAdmin(request);

        assertNotNull(response.getBody().getData());
        assertEquals(NEW_USER_EMAIL, response.getBody().getData().email());
    }

    // ==================== Request Validation Tests ====================

    @Test
    @DisplayName("Controller - Passes email from request to service")
    void createUserByAdmin_PassesEmailToService() {
        String testEmail = "test@example.com";
        CreateUserByAdminRequest request = CreateUserByAdminRequest.builder()
                .email(testEmail)
                .role(Role.USER)
                .build();

        UserResponseDTO userDto = createUserResponseDTO(testEmail, Role.USER);

        when(authServiceImpl.createUserByAdmin(eq(request), anyString()))
                .thenReturn(userDto);

        setupSecurityContext(ADMIN_EMAIL);

        authController.createUserByAdmin(request);

        verify(authServiceImpl).createUserByAdmin(argThat(req -> req.email().equals(testEmail)), anyString());
    }

    @Test
    @DisplayName("Controller - Passes role from request to service")
    void createUserByAdmin_PassesRoleToService() {
        CreateUserByAdminRequest request = CreateUserByAdminRequest.builder()
                .email(NEW_USER_EMAIL)
                .role(Role.ADMIN)
                .build();

        UserResponseDTO userDto = createUserResponseDTO(NEW_USER_EMAIL, Role.ADMIN);

        when(authServiceImpl.createUserByAdmin(eq(request), anyString()))
                .thenReturn(userDto);

        setupSecurityContext(ADMIN_EMAIL);

        authController.createUserByAdmin(request);

        verify(authServiceImpl).createUserByAdmin(argThat(req -> req.role() == Role.ADMIN), anyString());
    }

    // ==================== Error Handling Tests ====================

    @Test
    @DisplayName("Controller - Service exception propagates to caller")
    void createUserByAdmin_ServiceThrowsException_PropagatesUpward() {
        CreateUserByAdminRequest request = CreateUserByAdminRequest.builder()
                .email(NEW_USER_EMAIL)
                .role(Role.USER)
                .build();

        when(authServiceImpl.createUserByAdmin(eq(request), eq(ADMIN_EMAIL)))
                .thenThrow(new EmailAlreadyExistsException("Email already exists"));

        setupSecurityContext(ADMIN_EMAIL);

        assertThrows(EmailAlreadyExistsException.class, () -> authController.createUserByAdmin(request));
    }

    @Test
    @DisplayName("Controller - Handles RuntimeException from service")
    void createUserByAdmin_ServiceThrowsRuntime_PropagatesUpward() {
        CreateUserByAdminRequest request = CreateUserByAdminRequest.builder()
                .email(NEW_USER_EMAIL)
                .role(Role.USER)
                .build();

        when(authServiceImpl.createUserByAdmin(eq(request), eq(ADMIN_EMAIL)))
                .thenThrow(new RuntimeException("Unexpected error"));

        setupSecurityContext(ADMIN_EMAIL);

        assertThrows(RuntimeException.class, () -> authController.createUserByAdmin(request));
    }

    // ==================== Role-Based Tests ====================

    @Test
    @DisplayName("Controller - Creates USER role successfully")
    void createUserByAdmin_CreatesUserRole() {
        CreateUserByAdminRequest request = CreateUserByAdminRequest.builder()
                .email(NEW_USER_EMAIL)
                .role(Role.USER)
                .build();

        UserResponseDTO userDto = createUserResponseDTO(NEW_USER_EMAIL, Role.USER);

        when(authServiceImpl.createUserByAdmin(eq(request), eq(ADMIN_EMAIL)))
                .thenReturn(userDto);

        setupSecurityContext(ADMIN_EMAIL);

        ResponseEntity<ApiResponse<UserResponseDTO>> response = authController.createUserByAdmin(request);

        assertEquals(Role.USER, response.getBody().getData().role());
    }

    @Test
    @DisplayName("Controller - Creates ADMIN role successfully")
    void createUserByAdmin_CreatesAdminRole() {
        CreateUserByAdminRequest request = CreateUserByAdminRequest.builder()
                .email(NEW_USER_EMAIL)
                .role(Role.ADMIN)
                .build();

        UserResponseDTO userDto = createUserResponseDTO(NEW_USER_EMAIL, Role.ADMIN);

        when(authServiceImpl.createUserByAdmin(eq(request), eq(ADMIN_EMAIL)))
                .thenReturn(userDto);

        setupSecurityContext(ADMIN_EMAIL);

        ResponseEntity<ApiResponse<UserResponseDTO>> response = authController.createUserByAdmin(request);

        assertEquals(Role.ADMIN, response.getBody().getData().role());
    }

    // ==================== Multiple Admin Tests ====================

    @Test
    @DisplayName("Controller - Different admins can create users")
    void createUserByAdmin_DifferentAdmins() {
        String admin1 = "admin1@example.com";
        String admin2 = "admin2@example.com";

        CreateUserByAdminRequest request = CreateUserByAdminRequest.builder()
                .email(NEW_USER_EMAIL)
                .role(Role.USER)
                .build();

        UserResponseDTO userDto = createUserResponseDTO(NEW_USER_EMAIL, Role.USER);

        when(authServiceImpl.createUserByAdmin(eq(request), anyString()))
                .thenReturn(userDto);

        // First admin
        setupSecurityContext(admin1);
        authController.createUserByAdmin(request);
        
        // Second admin
        setupSecurityContext(admin2);
        authController.createUserByAdmin(request);

        ArgumentCaptor<String> adminEmailCaptor = ArgumentCaptor.forClass(String.class);
        verify(authServiceImpl, times(2)).createUserByAdmin(eq(request), adminEmailCaptor.capture());

        var adminEmails = adminEmailCaptor.getAllValues();
        assertEquals(admin1, adminEmails.get(0));
        assertEquals(admin2, adminEmails.get(1));
    }

    // ==================== Request Consistency Tests ====================

    @Test
    @DisplayName("Controller - Service receives unchanged request object")
    void createUserByAdmin_ServiceReceivesUnchangedRequest() {
        CreateUserByAdminRequest request = CreateUserByAdminRequest.builder()
                .email(NEW_USER_EMAIL)
                .role(Role.ADMIN)
                .build();

        UserResponseDTO userDto = createUserResponseDTO(NEW_USER_EMAIL, Role.ADMIN);

        when(authServiceImpl.createUserByAdmin(eq(request), anyString()))
                .thenReturn(userDto);

        setupSecurityContext(ADMIN_EMAIL);

        authController.createUserByAdmin(request);

        ArgumentCaptor<CreateUserByAdminRequest> requestCaptor = ArgumentCaptor.forClass(CreateUserByAdminRequest.class);
        verify(authServiceImpl).createUserByAdmin(requestCaptor.capture(), anyString());

        CreateUserByAdminRequest capturedRequest = requestCaptor.getValue();
        assertEquals(request.email(), capturedRequest.email());
        assertEquals(request.role(), capturedRequest.role());
    }

    @Test
    @DisplayName("Controller - Response contains correct HTTP status code")
    void createUserByAdmin_StatusCodeIsOk() {
        CreateUserByAdminRequest request = CreateUserByAdminRequest.builder()
                .email(NEW_USER_EMAIL)
                .role(Role.USER)
                .build();

        UserResponseDTO userDto = createUserResponseDTO(NEW_USER_EMAIL, Role.USER);

        when(authServiceImpl.createUserByAdmin(eq(request), anyString()))
                .thenReturn(userDto);

        setupSecurityContext(ADMIN_EMAIL);

        ResponseEntity<ApiResponse<UserResponseDTO>> response = authController.createUserByAdmin(request);

        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    @DisplayName("Controller - Multiple sequential requests handled correctly")
    void createUserByAdmin_MultipleSequentialRequests() {
        String[] emails = {"user0@example.com", "user1@example.com", "user2@example.com"};
        
        for (String email : emails) {
            CreateUserByAdminRequest request = CreateUserByAdminRequest.builder()
                    .email(email)
                    .role(Role.USER)
                    .build();

            UserResponseDTO userDto = createUserResponseDTO(email, Role.USER);

            when(authServiceImpl.createUserByAdmin(eq(request), eq(ADMIN_EMAIL)))
                    .thenReturn(userDto);

            setupSecurityContext(ADMIN_EMAIL);

            ResponseEntity<ApiResponse<UserResponseDTO>> response = authController.createUserByAdmin(request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(email, response.getBody().getData().email());
        }
    }

    // ==================== Helper Methods ====================

    private UserResponseDTO createUserResponseDTO(String email, Role role) {
        return UserResponseDTO.builder()
                .id(UUID.randomUUID())
                .email(email)
                .username(null)
                .role(role)
                .state(UserState.REGISTERED)
                .tourStatus(null)
                .is_verified(true)
                .premiumTier(null)
                .language("en")
                .timezone("UTC")
                .build();
    }

    private void setupSecurityContext(String email) {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(email);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);
    }
}
