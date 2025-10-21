package com.amalitech.user.service.service;

import com.amalitech.user.service.model.User;

/**
 * Service interface for managing user-related operations, such as
 * profile updates, password management, and skill associations.
 */
public interface UserService {

    /**
     * Updates a user's password.
     * This method is responsible for encoding the new password
     * before saving it to the database.
     *
     * @param user        the User entity to update
     * @param newPassword the raw, unencoded new password
     */
    void updatePassword(User user, String newPassword);
}