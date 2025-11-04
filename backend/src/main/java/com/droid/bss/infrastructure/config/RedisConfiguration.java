package com.droid.bss.infrastructure.config;

import org.springframework.context.annotation.Configuration;

/**
 * Redis Configuration for Rate Limiting and Caching
 *
 * Note: RedisTemplate bean is provided by RedisCacheConfiguration
 * and marked as @Primary to avoid conflicts with Spring Boot auto-configuration.
 * This configuration is kept for future custom Redis beans if needed.
 */
@Configuration
public class RedisConfiguration {
    // Intentionally empty - RedisTemplate provided by RedisCacheConfiguration
}
