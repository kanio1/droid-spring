package com.droid.bss.application.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * High-performance caching service with TTL and cache warming
 */
@Service
public class PerformanceCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final long defaultTtlSeconds;

    public PerformanceCacheService(RedisTemplate<String, Object> redisTemplate,
                                  @Value("${bss.cache.default-ttl-seconds:300}") long defaultTtlSeconds) {
        this.redisTemplate = redisTemplate;
        this.defaultTtlSeconds = defaultTtlSeconds;
    }

    public <T> T get(String key, Class<T> type) {
        Object value = redisTemplate.opsForValue().get(key);
        return type.cast(value);
    }

    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value, defaultTtlSeconds, TimeUnit.SECONDS);
    }

    public void setWithTtl(String key, Object value, long ttlSeconds) {
        redisTemplate.opsForValue().set(key, value, ttlSeconds, TimeUnit.SECONDS);
    }

    public void setWithCustomTtl(String key, Object value, long valueTimeout, TimeUnit timeUnit) {
        redisTemplate.opsForValue().set(key, value, valueTimeout, timeUnit);
    }

    public boolean evict(String key) {
        return Boolean.TRUE.equals(redisTemplate.delete(key));
    }

    public void evictPattern(String pattern) {
        var keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    public void clear() {
        redisTemplate.getConnectionFactory().getConnection().flushDb();
    }

    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public void putIfAbsent(String key, Object value) {
        redisTemplate.opsForValue().setIfAbsent(key, value, defaultTtlSeconds, TimeUnit.SECONDS);
    }

    public void putIfAbsentWithTtl(String key, Object value, long ttlSeconds) {
        redisTemplate.opsForValue().setIfAbsent(key, value, ttlSeconds, TimeUnit.SECONDS);
    }

    public long increment(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    public long incrementBy(String key, long delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }

    public void expire(String key, long ttlSeconds) {
        redisTemplate.expire(key, ttlSeconds, TimeUnit.SECONDS);
    }

    public long getTtl(String key) {
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    public <T> T getOrCompute(String key, Class<T> type, CacheLoader<T> loader) {
        T value = get(key, type);
        if (value == null) {
            value = loader.load();
            set(key, value);
        }
        return value;
    }

    public <T> T getOrComputeWithTtl(String key, Class<T> type, CacheLoader<T> loader, long ttlSeconds) {
        T value = get(key, type);
        if (value == null) {
            value = loader.load();
            setWithTtl(key, value, ttlSeconds);
        }
        return value;
    }

    public void warmUp(String key, Object value) {
        setWithTtl(key, value, defaultTtlSeconds);
    }

    public void batchSet(java.util.Map<String, Object> entries) {
        entries.forEach((key, value) -> setWithTtl(key, value, defaultTtlSeconds));
    }

    @FunctionalInterface
    public interface CacheLoader<T> {
        T load();
    }
}
