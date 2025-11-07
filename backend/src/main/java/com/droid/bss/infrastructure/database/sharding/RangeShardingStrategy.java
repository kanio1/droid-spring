package com.droid.bss.infrastructure.database.sharding;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Stub class for range-based sharding strategy
 * Minimal implementation for testing purposes
 */
public class RangeShardingStrategy implements ShardingStrategy {

    private final List<Shard> shards;

    public RangeShardingStrategy() {
        this.shards = new CopyOnWriteArrayList<>();
    }

    public void addShard(Shard shard) {
        shards.add(shard);
    }

    @Override
    public Optional<Shard> route(String key) {
        if (shards.isEmpty()) {
            return Optional.empty();
        }

        // Simple range-based routing - first shard
        return Optional.of(shards.get(0));
    }

    @Override
    public List<Shard> getAllShards() {
        return List.copyOf(shards);
    }

    @Override
    public boolean isShardAvailable(Shard shard) {
        return shard != null && shard.isActive();
    }
}
