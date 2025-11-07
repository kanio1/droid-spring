package com.droid.bss.infrastructure.cache.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Cache Metrics Configuration
 *
 * Configures AOP proxy for automatic cache metrics collection
 */
@Configuration
@EnableAspectJAutoProxy
public class CacheMetricsConfig {

    // This configuration enables automatic tracking of cache operations
    // The CacheMetricsInterceptor will be applied to all @Cacheable methods
}
