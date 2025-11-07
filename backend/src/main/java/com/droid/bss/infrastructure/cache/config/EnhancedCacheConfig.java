package com.droid.bss.infrastructure.cache.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.time.Duration;

/**
 * Enhanced Cache Configuration
 * Provides custom TTL for different entity types and improved cache management
 */
@Configuration
@EnableCaching
public class EnhancedCacheConfig {

    // Custom TTL values for different entity types
    public static final Duration CUSTOMER_TTL = Duration.ofMinutes(10);
    public static final Duration CUSTOMER_LIST_TTL = Duration.ofMinutes(5);
    public static final Duration INVOICE_TTL = Duration.ofMinutes(5);
    public static final Duration INVOICE_LIST_TTL = Duration.ofMinutes(3);
    public static final Duration PAYMENT_TTL = Duration.ofMinutes(5);
    public static final Duration PAYMENT_LIST_TTL = Duration.ofMinutes(3);
    public static final Duration DASHBOARD_TTL = Duration.ofMinutes(2);
    public static final Duration SEARCH_TTL = Duration.ofMinutes(1);

    /**
     * Configure Redis cache manager with custom configurations for different cache names
     */
    @Bean
    @Primary
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        RedisCacheManager.Builder builder = RedisCacheManager.builder(redisConnectionFactory);

        // Default configuration
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration
                .defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .disableCachingNullValues();

        // Custom configurations for specific caches
        builder.cacheDefaults(defaultConfig)
                .withCacheConfiguration("customers",
                    RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(CUSTOMER_TTL)
                        .disableCachingNullValues())
                .withCacheConfiguration("customersList",
                    RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(CUSTOMER_LIST_TTL)
                        .disableCachingNullValues())
                .withCacheConfiguration("invoices",
                    RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(INVOICE_TTL)
                        .disableCachingNullValues())
                .withCacheConfiguration("invoicesList",
                    RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(INVOICE_LIST_TTL)
                        .disableCachingNullValues())
                .withCacheConfiguration("payments",
                    RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(PAYMENT_TTL)
                        .disableCachingNullValues())
                .withCacheConfiguration("paymentsList",
                    RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(PAYMENT_LIST_TTL)
                        .disableCachingNullValues())
                .withCacheConfiguration("dashboard",
                    RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(DASHBOARD_TTL)
                        .disableCachingNullValues())
                .withCacheConfiguration("search",
                    RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(SEARCH_TTL)
                        .disableCachingNullValues());

        return builder.build();
    }
}
