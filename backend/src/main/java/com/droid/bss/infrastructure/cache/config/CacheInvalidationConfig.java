package com.droid.bss.infrastructure.cache.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Configuration for Event-Based Cache Invalidation
 *
 * Ensures Kafka listeners for cache invalidation are properly enabled
 */
@Configuration
@EnableKafka
public class CacheInvalidationConfig {
    // Configuration is automatically applied through @EnableKafka
    // and the EventBasedCacheInvalidator component
}
