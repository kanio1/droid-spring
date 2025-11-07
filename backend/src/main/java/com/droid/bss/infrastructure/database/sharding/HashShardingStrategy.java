package com.droid.bss.infrastructure.database.sharding;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Stub class for hash-based sharding strategy
 * Minimal implementation for testing purposes
 */
public class HashShardingStrategy implements ShardingStrategy {

    private final List<Shard> shards;
    private final int numberOfShards;

    public HashShardingStrategy(int numberOfShards) {
        this.shards = new CopyOnWriteArrayList<>();
        this.numberOfShards = numberOfShards;
    }

    public void addShard(Shard shard) {
        shards.add(shard);
    }

    @Override
    public Optional<Shard> route(String key) {
        if (shards.isEmpty()) {
            return Optional.empty();
        }

        int hash = Math.abs(key.hashCode());
        int shardIndex = hash % shards.size();
        return Optional.of(shards.get(shardIndex));
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
