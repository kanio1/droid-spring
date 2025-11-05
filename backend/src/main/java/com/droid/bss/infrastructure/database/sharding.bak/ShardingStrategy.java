package com.droid.bss.infrastructure.database.sharding;

import java.util.List;

/**
 * Strategy for determining which shard to use.
 *
 * @since 1.0
 */
public interface ShardingStrategy {

    /**
     * Determines the shard ID for a given shard key.
     *
     * @param shardKey the shard key
     * @param shards the available shards
     * @return the shard ID
     */
    String determineShard(ShardKey shardKey, List<Shard> shards);

    /**
     * Determines the shard for a given shard key.
     *
     * @param shardKey the shard key
     * @param shards the available shards
     * @return the shard
     */
    Shard determineShardObject(ShardKey shardKey, List<Shard> shards);

    /**
     * Checks if this strategy requires hashing the key.
     *
     * @return true if hashing is required
     */
    boolean requiresHashing();

    /**
     * Gets the strategy name.
     *
     * @return the name
     */
    String getName();

    /**
     * Gets the strategy description.
     *
     * @return the description
     */
    String getDescription();
}
