package com.amalitech.task.service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures RabbitMQ exchanges, queues, and bindings for the task service.
 * <p>
 * This class defines the necessary AMQP components for handling:
 * <ul>
 * <li>Task generation requests (batch and admin).</li>
 * <li>Task submission events (created and evaluated).</li>
 * <li>Events from the User Service (user and skill events).</li>
 * </ul>
 * It also configures the {@link RabbitTemplate} to use JSON message conversion.
 */
@Configuration
public class RabbitMQConfig {
    public static final String BATCH_GENERATION_QUEUE = "task.generation.batch.q";
    public static final String ADMIN_GENERATION_QUEUE = "task.generation.admin.q";

    public static final String SUBMISSION_EXCHANGE = "submission.exchange";
    public static final String SUBMISSION_CREATED_QUEUE = "submission.created.q";
    public static final String SUBMISSION_EVALUATED_QUEUE = "submission.evaluated.q";
    public static final String SUBMISSION_CREATED_ROUTING_KEY = "submission.created";
    public static final String SUBMISSION_EVALUATED_ROUTING_KEY = "submission.evaluated";

    public static final String USER_SERVICE_EXCHANGE = "user.exchange";
    public static final String USER_EVENTS_QUEUE = "user.events.task_service.q";
    public static final String USER_EVENTS_ROUTING_KEY = "user.#";

    public static final String SKILL_EVENTS_QUEUE = "skill.events.task_service.q";
    public static final String SKILL_EVENTS_ROUTING_KEY = "skill.#";

    public static final String ONBOARDING_COMPLETED_QUEUE = "user.onboarding.task_service.q";
    public static final String ONBOARDING_COMPLETED_ROUTING_KEY = "user.onboarding.completed";


    /**
     * Creates a durable queue for batch task generation requests.
     *
     * @return The batch generation queue.
     */
    @Bean
    public Queue batchGenerationQueue() {
        return new Queue(BATCH_GENERATION_QUEUE, true, false, false);
    }

    /**
     * Creates a durable queue for admin-initiated task generation requests.
     *
     * @return The admin generation queue.
     */
    @Bean
    public Queue adminGenerationQueue() {
        return new Queue(ADMIN_GENERATION_QUEUE, true, false, false);
    }

    /**
     * Creates a topic exchange for submission-related events.
     *
     * @return The submission topic exchange.
     */
    @Bean
    public TopicExchange submissionExchange() {
        return new TopicExchange(SUBMISSION_EXCHANGE);
    }

    /**
     * Creates a durable queue for 'submission created' events.
     *
     * @return The submission created queue.
     */
    @Bean
    public Queue submissionCreatedQueue() {
        return new Queue(SUBMISSION_CREATED_QUEUE, true, false, false);
    }

    /**
     * Creates a durable queue for 'submission evaluated' events.
     *
     * @return The submission evaluated queue.
     */
    @Bean
    public Queue submissionEvaluatedQueue() {
        return new Queue(SUBMISSION_EVALUATED_QUEUE, true, false, false);
    }

    /**
     * Binds the 'submission created' queue to the submission exchange
     * using the {@code SUBMISSION_CREATED_ROUTING_KEY}.
     *
     * @param submissionExchange     The submission topic exchange.
     * @param submissionCreatedQueue The 'submission created' queue.
     * @return The binding definition.
     */
    @Bean
    public Binding submissionCreatedBinding(TopicExchange submissionExchange, Queue submissionCreatedQueue) {
        return BindingBuilder.bind(submissionCreatedQueue)
                .to(submissionExchange)
                .with(SUBMISSION_CREATED_ROUTING_KEY);
    }

    /**
     * Binds the 'submission evaluated' queue to the submission exchange
     * using the {@code SUBMISSION_EVALUATED_ROUTING_KEY}.
     *
     * @param submissionExchange       The submission topic exchange.
     * @param submissionEvaluatedQueue The 'submission evaluated' queue.
     * @return The binding definition.
     */
    @Bean
    public Binding submissionEvaluatedBinding(TopicExchange submissionExchange, Queue submissionEvaluatedQueue) {
        return BindingBuilder.bind(submissionEvaluatedQueue)
                .to(submissionExchange)
                .with(SUBMISSION_EVALUATED_ROUTING_KEY);
    }

    /**
     * Creates a topic exchange for events originating from the User Service.
     *
     * @return The user service topic exchange.
     */
    @Bean
    public TopicExchange userServiceExchange() {
        return new TopicExchange(USER_SERVICE_EXCHANGE);
    }

    /**
     * Creates a durable queue to consume user-related events from the User Service.
     *
     * @return The user events queue.
     */
    @Bean
    public Queue userEventsQueue() {
        return new Queue(USER_EVENTS_QUEUE, true, false, false);
    }

    /**
     * Creates a durable queue to consume skill-related events from the User Service.
     *
     * @return The skill events queue.
     */
    @Bean
    public Queue skillEventsQueue() {
        return new Queue(SKILL_EVENTS_QUEUE, true, false, false);
    }

    /**
     * Binds the user events queue to the user service exchange
     * using a wildcard routing key ({@code USER_EVENTS_ROUTING_KEY}) to capture all user events.
     *
     * @param userServiceExchange The user service topic exchange.
     * @param userEventsQueue     The user events queue.
     * @return The binding definition.
     */
    @Bean
    public Binding userEventsBinding(TopicExchange userServiceExchange, Queue userEventsQueue) {
        return BindingBuilder.bind(userEventsQueue)
                .to(userServiceExchange)
                .with(USER_EVENTS_ROUTING_KEY);
    }

    /**
     * Binds the skill events queue to the user service exchange
     * using a wildcard routing key ({@code SKILL_EVENTS_ROUTING_KEY}) to capture all skill events.
     *
     * @param userServiceExchange The user service topic exchange.
     * @param skillEventsQueue    The skill events queue.
     * @return The binding definition.
     */
    @Bean
    public Binding skillEventsBinding(TopicExchange userServiceExchange, Queue skillEventsQueue) {
        return BindingBuilder.bind(skillEventsQueue)
                .to(userServiceExchange)
                .with(SKILL_EVENTS_ROUTING_KEY);
    }

    /**
     * Creates a durable queue for consuming user onboarding completed events.
     *
     * @return The onboarding completed queue.
     */
    @Bean
    public Queue onboardingCompletedQueue() {
        return new Queue(ONBOARDING_COMPLETED_QUEUE, true, false, false);
    }

    /**
     * Binds the onboarding completed queue to the user service exchange
     * using the onboarding completed routing key.
     *
     * @param userServiceExchange The user service topic exchange.
     * @param onboardingCompletedQueue The onboarding completed queue.
     * @return The binding definition.
     */
    @Bean
    public Binding onboardingCompletedBinding(TopicExchange userServiceExchange, Queue onboardingCompletedQueue) {
        return BindingBuilder.bind(onboardingCompletedQueue)
                .to(userServiceExchange)
                .with(ONBOARDING_COMPLETED_ROUTING_KEY);
    }

    /**
     * Defines a bean for JSON message conversion using Jackson.
     * This allows sending and receiving POJOs as JSON messages.
     *
     * @return A {@link Jackson2JsonMessageConverter} bean.
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * Configures the {@link RabbitTemplate} to use the JSON message converter.
     *
     * @param connectionFactory The auto-configured RabbitMQ connection factory.
     * @return A customized {@link RabbitTemplate} bean.
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}