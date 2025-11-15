package com.amalitech.analytics.service.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_dashboard_materialized")
@Data
public class UserDashboardMaterialized {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "data", nullable = false, columnDefinition = "jsonb")
    private String data;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();


}

