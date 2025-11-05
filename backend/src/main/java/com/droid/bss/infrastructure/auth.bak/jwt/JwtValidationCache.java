package com.droid.bss.infrastructure.auth.jwt;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Simple in-memory cache for JWT validation results.
 * Note: For production, consider using Redis or other distributed cache.
 *
 * @since 1.0
 */
public class JwtValidationCache {

    private final ConcurrentMap<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final long ttlMillis;
    private final int maxSize;

    public JwtValidationCache(JwtProperties properties) {
        this.ttlMillis = properties.getCacheTtl().toMillis();
        this.maxSize = properties.getCacheMaxSize();
    }

    /**
     * Puts a validation result in the cache.
     *
     * @param tokenId the token ID
     * @param result the validation result
     */
    public void put(String tokenId, JwtValidationResult result) {
        if (!result.isValid() || tokenId == null) {
            return;
        }

        if (cache.size() >= maxSize) {
            evictExpired();
            if (cache.size() >= maxSize) {
                cache.clear(); // Simple eviction strategy
            }
        }

        cache.put(tokenId, new CacheEntry(result, Instant.now().plusMillis(ttlMillis)));
    }

    /**
     * Gets a validation result from the cache.
     *
     * @param tokenId the token ID
     * @return the cached result or null if not found or expired
     */
    public JwtValidationResult get(String tokenId) {
        if (tokenId == null) {
            return null;
        }

        CacheEntry entry = cache.get(tokenId);
        if (entry == null) {
            return null;
        }

        if (Instant.now().isAfter(entry.expiresAt)) {
            cache.remove(tokenId);
            return null;
        }

        return entry.result;
    }

    /**
     * Removes a token from the cache.
     *
     * @param tokenId the token ID
     */
    public void remove(String tokenId) {
        if (tokenId != null) {
            cache.remove(tokenId);
        }
    }

    /**
     * Evicts all expired entries.
     */
    public void evictExpired() {
        Instant now = Instant.now();
        cache.entrySet().removeIf(entry -> now.isAfter(entry.getValue().expiresAt));
    }

    /**
     * Clears the entire cache.
     */
    public void clear() {
        cache.clear();
    }

    /**
     * Gets the current cache size.
     *
     * @return the number of entries
     */
    public int size() {
        return cache.size();
    }

    /**
     * Cache entry with expiration time.
     */
    private static class CacheEntry {
        final JwtValidationResult result;
        final Instant expiresAt;

        CacheEntry(JwtValidationResult result, Instant expiresAt) {
            this.result = result;
            this.expiresAt = expiresAt;
        }
    }
}
