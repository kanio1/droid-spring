package com.droid.bss.infrastructure.database.sharding;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link ShardManager}.
 * <p>
 * Provides thread-safe shard management with caching and routing.
 *
 * @since 1.0
 */
public class DefaultShardManager implements ShardManager {

    private static final Logger log = LoggerFactory.getLogger(DefaultShardManager.class);

    private final ShardRoutingStats routingStats;
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Lock writeLock = rwLock.writeLock();
    private final ConcurrentMap<String, Shard> shards;
    private final ConcurrentMap<String, Shard> activeShardsCache;
    private final ShardingStrategy shardingStrategy;
    private final AtomicReference<Instant> lastCacheRefresh;

    /**
     * Creates a new DefaultShardManager.
     *
     * @param shardingStrategy the sharding strategy to use
     */
    public DefaultShardManager(ShardingStrategy shardingStrategy) {
        this.shardingStrategy = Objects.requireNonNull(shardingStrategy, "Sharding strategy cannot be null");
        this.routingStats = new ShardRoutingStats();
        this.shards = new ConcurrentHashMap<>();
        this.activeShardsCache = new ConcurrentHashMap<>();
        this.lastCacheRefresh = new AtomicReference<>(Instant.now());
    }

    @Override
    public Shard getShard(ShardKey shardKey) {
        if (shardKey == null) {
            throw new IllegalArgumentException("Shard key cannot be null");
        }

        List<Shard> activeShards = getAllShards();
        if (activeShards.isEmpty()) {
            throw new IllegalStateException("No shards available");
        }

        String shardId;
        try {
            shardId = shardingStrategy.determineShard(shardKey, activeShards);
        } catch (Exception e) {
            throw new ShardingException("Failed to determine shard for key: " + shardKey.getValue(), e);
        }

        Shard shard = shards.get(shardId);
        if (shard == null || !shard.isActive()) {
            throw new ShardingException("Shard not found or inactive: " + shardId);
        }

        return shard;
    }

    @Override
    public Optional<Shard> getShardById(String shardId) {
        if (shardId == null || shardId.trim().isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(shards.get(shardId));
    }

    @Override
    public void registerShard(Shard shard) {
        if (shard == null) {
            throw new IllegalArgumentException("Shard cannot be null");
        }
        if (shard.getId() == null || shard.getId().trim().isEmpty()) {
            throw new IllegalArgumentException("Shard ID cannot be null or empty");
        }

        shards.put(shard.getId(), shard);
        refreshActiveShardsCache();
    }

    @Override
    public void unregisterShard(String shardId) {
        if (shardId == null || shardId.trim().isEmpty()) {
            return;
        }

        shards.remove(shardId);
        refreshActiveShardsCache();
    }

    @Override
    public Shard route(ShardKey shardKey, ShardOperation operation) {
        if (shardKey == null) {
            throw new IllegalArgumentException("Shard key cannot be null");
        }
        if (operation == null) {
            throw new IllegalArgumentException("Operation cannot be null");
        }

        routingStats.recordRouteCalculation();

        Optional<Shard> cachedShard = getCachedShard(shardKey);
        if (cachedShard.isPresent()) {
            routingStats.recordCacheHit();
            routingStats.recordRouting(cachedShard.get().getId());
            return cachedShard.get();
        }

        routingStats.recordCacheMiss();

        List<Shard> activeShards = getAllShards().stream()
            .filter(Shard::isActive)
            .collect(Collectors.toList());

        if (activeShards.isEmpty()) {
            routingStats.recordError(null);
            throw new ShardingException("No active shards available");
        }

        String shardId;
        try {
            shardId = shardingStrategy.determineShard(shardKey, activeShards);
        } catch (Exception e) {
            routingStats.recordError(null);
            throw new ShardingException("Failed to determine shard for key: " + shardKey.getValue(), e);
        }

        Shard shard = shards.get(shardId);
        if (shard == null || !shard.isActive()) {
            routingStats.recordError(shardId);
            throw new ShardingException("Shard not found or inactive: " + shardId);
        }

        cacheShard(shardKey, shard);
        routingStats.recordRouting(shard.getId());

        return shard;
    }

    @Override
    public List<Shard> broadcast(ShardOperation operation) {
        if (operation == null) {
            throw new IllegalArgumentException("Operation cannot be null");
        }

        List<Shard> targetShards = getAllShards();
        if (targetShards.isEmpty()) {
            return Collections.emptyList();
        }

        for (Shard shard : targetShards) {
            if (!shard.isActive()) {
                log.warn("Skipping inactive shard: {}", shard.getId());
            }
        }

        return targetShards;
    }

    @Override
    public List<Shard> getAllShards() {
        return List.copyOf(shards.values());
    }

    @Override
    public Shard getDefaultShard() {
        List<Shard> activeShards = getAllShards().stream()
            .filter(Shard::isActive)
            .collect(Collectors.toList());

        if (activeShards.isEmpty()) {
            throw new IllegalStateException("No active shards available");
        }

        return activeShards.get(0);
    }

    @Override
    public boolean hasShard(String shardId) {
        return shardId != null && !shardId.trim().isEmpty() && shards.containsKey(shardId);
    }

    @Override
    public int getShardCount() {
        return shards.size();
    }

    @Override
    public ShardingStrategy getStrategy() {
        return shardingStrategy;
    }

    @Override
    public boolean isHealthy() {
        return !shards.isEmpty() &&
               getAllShards().stream().anyMatch(Shard::isActive) &&
               routingStats.getTotalRequests() > 0;
    }

    @Override
    public void close() {
        writeLock.lock();
        try {
            // Clear all shards
            shards.clear();
            activeShardsCache.clear();

            log.info("Shard manager closed");
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public ShardRoutingStats getRoutingStats() {
        return routingStats;
    }

    /**
     * Gets a cached shard for the given shard key.
     *
     * @param shardKey the shard key
     * @return the cached shard if available
     */
    private Optional<Shard> getCachedShard(ShardKey shardKey) {
        String cacheKey = generateCacheKey(shardKey);
        return Optional.ofNullable(activeShardsCache.get(cacheKey));
    }

    /**
     * Caches a shard for the given shard key.
     *
     * @param shardKey the shard key
     * @param shard the shard to cache
     */
    private void cacheShard(ShardKey shardKey, Shard shard) {
        String cacheKey = generateCacheKey(shardKey);
        activeShardsCache.put(cacheKey, shard);
    }

    /**
     * Generates a cache key for a shard key.
     *
     * @param shardKey the shard key
     * @return the cache key
     */
    private String generateCacheKey(ShardKey shardKey) {
        return shardKey.getValue() + ":" + shardingStrategy.getName();
    }

    /**
     * Refreshes the active shards cache.
     */
    private void refreshActiveShardsCache() {
        Instant now = Instant.now();
        Instant lastRefresh = lastCacheRefresh.get();

        if (Duration.between(lastRefresh, now).toSeconds() < 5) {
            return;
        }

        activeShardsCache.clear();
        for (Shard shard : shards.values()) {
            if (shard.isActive()) {
                activeShardsCache.put(shard.getId(), shard);
            }
        }
        lastCacheRefresh.set(now);
    }

    /**
     * Clears all statistics.
     */
    public void clearStats() {
        routingStats.reset();
    }

    @Override
    public String toString() {
        return "DefaultShardManager{" +
            "strategy=" + shardingStrategy.getName() +
            ", totalShards=" + shards.size() +
            ", activeShards=" + activeShardsCache.size() +
            '}';
    }
}
