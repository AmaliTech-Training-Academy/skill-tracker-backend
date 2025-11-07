package com.amalitech.analytics.service.model;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "local_skill_config")
@Data
@NoArgsConstructor
public class LocalSkillConfig {
    @Id
    private UUID skillId;

    private String skillName;
    private int beginnerXp = 0;
    private int intermediateXp = 1500;
    private int advancedXp = 4500;
    private int masterXp = 9000;
}
