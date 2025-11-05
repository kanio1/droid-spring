package com.droid.bss.infrastructure.database.sharding;

import java.util.List;
import java.util.Optional;

/**
 * Interface for managing database shards.
 *
 * Handles shard registration, lookup, and routing.
 *
 * @since 1.0
 */
public interface ShardManager {

    /**
     * Gets the shard for a given shard key.
     *
     * @param shardKey the shard key
     * @return the shard
     */
    Shard getShard(ShardKey shardKey);

    /**
     * Gets the shard for a given shard ID.
     *
     * @param shardId the shard ID
     * @return the shard (may be empty)
     */
    Optional<Shard> getShardById(String shardId);

    /**
     * Gets all available shards.
     *
     * @return the list of shards
     */
    List<Shard> getAllShards();

    /**
     * Gets the default shard.
     *
     * @return the default shard
     */
    Shard getDefaultShard();

    /**
     * Registers a shard.
     *
     * @param shard the shard to register
     */
    void registerShard(Shard shard);

    /**
     * Unregisters a shard.
     *
     * @param shardId the shard ID
     */
    void unregisterShard(String shardId);

    /**
     * Checks if a shard exists.
     *
     * @param shardId the shard ID
     * @return true if shard exists
     */
    boolean hasShard(String shardId);

    /**
     * Gets the total number of shards.
     *
     * @return the shard count
     */
    int getShardCount();

    /**
     * Gets the shard routing statistics.
     *
     * @return the statistics
     */
    ShardRoutingStats getRoutingStats();

    /**
     * Routes a request to the appropriate shard.
     *
     * @param shardKey the shard key
     * @param operation the operation type
     * @return the shard
     */
    Shard route(ShardKey shardKey, ShardOperation operation);

    /**
     * Broadcasts a request to all shards.
     *
     * @param operation the operation type
     * @return the list of shards
     */
    List<Shard> broadcast(ShardOperation operation);

    /**
     * Gets the sharding strategy.
     *
     * @return the strategy
     */
    ShardingStrategy getStrategy();

    /**
     * Checks if the shard manager is healthy.
     *
     * @return true if healthy
     */
    boolean isHealthy();

    /**
     * Closes the shard manager.
     */
    void close();
}
