package com.amalitech.feedback.service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures RabbitMQ for the feedback-service.
 * This service acts as a CONSUMER of submission events
 * AND a PRODUCER of evaluation events.
 */
@Configuration
public class RabbitMQConfig {

    public static final String SUBMISSION_EXCHANGE = "submission.exchange";
    public static final String SUBMISSION_CREATED_QUEUE = "submission.created.q";
    public static final String SUBMISSION_CREATED_ROUTING_KEY = "submission.created";

    public static final String SUBMISSION_EXECUTED_ROUTING_KEY = "submission.executed";
    public static final String SUBMISSION_EVALUATED_ROUTING_KEY = "submission.evaluated";

    /**
     * Creates the message converter bean to handle JSON (DTOs).
     * We pass in a pre-configured ObjectMapper (which Spring Boot provides)
     * to ensure it understands our polymorphic DTOs.
     */
    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    /**
     * Configures the listener factory to use our JSON message converter.
     * This is what allows our @RabbitListener to receive a TaskSubmissionDTO.
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        return factory;
    }

    /**
     * Declares the exchange we listen to.
     * This is idempotent; it won't be re-created if it already exists.
     */
    @Bean
    public TopicExchange submissionExchange() {
        return new TopicExchange(SUBMISSION_EXCHANGE);
    }

    /**
     * Declares the queue we listen to.
     */
    @Bean
    public Queue submissionCreatedQueue() {
        return new Queue(SUBMISSION_CREATED_QUEUE, true, false, false);
    }

    /**
     * Binds our queue to the exchange with the correct routing key.
     */
    @Bean
    public Binding submissionCreatedBinding(TopicExchange submissionExchange, Queue submissionCreatedQueue) {
        return BindingBuilder.bind(submissionCreatedQueue)
                .to(submissionExchange)
                .with(SUBMISSION_CREATED_ROUTING_KEY);
    }

    /**
     * Creates the RabbitTemplate bean needed for PUBLISHING results.
     * It's configured to use the same JSON converter as the listener.
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}