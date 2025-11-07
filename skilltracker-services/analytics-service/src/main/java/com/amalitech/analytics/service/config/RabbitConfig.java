package com.amalitech.analytics.service.config;

import com.amalitech.common.event.events.RabbitMQConstants;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Bean
    public TopicExchange skillExchange() {
        return new TopicExchange(RabbitMQConstants.SKILL_EXCHANGE);
    }

    @Bean
    public Queue skillQueue() {
        return new Queue(RabbitMQConstants.SKILL_QUEUE);
    }

    @Bean
    public Binding skillBinding(Queue skillQueue, TopicExchange skillExchange) {
        return BindingBuilder.bind(skillQueue)
                .to(skillExchange)
                .with(RabbitMQConstants.SKILL_ROUTING_KEY);
    }

    // JSON message converter
    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // Make RabbitTemplate use JSON converter
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }

    // Listener factory to convert messages to POJOs
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter converter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(converter);
        return factory;
    }
}
