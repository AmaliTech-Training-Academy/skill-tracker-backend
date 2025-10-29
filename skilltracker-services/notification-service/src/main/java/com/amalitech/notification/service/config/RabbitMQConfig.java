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

@Configuration
@EnableRabbit
public class RabbitMQConfig {

    public static final String SUBMISSION_EXCHANGE = "submission.exchange";
    public static final String EXECUTED_QUEUE = "submission.executed.notification.q";
    public static final String EVALUATED_QUEUE = "submission.evaluated.notification.q";
    public static final String EXECUTED_ROUTING_KEY = "submission.executed";
    public static final String EVALUATED_ROUTING_KEY = "submission.evaluated";

    @Bean
    public TopicExchange submissionExchange() {
        return new TopicExchange(SUBMISSION_EXCHANGE);
    }

    @Bean
    public Queue executedQueue() {
        return new Queue(EXECUTED_QUEUE, true);
    }

    @Bean
    public Queue evaluatedQueue() {
        return new Queue(EVALUATED_QUEUE, true);
    }

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
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
