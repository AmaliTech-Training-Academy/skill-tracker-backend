package com.amalitech.user.service.service.impl;

import com.amalitech.user.service.dto.UserResponseDTO;
import com.amalitech.user.service.dto.response.UserStatusDTO;
import com.amalitech.user.service.exception.UserNotFoundException;
import com.amalitech.user.service.mapper.UserMapper;
import com.amalitech.user.service.model.User;
import com.amalitech.user.service.model.enums.GuidedTourStatus;
import com.amalitech.user.service.repository.UserRepository;
import com.amalitech.user.service.service.UserTourService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service implementation for managing user guided tour status.
 * <p>
 * This class provides operations related to updating the user's guided tour progress.
 * It interacts with the {@link UserRepository} to fetch and persist user data.
 * </p>
 */
@Service
public class UserTourServiceImpl implements UserTourService {

    private final UserRepository userRepository;

    /**
     * Constructs a new {@code UserTourServiceImpl} with the provided {@link UserRepository}.
     *
     * @param userRepository the repository used for accessing and persisting user data
     */
    public UserTourServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }


    /**
     * Updates the guided tour status of a user.
     * <p>
     * This method retrieves a user by their unique identifier, updates their guided tour status
     * to the specified {@link GuidedTourStatus}, saves the updated user, and returns
     * a {@link UserResponseDTO} representation of the user.
     * </p>
     *
     * @param userId    the unique identifier of the user whose tour status should be updated
     * @param newStatus the new guided tour status to be assigned to the user
     * @return a {@link UserResponseDTO} containing the updated user information
     * @throws UserNotFoundException if no user is found with the provided {@code userId}
     */
    @Override
    @Transactional
    public UserStatusDTO updateTourStatus(UUID userId , GuidedTourStatus newStatus) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));
        user.setTourStatus(newStatus);
        userRepository.save(user);
        return new UserStatusDTO(user.getTourStatus());
    }
}