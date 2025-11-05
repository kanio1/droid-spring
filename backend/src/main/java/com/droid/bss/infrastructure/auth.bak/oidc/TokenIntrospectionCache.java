package com.droid.bss.infrastructure.auth.oidc;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory cache for TokenIntrospection responses.
 *
 * @since 1.0
 */
public class TokenIntrospectionCache {

    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final long ttlMillis;
    private final int maxSize;

    public TokenIntrospectionCache(OidcProperties properties) {
        this.ttlMillis = properties.getIntrospectionCacheTtl().toMillis();
        this.maxSize = properties.getIntrospectionCacheMaxSize();
    }

    /**
     * Puts a TokenIntrospectionResponse in the cache.
     *
     * @param token the token
     * @param response the introspection response
     */
    public void put(String token, TokenIntrospectionResponse response) {
        if (token == null || response == null) {
            return;
        }

        if (cache.size() >= maxSize) {
            evictExpired();
            if (cache.size() >= maxSize) {
                cache.clear();
            }
        }

        cache.put(token, new CacheEntry(response, Instant.now().plusMillis(ttlMillis)));
    }

    /**
     * Gets a TokenIntrospectionResponse from the cache.
     *
     * @param token the token
     * @return the cached response or null
     */
    public TokenIntrospectionResponse get(String token) {
        if (token == null) {
            return null;
        }

        CacheEntry entry = cache.get(token);
        if (entry == null) {
            return null;
        }

        if (Instant.now().isAfter(entry.expiresAt)) {
            cache.remove(token);
            return null;
        }

        return entry.response;
    }

    /**
     * Removes a response from the cache.
     *
     * @param token the token
     */
    public void remove(String token) {
        if (token != null) {
            cache.remove(token);
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

    private static class CacheEntry {
        final TokenIntrospectionResponse response;
        final Instant expiresAt;

        CacheEntry(TokenIntrospectionResponse response, Instant expiresAt) {
            this.response = response;
            this.expiresAt = expiresAt;
        }
    }
}
