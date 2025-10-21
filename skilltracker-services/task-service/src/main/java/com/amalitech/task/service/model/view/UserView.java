package com.amalitech.task.service.model.view;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "user_view")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserView {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    private String role; // e.g., USER, ADMIN
}

