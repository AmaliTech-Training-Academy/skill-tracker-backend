package com.amalitech.analytics.service.util;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class RedisUtil {

    private final StringRedisTemplate redisTemplate;

    /**
     * Constructs a new RedisUtil with the given {@link StringRedisTemplate}.
     *
     * @param redisTemplate the Redis template used for operations
     */
    public RedisUtil(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Stores a key-value pair in Redis with a specified TTL (time-to-live).
     *
     * @param key the Redis key to set
     * @param value the value to store
     * @param ttlSeconds the time-to-live in seconds; after this time the key will expire
     */
    public void set(String key, String value, long ttlSeconds) {
        redisTemplate.opsForValue().set(key, value, ttlSeconds, TimeUnit.SECONDS);
    }

    /**
     * Retrieves the value associated with a given Redis key.
     *
     * @param key the Redis key to retrieve
     * @return the value stored at the key, or {@code null} if the key does not exist
     */
    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * Deletes the specified key from Redis.
     *
     * @param key the Redis key to delete
     */
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    /**
     * Checks if a given key exists in Redis.
     *
     * @param key the Redis key to check
     * @return {@code true} if the key exists, {@code false} otherwise
     */
    public boolean exists(String key) {
        return redisTemplate.hasKey(key);
    }
}

