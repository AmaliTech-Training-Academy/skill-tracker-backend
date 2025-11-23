package com.amalitech.analytics.service.events;

public class RabbitMQConstants {
    public static final String USER_EXCHANGE = "user.exchange";
    public static final String ONBOARDING_COMPLETED_ROUTING_KEY = "user.onboarding.completed";
    public static final String ONBOARDING_COMPLETED_QUEUE = "onboarding.completed.progress_analytics_service.q";
    public static final String SKILL_QUEUE = "skill.events.analytics_service.q";
    public static final String SKILL_ROUTING_KEY = "skill.#";
    public static final String TASK_COMPLETION_EXCHANGE = "task.completion.exchange";
    public static final String TASK_COMPLETION_QUEUE = "task.completed.progress_analytics_service.q";
    public static final String TASK_COMPLETION_ROUTING_KEY = "task.completed";
}
