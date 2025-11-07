package com.amalitech.analytics.service.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

import java.util.UUID;

@Entity
@Data
public class SkillGlobalPercentile {
    @Id
    private UUID skillId;


    private Double p50Proficiency;
    private Double p75Proficiency;
    private Double p90Proficiency;

    private Long totalUsersRanked;
}