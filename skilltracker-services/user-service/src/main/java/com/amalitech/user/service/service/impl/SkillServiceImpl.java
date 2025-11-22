package com.amalitech.user.service.service.impl;

import com.amalitech.user.service.dto.UserSkillDto;
import com.amalitech.user.service.dto.response.SkillResponse;
import com.amalitech.user.service.mapper.SkillMapper;
import com.amalitech.user.service.model.Skill;
import com.amalitech.user.service.repository.SkillRepository;
import com.amalitech.user.service.repository.UserSkillRepository;
import com.amalitech.user.service.service.SkillService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of the {@link SkillService} interface.
 * This service provides methods for retrieving and managing skill data.
 */
@Service
@RequiredArgsConstructor
public class SkillServiceImpl implements SkillService {

    private final SkillRepository skillRepository;
    private final SkillMapper skillMapper;
    private final UserSkillRepository userSkillRepository;

    /**
     * {@inheritDoc}
     * <p>This implementation retrieves all skills from the database with pagination,
     * and maps them to {@link SkillResponse} DTOs for client consumption.</p>
     *
     * @param pageable Pagination information (page number, size, sort).
     * @return A list of {@link SkillResponse} objects representing the available skills.
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "skills-public-cache")
    public List<SkillResponse> getAllSkills(Pageable pageable) {
        Page<Skill> skillPage = skillRepository.findAll(pageable);
        return skillPage.map(skillMapper::toSkillResponse).getContent();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSkillDto> getUserSkills(UUID userId) {
        return userSkillRepository.findByUserId(userId).stream()
                .map(skillMapper::toUserSkillDto)
                .collect(Collectors.toList());
    }
}