package com.amalitech.task.service.service;

import com.amalitech.task.service.exception.ResourceNotFoundException;
import com.amalitech.task.service.model.view.SkillView;

public interface SkillService {
    /**
     * Retrieves a skill view by its name, using a cache.
     * @param skillName the name of the skill
     * @return the SkillView
     * @throws ResourceNotFoundException if not found
     */
    SkillView getSkillByName(String skillName);
}