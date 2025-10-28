package com.amalitech.user.service.service;

import com.amalitech.user.service.dto.response.UserStatusDTO;
import com.amalitech.user.service.model.enums.GuidedTourStatus;

import java.util.UUID;

public interface UserTourService {
    public UserStatusDTO updateTourStatus(UUID userId, GuidedTourStatus newStatus);
}
