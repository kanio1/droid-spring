package com.droid.bss.infrastructure.database.sharding;

import java.util.List;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Range-based sharding strategy.
 *
 * Distributes keys based on ranges.
 *
 * @since 1.0
 */
public class RangeShardingStrategy implements ShardingStrategy {

    private final long minValue;
    private final long maxValue;
    private final NavigableMap<Long, String> rangeToShardId = new TreeMap<>();

    /**
     * Creates a new RangeShardingStrategy.
     *
     * @param minValue the minimum value
     * @param maxValue the maximum value
     */
    public RangeShardingStrategy(long minValue, long maxValue) {
        if (minValue >= maxValue) {
            throw new IllegalArgumentException("Min value must be less than max value");
        }
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    /**
     * Adds a range mapping.
     *
     * @param start the start of the range (inclusive)
     * @param end the end of the range (exclusive)
     * @param shardId the shard ID
     * @return this for chaining
     */
    public RangeShardingStrategy addRange(long start, long end, String shardId) {
        if (start >= end) {
            throw new IllegalArgumentException("Start must be less than end");
        }
        if (start < minValue || end > maxValue) {
            throw new IllegalArgumentException("Range must be within [" + minValue + ", " + maxValue + "]");
        }
        rangeToShardId.put(end, shardId);
        return this;
    }

    @Override
    public String determineShard(ShardKey shardKey, List<Shard> shards) {
        if (shardKey == null) {
            throw new IllegalArgumentException("Shard key cannot be null");
        }
        if (shards == null || shards.isEmpty()) {
            throw new IllegalArgumentException("Shards list cannot be null or empty");
        }
        if (!shardKey.isNumeric()) {
            throw new IllegalArgumentException("Range strategy requires numeric shard key");
        }

        long keyValue = shardKey.getLongValue();

        if (keyValue < minValue || keyValue >= maxValue) {
            throw new IllegalArgumentException(
                "Key value " + keyValue + " is outside range [" + minValue + ", " + maxValue + "]"
            );
        }

        // Find the shard for this key
        String shardId = rangeToShardId.floorEntry(Long.valueOf(keyValue)).getValue();

        // Verify shard is active
        Shard shard = shards.stream()
            .filter(s -> Objects.equals(s.getId(), shardId))
            .findFirst()
            .orElse(null);

        if (shard == null || !shard.isActive()) {
            throw new IllegalArgumentException("Shard not found or inactive: " + shardId);
        }

        return shardId;
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
        return false;
    }

    @Override
    public String getName() {
        return "RANGE";
    }

    @Override
    public String getDescription() {
        return "Range-based sharding for range [" + minValue + ", " + maxValue + "] with " +
               rangeToShardId.size() + " range(s)";
    }

    @Override
    public String toString() {
        return "RangeShardingStrategy{name='" + getName() + "', range=[" + minValue + ", " + maxValue + "]}'";
    }
}
