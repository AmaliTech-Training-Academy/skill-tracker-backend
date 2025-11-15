package com.amalitech.analytics.service.service;

import com.amalitech.analytics.service.dto.DashboardDTO;
import com.amalitech.analytics.service.events.AnalyticsUpdateEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class DashboardMaterializer {

    private final AnalyticsReadService readService;
    private final JdbcTemplate jdbc;

    protected static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Async
    @EventListener
    public void materialize(AnalyticsUpdateEvent event) {
        UUID userId = event.getUserId();
        try {
            DashboardDTO dashboard = readService.buildFromScratch(userId);
            String json = MAPPER.writeValueAsString(dashboard);

            jdbc.update("""
                INSERT INTO user_dashboard_materialized (user_id, data, updated_at)
                VALUES (?, ?::jsonb, NOW())
                ON CONFLICT (user_id) DO UPDATE SET
                    data = EXCLUDED.data,
                    updated_at = NOW()
                """, userId, json);

            log.debug("Materialized dashboard for user {}", userId);
        } catch (Exception e) {
            log.error("Failed to materialize dashboard for user {}", userId, e);
        }
    }
}