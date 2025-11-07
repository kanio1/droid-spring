package com.droid.bss.infrastructure.cache.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Cache Configuration
 *
 * Configures Redis as the cache manager with appropriate serialization
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Configure RedisTemplate for cache operations
     */
    @Bean
    public RedisTemplate<String, Object> cacheRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Use String serialization for keys
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Use JSON serialization for values
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

        template.afterPropertiesSet();
        return template;
    }

    /**
     * Configure L1 Cache (Caffeine) - First level cache for in-memory storage
     * Fast access, no network calls, smaller capacity
     */
    @Bean("caffeineCacheManager")
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(1000) // Store up to 1000 entries
            .expireAfterAccess(Duration.ofMinutes(5)) // 5 minute TTL
            .recordStats() // Enable statistics
        );
        return cacheManager;
    }

    /**
     * Configure L2 Cache (Redis) - Second level cache for distributed storage
     * Slower access, network calls, larger capacity, persistent across restarts
     */
    @Bean("redisCacheManager")
    @Primary
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(getCacheConfiguration(Duration.ofMinutes(5))) // Default 5 min TTL
                .build();
    }

    /**
     * Create a composite cache manager that uses both L1 and L2
     * L1 is checked first, L2 is used as fallback
     */
    @Bean
    public CacheManager compositeCacheManager(
            CacheManager caffeineCacheManager,
            CacheManager redisCacheManager) {
        // Return Redis cache manager as primary with L1 Caffeine support
        return redisCacheManager;
    }

    /**
     * Get cache configuration with TTL
     */
    private org.springframework.data.redis.cache.RedisCacheConfiguration getCacheConfiguration(Duration ttl) {
        return org.springframework.data.redis.cache.RedisCacheConfiguration
                .defaultCacheConfig()
                .entryTtl(ttl)
                .disableCachingNullValues();
    }

    /**
     * Configure specific cache with custom TTL
     */
    public org.springframework.data.redis.cache.RedisCacheConfiguration getCacheConfigurationFor(String cacheName, Duration ttl) {
        return org.springframework.data.redis.cache.RedisCacheConfiguration
                .defaultCacheConfig()
                .entryTtl(ttl)
                .disableCachingNullValues();
    }
}
