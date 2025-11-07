package com.droid.bss.infrastructure.cache.advanced;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Advanced Multi-Layer Cache Configuration
 * Implements L1 (Caffeine) and L2 (Redis) caching
 */
@Slf4j
@Configuration
public class AdvancedCacheConfiguration {

    /**
     * L1 Cache (Caffeine) - In-memory, fast access
     */
    @Bean
    @Qualifier("l1Cache")
    public Cache<String, Object> l1Cache() {
        log.info("Initializing L1 Cache (Caffeine)");
        return Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(Duration.ofMinutes(10))
            .expireAfterAccess(Duration.ofMinutes(5))
            .recordStats()
            .build();
    }

    /**
     * L2 Cache (Redis) - Distributed, persistent
     */
    @Bean
    @Qualifier("l2Cache")
    public RedisTemplate<String, Object> l2Cache(
            RedisTemplate<String, Object> redisTemplate) {
        log.info("Initializing L2 Cache (Redis)");
        // Redis template is already configured
        return redisTemplate;
    }

    /**
     * Multi-layer cache manager
     */
    @Bean
    public MultiLayerCacheManager multiLayerCacheManager(
            @Qualifier("l1Cache") Cache<String, Object> l1Cache,
            @Qualifier("l2Cache") RedisTemplate<String, Object> l2Cache) {
        return new MultiLayerCacheManager(l1Cache, l2Cache);
    }

    /**
     * Cache key generator
     */
    @Bean
    public CacheKeyGenerator cacheKeyGenerator() {
        return new CacheKeyGenerator();
    }

    /**
     * Cache invalidation service
     */
    @Bean
    public CacheInvalidationService cacheInvalidationService(
            MultiLayerCacheManager cacheManager,
            CacheKeyGenerator keyGenerator) {
        return new CacheInvalidationService(cacheManager, keyGenerator);
    }

    /**
     * Cache warming service
     */
    @Bean
    public CacheWarmingService cacheWarmingService(
            MultiLayerCacheManager cacheManager) {
        return new CacheWarmingService(cacheManager);
    }

    /**
     * Cache statistics service
     */
    @Bean
    public CacheStatisticsService cacheStatisticsService(
            MultiLayerCacheManager cacheManager) {
        return new CacheStatisticsService(cacheManager);
    }

    /**
     * Cache decorator for method-level caching
     */
    @Bean
    public CacheDecorator cacheDecorator(
            MultiLayerCacheManager cacheManager,
            CacheKeyGenerator keyGenerator) {
        return new CacheDecorator(cacheManager, keyGenerator);
    }

    /**
     * Hot key detector
     */
    @Bean
    public HotKeyDetector hotKeyDetector() {
        return new HotKeyDetector();
    }

    /**
     * Probabilistic cache expiration service
     */
    @Bean
    public ProbabilisticExpirationService probabilisticExpirationService(
            MultiLayerCacheManager cacheManager) {
        return new ProbabilisticExpirationService(cacheManager);
    }
}
