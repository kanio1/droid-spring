package com.droid.bss.infrastructure.database.sharding;

import java.util.List;
import java.util.Objects;

/**
 * Hash-based sharding strategy.
 *
 * Uses consistent hashing to distribute keys across shards.
 *
 * @since 1.0
 */
public class HashShardingStrategy implements ShardingStrategy {

    private final int hashMultiplier;

    /**
     * Creates a new HashShardingStrategy.
     *
     * @param hashMultiplier the hash multiplier
     */
    public HashShardingStrategy(int hashMultiplier) {
        if (hashMultiplier <= 0) {
            throw new IllegalArgumentException("Hash multiplier must be positive");
        }
        this.hashMultiplier = hashMultiplier;
    }

    /**
     * Creates a HashShardingStrategy with default multiplier.
     */
    public HashShardingStrategy() {
        this(31);
    }

    @Override
    public String determineShard(ShardKey shardKey, List<Shard> shards) {
        if (shardKey == null) {
            throw new IllegalArgumentException("Shard key cannot be null");
        }
        if (shards == null || shards.isEmpty()) {
            throw new IllegalArgumentException("Shards list cannot be null or empty");
        }

        // Filter active shards
        List<Shard> activeShards = shards.stream()
            .filter(Shard::isActive)
            .toList();

        if (activeShards.isEmpty()) {
            throw new IllegalArgumentException("No active shards available");
        }

        // Calculate hash
        int hash = calculateHash(shardKey);

        // Determine shard index
        int shardIndex = Math.abs(hash % activeShards.size());

        return activeShards.get(shardIndex).getId();
    }

    @Override
    public Shard determineShardObject(ShardKey shardKey, List<Shard> shards) {
        String shardId = determineShard(shardKey, shards);

        return shards.stream()
            .filter(s -> Objects.equals(s.getId(), shardId))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Shard not found: " + shardId));
    }

    @Override
    public boolean requiresHashing() {
        return true;
    }

    @Override
    public String getName() {
        return "HASH";
    }

    @Override
    public String getDescription() {
        return "Hash-based sharding using consistent hashing with multiplier " + hashMultiplier;
    }

    private int calculateHash(ShardKey shardKey) {
        String value = shardKey.getValue();
        int hash = 0;

        for (int i = 0; i < value.length(); i++) {
            hash = (hash * hashMultiplier) + value.charAt(i);
        }

        return hash;
    }

    @Override
    public String toString() {
        return "HashShardingStrategy{name='" + getName() + "', hashMultiplier=" + hashMultiplier + '}';
    }
}
