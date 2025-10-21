package com.amalitech.user.service.service.impl;

import com.amalitech.user.service.dto.UserRequestDTO;
import com.amalitech.user.service.dto.UserResponseDTO;
import com.amalitech.user.service.model.User;
import com.amalitech.user.service.repository.UserRepository;
import com.amalitech.user.service.util.EmailUtil;
import com.amalitech.user.service.util.PasswordEncoderUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


class UserServiceImplTest {

    @Mock
    private UserRepository repo;

    @Mock
    private EmailUtil emailUtil;

    @InjectMocks
    private UserServiceImpl userService;

    private UserRequestDTO userRequest;
    private User user;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        userRequest = new UserRequestDTO( "john@example.com", "JohnDoe", "password123");
        user = new User();
        user.setEmail("john@example.com");
        user.setUsername("JohnDoe");
        user.setPasswordHash("hashedpassword");
    }

    @Test
    void shouldCreateUserSuccessfully() {
        when(repo.existsByEmail(userRequest.email())).thenReturn(false);
        when(repo.save(any(User.class))).thenReturn(user);

        UserResponseDTO response = userService.createUser(userRequest);

        assertNotNull(response);
        assertEquals("john@example.com", response.email());
        verify(repo).save(any(User.class));
        verify(emailUtil).sendEmail(anyString(), anyString(), contains("verification code"), anyString());
    }

    @Test
    void shouldThrowExceptionWhenUserAlreadyExists() {
        when(repo.existsByEmail(userRequest.email())).thenReturn(true);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> userService.createUser(userRequest)
        );

        assertEquals("A user already exists with this email.", exception.getMessage());
        verify(repo, never()).save(any());
    }

    @Test
    void shouldVerifyCodeSuccessfully() {
        Integer code = userService.generateCode();
        when(repo.findByEmail("john@example.com")).thenReturn(Optional.of(user));

        Optional<UserResponseDTO> result = userService.verifyCode(code.toString(), "john@example.com");

        assertTrue(result.isPresent());
        assertEquals("john@example.com", result.get().email());
    }

    @Test
    void shouldFailVerificationWithWrongCode() {
        userService.generateCode();
        Optional<UserResponseDTO> result = userService.verifyCode("000000", "john@example.com");

        assertFalse(result.isPresent());
    }

    @Test
    void shouldUpdatePasswordSuccessfully() {
        String newPassword = "newPassword123";
        String encodedPassword = "encodedPassword";
        when(repo.save(any(User.class))).thenReturn(user);

        try (MockedStatic<PasswordEncoderUtil> mocked = mockStatic(PasswordEncoderUtil.class)) {
            mocked.when(() -> PasswordEncoderUtil.encodePassword(newPassword))
                    .thenReturn(encodedPassword);

            userService.updatePassword(user, newPassword);

            assertEquals(encodedPassword, user.getPasswordHash());
            verify(repo).save(user);
        }
    }
}