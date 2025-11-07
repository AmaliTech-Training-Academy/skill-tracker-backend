package com.amalitech.task.service.service.impl;

import com.amalitech.task.service.exception.ResourceNotFoundException;
import com.amalitech.task.service.model.view.SkillView;
import com.amalitech.task.service.repository.SkillViewRepository;
import com.amalitech.task.service.service.SkillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SkillServiceImpl implements SkillService {

    private final SkillViewRepository skillViewRepository;

    @Override
    @Cacheable(cacheNames = "skills-cache", key = "#skillName")
    public SkillView getSkillByName(String skillName) {
        log.info("Cache miss for SkillView: {}", skillName);
        return skillViewRepository.findByName(skillName)
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found: " + skillName));
    }
}