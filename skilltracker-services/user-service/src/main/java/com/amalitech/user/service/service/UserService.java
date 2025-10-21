package com.amalitech.user.service.service;

import com.amalitech.user.service.model.User;
import com.amalitech.user.service.dto.UserRequestDTO;
import com.amalitech.user.service.dto.UserResponseDTO;
import java.util.Optional;

public interface UserService {
    UserResponseDTO createUser(UserRequestDTO userdto);
    Optional<UserResponseDTO> verifyCode(String code, String email);
    void updatePassword(User user, String newPassword);
}