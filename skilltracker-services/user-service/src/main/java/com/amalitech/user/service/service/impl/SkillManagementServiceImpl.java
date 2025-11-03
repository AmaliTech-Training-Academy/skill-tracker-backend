package com.amalitech.user.service.service.impl;

import com.amalitech.common.event.events.SkillEvent;
import com.amalitech.user.service.dto.request.CreateSkillRequest;
import com.amalitech.user.service.dto.request.UpdateSkillRequest;
import com.amalitech.user.service.dto.response.SkillResponse;
import com.amalitech.user.service.events.EventProducer;
import com.amalitech.user.service.exception.DuplicateSkillException;
import com.amalitech.user.service.mapper.SkillMapper;
import com.amalitech.user.service.model.Skill;
import com.amalitech.user.service.repository.SkillRepository;
import com.amalitech.user.service.service.SkillManagementService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SkillManagementServiceImpl implements SkillManagementService {

    private final SkillRepository skillRepository;
    private final EventProducer eventProducer;
    private final SkillMapper skillMapper;

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "skills-admin-cache", allEntries = true),
            @CacheEvict(cacheNames = "skills-public-cache", allEntries = true)
    })
    public SkillResponse createSkill(CreateSkillRequest request) {
        Skill skill = skillMapper.toEntity(request);

        try {
            Skill saved = skillRepository.saveAndFlush(skill);

            publishSkillEvent(saved, SkillEvent.EventType.SKILL_CREATED);
            return skillMapper.toSkillResponse(saved);
        } catch (DataIntegrityViolationException e) {
            if (e.getMessage() != null && e.getMessage().contains("idx_name")) {
                String upperCaseName = request.name() != null ? request.name().toUpperCase() : "NULL";

                throw new DuplicateSkillException("A skill with the name '" + upperCaseName + "' already exists.");
            }
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "skills-admin-cache")
    public Page<SkillResponse> getAllSkills(Pageable pageable) {
        Page<Skill> skillPage = skillRepository.findAll(pageable);

        return skillMapper.toSkillResponsePage(skillPage);
    }


    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "skills-admin-cache", allEntries = true),
            @CacheEvict(cacheNames = "skills-public-cache", allEntries = true)
    })
    public SkillResponse updateSkill(UUID skillId, UpdateSkillRequest request) {
        log.info("Updating skill: {}", skillId);
        Skill existing = skillRepository.findById(skillId)
                .orElseThrow(() -> new RuntimeException("Skill not found: " + skillId));

        skillMapper.updateEntity(existing, request);

        try {
            Skill saved = skillRepository.saveAndFlush(existing);

            log.info("Skill updated: {}", skillId);
            publishSkillEvent(saved, SkillEvent.EventType.SKILL_UPDATED);
            return skillMapper.toSkillResponse(saved);
        } catch (DataIntegrityViolationException e) {
            if (e.getMessage() != null && e.getMessage().contains("idx_name")) {
                String upperCaseName = request.name() != null ? request.name().toUpperCase() : "NULL";

                throw new DuplicateSkillException(
                        "Cannot update name: a skill with the name '" + upperCaseName + "' already exists."
                );
            }
            throw e;
        }
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "skills-admin-cache", allEntries = true),
            @CacheEvict(cacheNames = "skills-public-cache", allEntries = true)
    })
    public void deleteSkill(UUID skillId) {
        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new RuntimeException("Skill not found: " + skillId));
        skillRepository.delete(skill);
        log.info("Skill deleted: {}", skillId);
        publishSkillEvent(skill, SkillEvent.EventType.SKILL_DELETED);
    }

    @Override
    @Transactional(readOnly = true)
    public SkillResponse getSkill(UUID skillId) {
        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new RuntimeException("Skill not found: " + skillId));
        return skillMapper.toSkillResponse(skill);
    }

    private void publishSkillEvent(Skill skill, SkillEvent.EventType eventType) {
        SkillEvent event = SkillEvent.builder()
                .eventType(eventType)
                .skillId(skill.getId())
                .name(skill.getName())
                .description(skill.getDescription())
                .category(skill.getCategory())
                .supportedTaskTypes(new HashSet<>(skill.getSupportedTaskTypes()))
                .levelXpMap(skill.getLevelXpMap())
                .build();

        try {
            eventProducer.publishSkillEvent(event);
            log.debug("Published {} event for skill: {}", eventType, skill.getId());
        } catch (Exception e) {
            log.error("Failed to publish {} event for skill: {}", eventType, skill.getId(), e);
        }
    }
}