package com.droid.bss.infrastructure.database.sharding;

import java.util.List;
import java.util.Optional;

/**
 * Stub interface for database sharding strategy
 * Minimal implementation for testing purposes
 */
public interface ShardingStrategy {

    /**
     * Route a key to a specific shard
     */
    Optional<Shard> route(String key);

    /**
     * Get all available shards
     */
    List<Shard> getAllShards();

    /**
     * Check if a shard is available
     */
    boolean isShardAvailable(Shard shard);
}
