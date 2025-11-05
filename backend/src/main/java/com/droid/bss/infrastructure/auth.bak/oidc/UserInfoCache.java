package com.droid.bss.infrastructure.auth.oidc;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory cache for UserInfo responses.
 *
 * @since 1.0
 */
public class UserInfoCache {

    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final long ttlMillis;
    private final int maxSize;

    public UserInfoCache(OidcProperties properties) {
        this.ttlMillis = properties.getUserInfoCacheTtl().toMillis();
        this.maxSize = properties.getUserInfoCacheMaxSize();
    }

    /**
     * Puts a UserInfo in the cache.
     *
     * @param accessToken the access token
     * @param userInfo the UserInfo
     */
    public void put(String accessToken, UserInfo userInfo) {
        if (accessToken == null || userInfo == null) {
            return;
        }

        if (cache.size() >= maxSize) {
            evictExpired();
            if (cache.size() >= maxSize) {
                cache.clear();
            }
        }

        cache.put(accessToken, new CacheEntry(userInfo, Instant.now().plusMillis(ttlMillis)));
    }

    /**
     * Gets a UserInfo from the cache.
     *
     * @param accessToken the access token
     * @return the cached UserInfo or null
     */
    public UserInfo get(String accessToken) {
        if (accessToken == null) {
            return null;
        }

        CacheEntry entry = cache.get(accessToken);
        if (entry == null) {
            return null;
        }

        if (Instant.now().isAfter(entry.expiresAt)) {
            cache.remove(accessToken);
            return null;
        }

        return entry.userInfo;
    }

    /**
     * Removes a UserInfo from the cache.
     *
     * @param accessToken the access token
     */
    public void remove(String accessToken) {
        if (accessToken != null) {
            cache.remove(accessToken);
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
        final UserInfo userInfo;
        final Instant expiresAt;

        CacheEntry(UserInfo userInfo, Instant expiresAt) {
            this.userInfo = userInfo;
            this.expiresAt = expiresAt;
        }
    }
}
