package com.droid.bss.infrastructure.database.sharding;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Stub class for shard management
 * Minimal implementation for testing purposes
 */
public class DefaultShardManager {

    private final ShardingStrategy shardingStrategy;
    private final ConcurrentHashMap<String, Shard> shards;
    private final ReadWriteLock lock;

    public DefaultShardManager(ShardingStrategy shardingStrategy) {
        this.shardingStrategy = shardingStrategy;
        this.shards = new ConcurrentHashMap<>();
        this.lock = new ReentrantReadWriteLock();
    }

    public void registerShard(Shard shard) {
        lock.writeLock().lock();
        try {
            shards.put(shard.getName(), shard);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void unregisterShard(String shardName) {
        lock.writeLock().lock();
        try {
            shards.remove(shardName);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Optional<Shard> getShard(String shardName) {
        lock.readLock().lock();
        try {
            return Optional.ofNullable(shards.get(shardName));
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<Shard> getAllShards() {
        lock.readLock().lock();
        try {
            return List.copyOf(shards.values());
        } finally {
            lock.readLock().unlock();
        }
    }

    public Optional<Shard> route(String key) {
        return shardingStrategy.route(key);
    }
}
