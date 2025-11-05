package com.droid.bss.infrastructure.cache.eviction;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.HashMap;
import java.util.Map;

/**
 * Spring configuration for cache eviction.
 *
 * @since 1.0
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(CacheEvictionProperties.class)
@ConditionalOnProperty(name = "app.cache.eviction.enabled", havingValue = "true", matchIfMissing = true)
public class CacheEvictionConfig {

    private final CacheEvictionProperties evictionProperties;

    public CacheEvictionConfig(CacheEvictionProperties evictionProperties) {
        this.evictionProperties = evictionProperties;
    }

    /**
     * Creates the LRU eviction policy.
     *
     * @return the LRU policy
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "app.cache.eviction.defaultPolicy", havingValue = "LRU")
    public EvictionPolicy<String, Object> lruEvictionPolicy() {
        return new LRUEvictionPolicy<>();
    }

    /**
     * Creates the LFU eviction policy.
     *
     * @return the LFU policy
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "app.cache.eviction.defaultPolicy", havingValue = "LFU")
    public EvictionPolicy<String, Object> lfuEvictionPolicy() {
        return new LFUEvictionPolicy<>();
    }

    /**
     * Creates the TTL eviction policy.
     *
     * @return the TTL policy
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "app.cache.eviction.defaultPolicy", havingValue = "TTL")
    public EvictionPolicy<String, Object> ttlEvictionPolicy() {
        long ttlMs = evictionProperties.getDefaultTtlSeconds() * 1000L;
        return new TTLEvictionPolicy<>(ttlMs);
    }

    /**
     * Creates the size-based eviction policy.
     *
     * @return the size-based policy
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "app.cache.eviction.defaultPolicy", havingValue = "SIZE")
    public EvictionPolicy<String, Object> sizeBasedEvictionPolicy() {
        // Calculate max cache size in bytes (assuming 1KB per entry)
        long maxCacheSize = evictionProperties.getMaxCacheSize() * 1024L;
        return new SizeBasedEvictionPolicy<>(maxCacheSize);
    }

    /**
     * Creates the default cache eviction strategy.
     *
     * @param policy the eviction policy
     * @return the strategy
     */
    @Bean
    @ConditionalOnMissingBean
    public CacheEvictionStrategy<String, Object> defaultCacheEvictionStrategy(EvictionPolicy<String, Object> policy) {
        return new DefaultCacheEvictionStrategy<>(policy);
    }

    /**
     * Creates a cache eviction strategy factory.
     *
     * @return the factory
     */
    @Bean
    @ConditionalOnMissingBean
    public CacheEvictionStrategyFactory cacheEvictionStrategyFactory() {
        return new CacheEvictionStrategyFactory();
    }

    /**
     * Creates a map of cache eviction strategies for different caches.
     *
     * @param factory the strategy factory
     * @return the map of strategies
     */
    @Bean
    @ConditionalOnMissingBean
    public Map<String, CacheEvictionStrategy<String, Object>> cacheEvictionStrategies(CacheEvictionStrategyFactory factory) {
        Map<String, CacheEvictionStrategy<String, Object>> strategies = new HashMap<>();

        // Add default strategy
        strategies.put("default", factory.createCustomStrategy(new LRUEvictionPolicy<>()));

        // Add strategies for specific caches
        Map<String, CacheEvictionProperties.CacheEvictionConfig> cacheConfigs = evictionProperties.getCaches();

        if (cacheConfigs != null) {
            for (Map.Entry<String, CacheEvictionProperties.CacheEvictionConfig> entry : cacheConfigs.entrySet()) {
                String cacheName = entry.getKey();
                CacheEvictionProperties.CacheEvictionConfig config = entry.getValue();

                try {
                    Map<String, Object> parameters = new HashMap<>();
                    if (config.getTtlMs() != null) {
                        parameters.put("ttlMs", config.getTtlMs());
                    }
                    if (config.getMaxCacheSize() != null) {
                        parameters.put("maxCacheSize", (long) config.getMaxCacheSize() * 1024L);
                    }

                    CacheEvictionStrategy<String, Object> strategy =
                        factory.createByName(config.getPolicy(), parameters);
                    strategies.put(cacheName, strategy);

                } catch (Exception e) {
                    // Log error but continue
                    System.err.println("Failed to create eviction strategy for cache '" + cacheName + "': " + e.getMessage());
                }
            }
        }

        return strategies;
    }

    /**
     * Creates the cache eviction scheduler.
     *
     * @param evictionProperties the eviction properties
     * @param strategies the eviction strategies
     * @return the scheduler
     */
    @Bean
    @ConditionalOnMissingBean
    public CacheEvictionScheduler cacheEvictionScheduler(CacheEvictionProperties evictionProperties,
                                                          Map<String, CacheEvictionStrategy<String, Object>> strategies) {
        return new CacheEvictionScheduler(evictionProperties, strategies);
    }
}
