package com.amalitech.user.service.service.impl;

import com.amalitech.user.service.dto.UserRequestDTO;
import com.amalitech.user.service.dto.UserResponseDTO;
import com.amalitech.user.service.model.User;
import com.amalitech.user.service.repository.UserRepository;
import com.amalitech.user.service.util.EmailUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UserServiceImplTest {

    private static final UUID UUID = java.util.UUID.randomUUID();
    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailUtil emailUtil;

    @InjectMocks
    private UserServiceImpl userService;

    private UserRequestDTO requestDTO;
    private User user;
    private User savedUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        UserRequestDTO requestDTO = new UserRequestDTO(
                "John Doe",
                "john@example.com",
                "password123"
        );

        user = new User();
        user.setEmail("john@example.com");
        user.setPasswordHash("password123");

        savedUser = new User();
        savedUser.setId(UUID);
        savedUser.setEmail("john@example.com");
        savedUser.setPasswordHash("password123");
    }

    @Test
    void createUser_ShouldSaveAndReturnUser_WhenEmailNotExists() {
        when(userRepository.existsByEmail(requestDTO.email())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserResponseDTO response = userService.createUser(requestDTO);

        assertNotNull(response);
        assertEquals(savedUser.getEmail(), response.email());
        verify(userRepository).save(any(User.class));
        verify(emailUtil).sendEmail(
                eq(savedUser.getEmail()),
                anyString(),
                contains("verification code"),
                System.getenv("notification.skillboost@gmail.com")
        );
    }

    @Test
    void createUser_ShouldThrowException_WhenEmailExists() {
        when(userRepository.existsByEmail(requestDTO.email())).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> userService.createUser(requestDTO));
        verify(userRepository, never()).save(any());
    }

    @Test
    void verifyCode_ShouldReturnUser_WhenCodeIsValid() {
        // Arrange
        userService.generateCode();
        String validCode = userService.generateCode().toString();
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(savedUser));

        // Act
        Optional<UserResponseDTO> response = userService.verifyCode(validCode, "john@example.com");

        // Assert
        assertTrue(response.isPresent());
        assertEquals(savedUser.getEmail(), response.get().email());
    }

    @Test
    void verifyCode_ShouldReturnEmpty_WhenCodeIsInvalid() {
        userService.generateCode(); // generate one code
        String invalidCode = "000000"; // wrong code

        Optional<UserResponseDTO> response = userService.verifyCode(invalidCode, "john@example.com");

        assertTrue(response.isEmpty());
    }

    @Test
    void notifyUser_ShouldSendEmail() {
        userService.notifyUser("john@example.com", "Hello", "Welcome!", System.getenv("notification.skillboost@gmail.com"));
        verify(emailUtil).sendEmail("john@example.com", "Hello", "Welcome!",System.getenv("notification.skillboost@gmail.com"));
    }
}