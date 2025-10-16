package com.amalitech.user.service.service;


import com.amalitech.user.service.dto.UserRequestDTO;
import com.amalitech.user.service.dto.UserResponseDTO;

public interface UserService {
    UserResponseDTO createUser(UserRequestDTO user);
}