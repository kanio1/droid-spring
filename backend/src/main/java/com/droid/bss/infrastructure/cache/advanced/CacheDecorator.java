package com.droid.bss.infrastructure.cache.advanced;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Arrays;

/**
 * Cache Decorator
 * AOP-based method caching with @Cacheable annotation
 */
@Slf4j
@Aspect
@Component
public class CacheDecorator {

    private final MultiLayerCacheManager cacheManager;
    private final CacheKeyGenerator keyGenerator;
    private final CacheStatisticsService statisticsService;

    public CacheDecorator(
            MultiLayerCacheManager cacheManager,
            CacheKeyGenerator keyGenerator) {
        this.cacheManager = cacheManager;
        this.keyGenerator = keyGenerator;
        this.statisticsService = new CacheStatisticsService(cacheManager);
    }

    /**
     * Cacheable method advice
     */
    @Around("@annotation(com.droid.bss.infrastructure.cache.advanced.Cacheable)")
    public Object cacheable(ProceedingJoinPoint joinPoint) throws Throwable {
        Method method = getMethod(joinPoint);
        Cacheable cacheable = method.getAnnotation(Cacheable.class);

        String className = method.getDeclaringClass().getSimpleName();
        String methodName = method.getName();
        Object[] args = joinPoint.getArgs();
        Duration ttl = Duration.ofSeconds(cacheable.ttl());

        String cacheKey = keyGenerator.generateKey(className, methodName, args);

        // Try to get from cache
        Optional<Object> cachedValue = cacheManager.get(cacheKey, Object.class);
        if (cachedValue.isPresent()) {
            statisticsService.recordCacheHit(cacheKey);
            log.trace("Cache hit (decorator): {}", cacheKey);
            return cachedValue.get();
        }

        // Cache miss, execute method
        statisticsService.recordCacheMiss(cacheKey);
        log.trace("Cache miss (decorator): {}", cacheKey);

        Object result = joinPoint.proceed();

        // Store in cache
        if (result != null) {
            cacheManager.put(cacheKey, result, ttl);
            statisticsService.recordCachePut(cacheKey);
            log.trace("Cached result: {}", cacheKey);
        }

        return result;
    }

    /**
     * Cache evict advice
     */
    @Around("@annotation(com.droid.bss.infrastructure.cache.advanced.CacheEvict)")
    public Object cacheEvict(ProceedingJoinPoint joinPoint) throws Throwable {
        Method method = getMethod(joinPoint);
        CacheEvict cacheEvict = method.getAnnotation(CacheEvict.class);

        String className = method.getDeclaringClass().getSimpleName();
        String methodName = method.getName();
        Object[] args = joinPoint.getArgs();

        // Generate cache key
        String cacheKey = keyGenerator.generateKey(className, methodName, args);

        // Evict from cache
        cacheManager.evict(cacheKey);
        log.trace("Evicted cache: {}", cacheKey);

        // Execute method
        return joinPoint.proceed();
    }

    /**
     * Cache put advice
     */
    @Around("@annotation(com.droid.bss.infrastructure.cache.advanced.CachePut)")
    public Object cachePut(ProceedingJoinPoint joinPoint) throws Throwable {
        Method method = getMethod(joinPoint);
        CachePut cachePut = method.getAnnotation(CachePut.class);

        String className = method.getDeclaringClass().getSimpleName();
        String methodName = method.getName();
        Object[] args = joinPoint.getArgs();
        Duration ttl = Duration.ofSeconds(cachePut.ttl());

        // Execute method first
        Object result = joinPoint.proceed();

        // Store in cache
        if (result != null) {
            String cacheKey = keyGenerator.generateKey(className, methodName, args);
            cacheManager.put(cacheKey, result, ttl);
            log.trace("Cache put: {}", cacheKey);
        }

        return result;
    }

    private Method getMethod(ProceedingJoinPoint joinPoint) {
        return Arrays.stream(joinPoint.getTarget().getClass().getDeclaredMethods())
            .filter(m -> m.getName().equals(joinPoint.getSignature().getName()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Method not found"));
    }
}
