package com.amalitech.notification.service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures RabbitMQ exchanges, queues, and bindings for the notification service.
 * This setup ensures that the service can consume submission-related events.
 */
@Configuration
@EnableRabbit
public class RabbitMQConfig {

    // --- Exchange for Submissions ---
    public static final String SUBMISSION_EXCHANGE = "submission.exchange";
    public static final String EXECUTED_QUEUE = "submission.executed.notification.q";
    public static final String EVALUATED_QUEUE = "submission.evaluated.notification.q";
    public static final String EXECUTED_ROUTING_KEY = "submission.executed";
    public static final String EVALUATED_ROUTING_KEY = "submission.evaluated";

    // --- Exchange for Task Generation ---
    public static final String TASK_GENERATION_EXCHANGE = "task.generation.exchange";
    public static final String TASK_GENERATION_QUEUE = "task.generation.notification.q";
    public static final String TASK_GENERATION_FAILED_QUEUE = "task.generation.failed.notification.q";
    public static final String TASK_GENERATION_ROUTING_KEY = "task.generation.succeeded";
    public static final String TASK_GENERATION_FAILED_ROUTING_KEY = "task.generation.failed";

    /**
     * Creates the topic exchange for submission events.
     * @return The TopicExchange bean.
     */
    @Bean
    public TopicExchange submissionExchange() {
        return new TopicExchange(SUBMISSION_EXCHANGE);
    }

    // --- Bean for Task Generation Exchange ---
    @Bean
    public TopicExchange taskGenerationExchange() {
        return new TopicExchange(TASK_GENERATION_EXCHANGE);
    }

    // --- Submission Queue Beans ---
    @Bean
    public Queue executedQueue() {
        return new Queue(EXECUTED_QUEUE, true);
    }

    @Bean
    public Queue evaluatedQueue() {
        return new Queue(EVALUATED_QUEUE, true);
    }

    // --- Task Generation Queue Beans ---
    /**
     * Creates the queue for task generation completion events.
     * @return The Queue bean for task generation notifications.
     */
    @Bean
    public Queue taskGenerationQueue() {
        return new Queue(TASK_GENERATION_QUEUE, true);
    }

    /**
     * Creates the queue for task generation failure events.
     * @return The Queue bean for task generation failure notifications.
     */
    @Bean
    public Queue taskGenerationFailedQueue() {
        return new Queue(TASK_GENERATION_FAILED_QUEUE, true);
    }

    // --- Bindings for Submission Exchange ---
    @Bean
    public Binding executedBinding(Queue executedQueue, TopicExchange submissionExchange) {
        return BindingBuilder.bind(executedQueue)
                .to(submissionExchange)
                .with(EXECUTED_ROUTING_KEY);
    }

    @Bean
    public Binding evaluatedBinding(Queue evaluatedQueue, TopicExchange submissionExchange) {
        return BindingBuilder.bind(evaluatedQueue)
                .to(submissionExchange)
                .with(EVALUATED_ROUTING_KEY);
    }

    @Bean
    public Binding taskGenerationBinding(Queue taskGenerationQueue, TopicExchange submissionExchange) {
        return BindingBuilder.bind(taskGenerationQueue)
                .to(submissionExchange)
                .with(TASK_GENERATION_ROUTING_KEY);
    }

    /**
     * Binds the task generation failed queue to the submission exchange with the failure routing key.
     * @param taskGenerationFailedQueue The queue for task generation failure events.
     * @param submissionExchange The submission topic exchange.
     * @return The Binding bean.
     */
    @Bean
    public Binding taskGenerationFailedBinding(Queue taskGenerationFailedQueue, TopicExchange submissionExchange) {
        return BindingBuilder.bind(taskGenerationFailedQueue)
                .to(submissionExchange)
                .with(TASK_GENERATION_FAILED_ROUTING_KEY);
    }

    /**
     * Provides a message converter to serialize and deserialize messages to and from JSON.
     * @return The Jackson2JsonMessageConverter bean.
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}