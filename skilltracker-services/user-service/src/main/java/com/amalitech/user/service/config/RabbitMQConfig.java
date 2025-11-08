package com.amalitech.user.service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
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
 * RabbitMQ configuration for the User Service.
 * Publishes events related to user actions and onboarding.
 * Listens for replies from downstream services to complete Sagas.
 */
@Configuration
@Slf4j
public class RabbitMQConfig {

    // --- Exchange for events *published* by User Service ---
    public static final String USER_EXCHANGE = "user.exchange";
    public static final String ONBOARDING_COMPLETED_ROUTING_KEY = "user.onboarding.completed";
    public static final String TASK_GENERATION_EXCHANGE = "task.generation.exchange";

    public static final String TASK_SUCCESS_QUEUE = "task.generation.success.user_service.q";
    public static final String TASK_SUCCESS_ROUTING_KEY = "task.generation.succeeded";

    public static final String TASK_FAILED_QUEUE = "task.generation.failed.user_service.q";
    public static final String TASK_FAILED_ROUTING_KEY = "task.generation.failed";


    @Bean
    public TopicExchange userExchange() {
        return new TopicExchange(USER_EXCHANGE);
    }

    @Bean
    public TopicExchange taskGenerationExchange() {
        return new TopicExchange(TASK_GENERATION_EXCHANGE);
    }

    @Bean
    public Queue taskSuccessQueue() {
        return new Queue(TASK_SUCCESS_QUEUE, true);
    }

    @Bean
    public Queue taskFailedQueue() {
        return new Queue(TASK_FAILED_QUEUE, true);
    }

    /**
     * Binds the success queue to the reply exchange.
     */
    @Bean
    public Binding taskSuccessBinding(TopicExchange taskGenerationExchange, Queue taskSuccessQueue) {
        return BindingBuilder.bind(taskSuccessQueue)
                .to(taskGenerationExchange)
                .with(TASK_SUCCESS_ROUTING_KEY);
    }

    /**
     * Binds the failed queue to the reply exchange.
     */
    @Bean
    public Binding taskFailedBinding(TopicExchange taskGenerationExchange, Queue taskFailedQueue) {
        return BindingBuilder.bind(taskFailedQueue)
                .to(taskGenerationExchange)
                .with(TASK_FAILED_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);

        template.setConfirmCallback((correlationData, ack, cause) -> {
            String correlationId = (correlationData != null) ? correlationData.getId() : "unknown";
            if (ack) {
                log.debug("RabbitMQ ACK received for message: id={}", correlationId);
            } else {
                log.error(
                        "RabbitMQ NACK received for message: id={}, cause={}",
                        correlationId,
                        cause
                );
            }
        });

        template.setReturnsCallback(returned -> {
            log.error(
                    "RabbitMQ Message Returned. exchange={}, routingKey={}, replyCode={}, replyText={}, messageBody={}",
                    returned.getExchange(),
                    returned.getRoutingKey(),
                    returned.getReplyCode(),
                    returned.getReplyText(),
                    new String(returned.getMessage().getBody())
            );
        });

        return template;
    }
}