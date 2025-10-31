package com.amalitech.user.service.service.impl;

import com.amalitech.user.service.dto.response.SkillResponseDto;
import com.amalitech.user.service.model.Skill;
import com.amalitech.user.service.repository.SkillRepository;
import com.amalitech.user.service.service.SkillService;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SkillServiceImpl implements SkillService {

    private final SkillRepository skillRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SkillResponseDto> getAllSkills(Pageable pageable) {
        Page<Skill> skillPage = skillRepository.findAll(pageable);

        return skillPage.getContent().stream()
                .map(skill -> new SkillResponseDto(
                        skill.getId(),
                        skill.getName(),
                        skill.getCategory(),
                        skill.getIconUrl()
                ))
                .collect(Collectors.toList());
    }
}