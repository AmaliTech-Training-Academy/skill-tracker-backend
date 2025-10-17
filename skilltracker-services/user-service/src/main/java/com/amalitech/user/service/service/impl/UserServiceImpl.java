package com.amalitech.user.service.service.impl;

import com.amalitech.user.service.dto.UserRequestDTO;
import com.amalitech.user.service.dto.UserResponseDTO;
import com.amalitech.user.service.enums.roleEnum;
import com.amalitech.user.service.enums.stateEnum;
import com.amalitech.user.service.enums.tierEnum;
import com.amalitech.user.service.model.User;
import com.amalitech.user.service.repository.UserRepository;
import com.amalitech.user.service.service.UserService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository repo;

    @Override
    @Transactional
    public UserResponseDTO createUser(UserRequestDTO userdto) {

        // Convert DTO -> Entity
        User user = new User();
        user.setEmail(userdto.email());
        user.setPasswordHash(userdto.password()); // update this with encoder when security arrives
        user.setLanguage("en");
        user.setTimezone("UTC");

        User savedUser = repo.save(user);

        // Convert Entity → DTO
        UserResponseDTO userResponse =  new UserResponseDTO(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getRole(),
                savedUser.getState(),
                savedUser.getPremiumTier(),
                savedUser.getLanguage(),
                savedUser.getTimezone(),
                savedUser.getLastLoginAt(),
                savedUser.getUpdatedAt());

        return userResponse;
    }
}
