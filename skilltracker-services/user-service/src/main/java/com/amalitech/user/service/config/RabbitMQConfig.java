package com.amalitech.user.service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for the User Service.
 * Publishes events related to user actions and onboarding.
 */
@Configuration
@Slf4j
public class RabbitMQConfig {

    public static final String USER_EXCHANGE = "user.exchange";
    public static final String ONBOARDING_COMPLETED_ROUTING_KEY = "user.onboarding.completed";

    @Bean
    public TopicExchange userExchange() {
        return new TopicExchange(USER_EXCHANGE);
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