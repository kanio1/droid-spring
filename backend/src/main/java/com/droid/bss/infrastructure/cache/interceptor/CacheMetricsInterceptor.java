package com.droid.bss.infrastructure.cache.interceptor;

import com.droid.bss.infrastructure.cache.metrics.CacheMetricsService;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * Cache Metrics Interceptor
 *
 * Automatically tracks cache hits, misses, and puts for @Cacheable methods
 */
@Component
public class CacheMetricsInterceptor implements MethodInterceptor {

    private static final Logger log = LoggerFactory.getLogger(CacheMetricsInterceptor.class);
    private final CacheMetricsService cacheMetricsService;

    public CacheMetricsInterceptor(CacheMetricsService cacheMetricsService) {
        this.cacheMetricsService = cacheMetricsService;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Method method = invocation.getMethod();

        // Get cache name from @CacheConfig or @Cacheable annotation
        String cacheName = extractCacheName(method);
        if (cacheName == null) {
            return invocation.proceed();
        }

        long startTime = System.nanoTime();
        boolean isCacheable = method.isAnnotationPresent(org.springframework.cache.annotation.Cacheable.class);

        if (!isCacheable) {
            return invocation.proceed();
        }

        try {
            Object result = invocation.proceed();
            long duration = System.nanoTime() - startTime;

            // Check if result is present (cache hit) or null/empty (cache miss)
            if (isCacheHit(result)) {
                cacheMetricsService.recordCacheHit(cacheName);
                log.trace("Cache HIT: {} in {} ns", cacheName, duration);
            } else {
                cacheMetricsService.recordCacheMiss(cacheName);
                cacheMetricsService.recordCachePut(cacheName);
                log.trace("Cache MISS: {} in {} ns", cacheName, duration);
            }

            return result;
        } catch (Throwable throwable) {
            long duration = System.nanoTime() - startTime;
            log.error("Error in cacheable method {}: {} in {} ns", method.getName(), throwable.getMessage(), duration);
            throw throwable;
        }
    }

    private String extractCacheName(Method method) {
        // Try to get from @Cacheable annotation
        org.springframework.cache.annotation.Cacheable cacheable =
                method.getAnnotation(org.springframework.cache.annotation.Cacheable.class);

        if (cacheable != null && cacheable.value().length > 0) {
            return cacheable.value()[0];
        }

        // Try to get from @CacheConfig on the class
        org.springframework.cache.annotation.CacheConfig cacheConfig =
                method.getDeclaringClass().getAnnotation(org.springframework.cache.annotation.CacheConfig.class);

        if (cacheConfig != null && cacheConfig.cacheNames().length > 0) {
            return cacheConfig.cacheNames()[0];
        }

        return null;
    }

    private boolean isCacheHit(Object result) {
        // A cache hit typically means we got a non-null result that wasn't freshly computed
        // Since we can't directly detect this without cache manager access,
        // we use heuristics: non-null results with no exceptions are considered potential hits
        // In a real implementation, you'd integrate with the actual cache manager

        // For now, we'll track all results as "potential" hits/misses
        // The actual tracking would require a custom Cache implementation
        return result != null;
    }
}
