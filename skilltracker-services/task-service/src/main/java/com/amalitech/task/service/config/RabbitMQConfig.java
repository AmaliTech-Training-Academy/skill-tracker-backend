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


    @Bean
    public Queue batchGenerationQueue() {
        return new Queue(BATCH_GENERATION_QUEUE, true, false, false);
    }

    @Bean
    public Queue adminGenerationQueue() {
        return new Queue(ADMIN_GENERATION_QUEUE, true, false, false);
    }

    @Bean
    public TopicExchange submissionExchange() {
        return new TopicExchange(SUBMISSION_EXCHANGE);
    }

    @Bean
    public Queue submissionCreatedQueue() {
        return new Queue(SUBMISSION_CREATED_QUEUE, true, false, false);
    }

    @Bean
    public Queue submissionEvaluatedQueue() {
        return new Queue(SUBMISSION_EVALUATED_QUEUE, true, false, false);
    }

    @Bean
    public Binding submissionCreatedBinding(TopicExchange submissionExchange, Queue submissionCreatedQueue) {
        return BindingBuilder.bind(submissionCreatedQueue)
                .to(submissionExchange)
                .with(SUBMISSION_CREATED_ROUTING_KEY);
    }

    @Bean
    public Binding submissionEvaluatedBinding(TopicExchange submissionExchange, Queue submissionEvaluatedQueue) {
        return BindingBuilder.bind(submissionEvaluatedQueue)
                .to(submissionExchange)
                .with(SUBMISSION_EVALUATED_ROUTING_KEY);
    }

    @Bean
    public TopicExchange userServiceExchange() {
        return new TopicExchange(USER_SERVICE_EXCHANGE);
    }

    @Bean
    public Queue userEventsQueue() {
        return new Queue(USER_EVENTS_QUEUE, true, false, false);
    }

    @Bean
    public Queue skillEventsQueue() {
        return new Queue(SKILL_EVENTS_QUEUE, true, false, false);
    }

    @Bean
    public Binding userEventsBinding(TopicExchange userServiceExchange, Queue userEventsQueue) {
        return BindingBuilder.bind(userEventsQueue)
                .to(userServiceExchange)
                .with(USER_EVENTS_ROUTING_KEY);
    }

    @Bean
    public Binding skillEventsBinding(TopicExchange userServiceExchange, Queue skillEventsQueue) {
        return BindingBuilder.bind(skillEventsQueue)
                .to(userServiceExchange)
                .with(SKILL_EVENTS_ROUTING_KEY);
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