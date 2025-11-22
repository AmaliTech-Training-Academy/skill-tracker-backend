package com.amalitech.user.service.mapper;

import com.amalitech.user.service.dto.UserSkillDto;
import com.amalitech.user.service.dto.request.CreateSkillRequest;
import com.amalitech.user.service.dto.request.UpdateSkillRequest;
import com.amalitech.user.service.dto.response.SkillResponse;
import com.amalitech.user.service.model.Skill;
import com.amalitech.user.service.model.UserSkill;

import org.springframework.stereotype.Component;
import org.springframework.data.domain.Page;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Manual mapper for converting between Skill DTOs and entities.
 * Provides clean separation between the API layer and domain layer.
 */
@Component
public class SkillMapper {

    /**
     * Maps a CreateSkillRequest DTO to a Skill entity.
     * Sets default values for XP map and generates UUID.
     *
     * @param request The skill creation request
     * @return The mapped Skill entity ready for persistence
     */
    public Skill toEntity(CreateSkillRequest request) {
        Skill skill = new Skill();
        skill.setName(request.name());
        skill.setDescription(request.description());
        skill.setCategory(request.category());
        skill.setIconUrl(request.iconUrl());
        skill.setSupportedTaskTypes(new HashSet<>(request.supportedTaskTypes()));
        skill.setLevelXpMap(createDefaultXpMap());
        return skill;
    }

    /**
     * Maps a Skill entity to a SkillResponse DTO.
     *
     * @param skill The skill entity
     * @return The corresponding SkillResponse DTO
     */
    public SkillResponse toSkillResponse(Skill skill) {
        return new SkillResponse(
                skill.getId(),
                skill.getName(),
                skill.getDescription(),
                skill.getCategory(),
                skill.getIconUrl(),
                skill.getSupportedTaskTypes(),
                skill.getLevelXpMap()
        );
    }

    /**
     * Maps a Page of Skill entities to a Page of SkillResponse DTOs.
     *
     * @param skillPage The page of skill entities
     * @return The corresponding page of SkillResponse DTOs
     */
    public Page<SkillResponse> toSkillResponsePage(Page<Skill> skillPage) {
        return skillPage.map(this::toSkillResponse);
    }

    /**
     * Creates the default XP mapping for skill levels.
     * Can be overridden by business requirements.
     *
     * @return Default XP map with BEGINNER, INTERMEDIATE, ADVANCED levels
     */
    private Map<String, Long> createDefaultXpMap() {
        Map<String, Long> xpMap = new HashMap<>();
        xpMap.put("BEGINNER", 100L);
        xpMap.put("INTERMEDIATE", 500L);
        xpMap.put("ADVANCED", 1000L);
        return xpMap;
    }

    /**
     * Updates an existing Skill entity with data from UpdateSkillRequest.
     * Preserves the entity's ID and other immutable fields.
     *
     * @param skill The existing skill entity to update
     * @param request The update request containing new data
     * @return The updated skill entity
     */
    public Skill updateEntity(Skill skill, UpdateSkillRequest request) {
        skill.setName(request.name());
        skill.setDescription(request.description());
        skill.setCategory(request.category());
        skill.setIconUrl(request.iconUrl());
        skill.setSupportedTaskTypes(new java.util.HashSet<>(request.supportedTaskTypes()));
        // Note: XP map is not updated during skill updates to preserve progression
        return skill;
    }

    /**
     * Maps a Skill entity to a CreateSkillRequest DTO.
     * Useful for update operations where we want to modify existing data.
     *
     * @param skill The skill entity
     * @return The corresponding CreateSkillRequest DTO
     */
    public CreateSkillRequest toCreateRequest(Skill skill) {
        return new CreateSkillRequest(
            skill.getName(),
            skill.getDescription(),
            skill.getCategory(),
            skill.getIconUrl(),
            skill.getSupportedTaskTypes()
        );
    }

    /**
     * Maps a UserSkill entity to a UserSkillDto.
     *
     * @param userSkill The user skill entity
     * @return The corresponding UserSkillDto
     */
    public UserSkillDto toUserSkillDto(UserSkill userSkill) {
        return new UserSkillDto(
                userSkill.getSkill().getId(),
                userSkill.getSkill().getName(),
                userSkill.getCurrentLevel(),
                userSkill.getSelectedAt()
        );
    }
}
