package com.amalitech.analytics.service.repository;

import com.amalitech.analytics.service.model.UserAggregateStats;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserAggregateStatsRepository extends JpaRepository<UserAggregateStats, UUID> {
}
