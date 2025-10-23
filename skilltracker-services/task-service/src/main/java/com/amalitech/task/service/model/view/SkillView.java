package com.amalitech.task.service.model.view;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "skill_view")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkillView {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    private String description;

    private String category;
}

