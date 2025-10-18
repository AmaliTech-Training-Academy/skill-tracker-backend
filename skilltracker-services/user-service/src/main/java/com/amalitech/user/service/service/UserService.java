package com.amalitech.user.service.service;

import com.amalitech.user.service.model.User;
import com.amalitech.user.service.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.amalitech.user.service.dto.UserRequestDTO;
import com.amalitech.user.service.dto.UserResponseDTO;
import java.util.Optional;

public interface UserService {
    UserResponseDTO createUser(UserRequestDTO userdto);
    Optional<UserResponseDTO> verifyCode(String code, String email);

    public void updatePassword(User user, String newPassword) {
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}