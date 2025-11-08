package com.amalitech.analytics.service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for setting up RabbitMQ messaging components.
 * <p>
 * This class defines the exchange, queue, and binding for handling
 * skill-related messages, along with a JSON-based message converter
 * and a configured {@link RabbitTemplate} for message publishing.
 * </p>
 *
 * <p>The {@link Jackson2JsonMessageConverter} ensures that all messages
 * are serialized and deserialized as JSON, enabling seamless integration
 * between producers and consumers using POJOs.</p>
 *
 * @author
 * @since 1.0
 */
@Configuration
public class RabbitConfig {

    /**
     * Declares a topic exchange for routing skill-related messages.
     *
     * @return a {@link TopicExchange} configured with the skill exchange name
     */
    @Bean
    public TopicExchange skillExchange() {
        return new TopicExchange(RabbitMQConstants.SKILL_EXCHANGE);
    }

    /**
     * Declares a queue to hold skill-related messages.
     *
     * @return a {@link Queue} configured with the skill queue name
     */
    @Bean
    public Queue skillQueue() {
        return new Queue(RabbitMQConstants.SKILL_QUEUE);
    }

    /**
     * Creates a binding between the skill queue and the skill exchange
     * using the defined routing key.
     *
     * @param skillQueue     the queue to bind
     * @param skillExchange  the exchange to bind to
     * @return a {@link Binding} linking the queue and exchange
     */
    @Bean
    public Binding skillBinding(Queue skillQueue, TopicExchange skillExchange) {
        return BindingBuilder.bind(skillQueue)
                .to(skillExchange)
                .with(RabbitMQConstants.SKILL_ROUTING_KEY);
    }

    /**
     * Provides a message converter that serializes and deserializes
     * messages using JSON via Jackson.
     *
     * @return a configured {@link Jackson2JsonMessageConverter}
     */
    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * Configures the {@link RabbitTemplate} to use JSON message conversion
     * for publishing messages to RabbitMQ.
     *
     * @param connectionFactory the RabbitMQ connection factory
     * @param converter          the JSON message converter
     * @return a {@link RabbitTemplate} configured with the JSON converter
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }

    /**
     * Creates a {@link SimpleRabbitListenerContainerFactory} that
     * automatically converts incoming messages into POJOs using
     * the JSON message converter.
     *
     * @param connectionFactory the RabbitMQ connection factory
     * @param converter          the JSON message converter
     * @return a configured {@link SimpleRabbitListenerContainerFactory}
     */
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
