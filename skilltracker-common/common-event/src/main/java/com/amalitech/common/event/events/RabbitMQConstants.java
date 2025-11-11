package com.amalitech.common.event.events;

public final class RabbitMQConstants {

    private RabbitMQConstants() {
        // prevent instantiation
    }

    // Exchange
    public static final String SKILL_EXCHANGE = "skill-exchange";

    // Queues
    public static final String SKILL_ANALYTICS_QUEUE = "skill-analytics-queue";
    public static final String SKILL_NOTIFICATION_QUEUE = "skill-notification-queue";

    // Routing Keys
    public static final String SKILL_CREATED_ROUTING_KEY = "skill.created";
    public static final String SKILL_UPDATED_ROUTING_KEY = "skill.updated";
    public static final String SKILL_DELETED_ROUTING_KEY = "skill.deleted";
}
