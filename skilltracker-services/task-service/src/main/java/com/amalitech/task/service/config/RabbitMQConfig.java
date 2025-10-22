package com.amalitech.task.service.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    public static final String BATCH_GENERATION_QUEUE = "task.generation.batch.q";
    public static final String ADMIN_GENERATION_QUEUE = "task.generation.admin.q";

    @Bean
    public Queue batchGenerationQueue() {
        return new Queue(BATCH_GENERATION_QUEUE, true, false, false);
    }

    @Bean
    public Queue adminGenerationQueue() {
        return new Queue(ADMIN_GENERATION_QUEUE, true, false, false);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}