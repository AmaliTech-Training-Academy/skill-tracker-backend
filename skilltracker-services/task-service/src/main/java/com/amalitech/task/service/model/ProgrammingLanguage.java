package com.amalitech.task.service.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.UUID;

/**
 * Represents a programming language that can be used in coding tasks.
 * Example: Java, Python, C++, JavaScript, etc.
 */
@Entity
@Table(name = "programming_languages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgrammingLanguage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank(message = "Language name cannot be blank")
    @Column(nullable = false, unique = true)
    private String name;
}
