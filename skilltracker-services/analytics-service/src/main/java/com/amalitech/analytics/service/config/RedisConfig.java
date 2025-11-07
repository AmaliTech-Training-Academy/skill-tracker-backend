package com.amalitech.analytics.service.config;


import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;


/**
 * Configuration class for Redis integration and caching setup.
 * <p>
 * This configuration defines a custom {@link RedisTemplate} that uses
 * JSON serialization for values and string serialization for keys.
 * It ensures that objects such as {@code SkillDetailsDTO} can be
 * stored and retrieved from Redis in a human-readable JSON format,
 * while maintaining proper deserialization through type metadata.
 * </p>
 *
 * <p>
 * The {@link EnableCaching} annotation enables Spring's annotation-driven
 * cache management capability, allowing the use of caching annotations
 * such as {@code @Cacheable}, {@code @CacheEvict}, and {@code @CachePut}.
 * </p>
 *
 * @author
 * @since 1.0
 */
@Configuration
@EnableCaching
public class RedisConfig {

    /**
     * Configures and provides a {@link RedisTemplate} bean for Redis operations.
     * <p>
     * This template uses:
     * <ul>
     *   <li>{@link StringRedisSerializer} for keys to ensure readable string keys (e.g., {@code skillConfig::uuid}).</li>
     *   <li>{@link GenericJackson2JsonRedisSerializer} for values to serialize Java objects as JSON with embedded type information,
     *       ensuring accurate deserialization into the correct object type on retrieval.</li>
     * </ul>
     * </p>
     *
     * @param connectionFactory the {@link RedisConnectionFactory} that manages Redis connections
     * @return a configured {@link RedisTemplate} for performing Redis operations
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

        template.afterPropertiesSet();
        return template;
    }
}
