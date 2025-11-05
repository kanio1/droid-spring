package com.droid.bss.infrastructure.database.sharding;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link ShardAwareRepository}.
 * <p>
 * Provides automatic routing to shards based on entity or ID.
 *
 * @param <T> the entity type
 * @param <ID> the ID type
 * @since 1.0
 */
public class DefaultShardAwareRepository<T, ID> implements ShardAwareRepository<T, ID> {

    private final ShardManager shardManager;
    private final ConcurrentMap<String, RepositoryDelegate<T, ID>> delegates;
    private final ShardKeyExtractor<T, ID> shardKeyExtractor;

    /**
     * Creates a new DefaultShardAwareRepository.
     *
     * @param shardManager the shard manager
     * @param shardKeyExtractor the shard key extractor
     */
    public DefaultShardAwareRepository(
        ShardManager shardManager,
        ShardKeyExtractor<T, ID> shardKeyExtractor) {
        this.shardManager = Objects.requireNonNull(shardManager, "Shard manager cannot be null");
        this.shardKeyExtractor = Objects.requireNonNull(shardKeyExtractor, "Shard key extractor cannot be null");
        this.delegates = new ConcurrentHashMap<>();
    }

    @Override
    public T save(T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }

        ShardKey shardKey = getShardKey(entity);
        Optional<Shard> shard = shardManager.route(shardKey, ShardOperation.WRITE);

        if (shard.isEmpty()) {
            throw new ShardingException("No shard available for key: " + shardKey.getValue());
        }

        RepositoryDelegate<T, ID> delegate = getDelegate(shard.get().getId());
        return delegate.save(entity);
    }

    @Override
    public List<T> saveAll(Iterable<T> entities) {
        if (entities == null) {
            throw new IllegalArgumentException("Entities cannot be null");
        }

        Map<String, List<T>> entitiesByShard = new HashMap<>();

        for (T entity : entities) {
            ShardKey shardKey = getShardKey(entity);
            Optional<Shard> shard = shardManager.route(shardKey, ShardOperation.WRITE);

            if (shard.isPresent()) {
                String shardId = shard.get().getId();
                entitiesByShard.computeIfAbsent(shardId, k -> new ArrayList<>()).add(entity);
            }
        }

        List<T> saved = new ArrayList<>();
        for (Map.Entry<String, List<T>> entry : entitiesByShard.entrySet()) {
            RepositoryDelegate<T, ID> delegate = getDelegate(entry.getKey());
            saved.addAll(delegate.saveAll(entry.getValue()));
        }

        return saved;
    }

    @Override
    public Optional<T> findById(ID id) {
        if (id == null) {
            return Optional.empty();
        }

        ShardKey shardKey = getShardKey(id);
        Optional<Shard> shard = shardManager.route(shardKey, ShardOperation.READ);

        if (shard.isEmpty()) {
            return Optional.empty();
        }

        RepositoryDelegate<T, ID> delegate = getDelegate(shard.get().getId());
        return delegate.findById(id);
    }

    @Override
    public List<T> findAllById(Iterable<ID> ids) {
        if (ids == null) {
            return Collections.emptyList();
        }

        Map<String, List<ID>> idsByShard = new HashMap<>();

        for (ID id : ids) {
            ShardKey shardKey = getShardKey(id);
            Optional<Shard> shard = shardManager.route(shardKey, ShardOperation.READ);

            if (shard.isPresent()) {
                String shardId = shard.get().getId();
                idsByShard.computeIfAbsent(shardId, k -> new ArrayList<>()).add(id);
            }
        }

        List<T> results = new ArrayList<>();
        for (Map.Entry<String, List<ID>> entry : idsByShard.entrySet()) {
            RepositoryDelegate<T, ID> delegate = getDelegate(entry.getKey());
            results.addAll(delegate.findAllById(entry.getValue()));
        }

        return results;
    }

    @Override
    public long count() {
        long total = 0;
        for (Shard shard : shardManager.getAllShards()) {
            if (shard.isActive()) {
                RepositoryDelegate<T, ID> delegate = getDelegate(shard.getId());
                total += delegate.count();
            }
        }
        return total;
    }

    @Override
    public long count(String shardId) {
        if (shardId == null || shardId.trim().isEmpty()) {
            return 0;
        }

        RepositoryDelegate<T, ID> delegate = getDelegate(shardId);
        return delegate.count();
    }

    @Override
    public void delete(T entity) {
        if (entity == null) {
            return;
        }

        ShardKey shardKey = getShardKey(entity);
        Optional<Shard> shard = shardManager.route(shardKey, ShardOperation.DELETE);

        if (shard.isPresent()) {
            RepositoryDelegate<T, ID> delegate = getDelegate(shard.get().getId());
            delegate.delete(entity);
        }
    }

    @Override
    public void deleteById(ID id) {
        if (id == null) {
            return;
        }

        ShardKey shardKey = getShardKey(id);
        Optional<Shard> shard = shardManager.route(shardKey, ShardOperation.DELETE);

        if (shard.isPresent()) {
            RepositoryDelegate<T, ID> delegate = getDelegate(shard.get().getId());
            delegate.deleteById(id);
        }
    }

    @Override
    public void deleteAll(Iterable<T> entities) {
        if (entities == null) {
            return;
        }

        Map<String, List<T>> entitiesByShard = new HashMap<>();

        for (T entity : entities) {
            ShardKey shardKey = getShardKey(entity);
            Optional<Shard> shard = shardManager.route(shardKey, ShardOperation.DELETE);

            if (shard.isPresent()) {
                String shardId = shard.get().getId();
                entitiesByShard.computeIfAbsent(shardId, k -> new ArrayList<>()).add(entity);
            }
        }

        for (Map.Entry<String, List<T>> entry : entitiesByShard.entrySet()) {
            RepositoryDelegate<T, ID> delegate = getDelegate(entry.getKey());
            delegate.deleteAll(entry.getValue());
        }
    }

    @Override
    public boolean existsById(ID id) {
        if (id == null) {
            return false;
        }

        ShardKey shardKey = getShardKey(id);
        Optional<Shard> shard = shardManager.route(shardKey, ShardOperation.READ);

        if (shard.isEmpty()) {
            return false;
        }

        RepositoryDelegate<T, ID> delegate = getDelegate(shard.get().getId());
        return delegate.existsById(id);
    }

    @Override
    public List<T> findAll() {
        List<T> results = new ArrayList<>();
        for (Shard shard : shardManager.getAllShards()) {
            if (shard.isActive()) {
                RepositoryDelegate<T, ID> delegate = getDelegate(shard.getId());
                results.addAll(delegate.findAll());
            }
        }
        return results;
    }

    @Override
    public List<T> findAll(String shardId) {
        if (shardId == null || shardId.trim().isEmpty()) {
            return Collections.emptyList();
        }

        RepositoryDelegate<T, ID> delegate = getDelegate(shardId);
        return delegate.findAll();
    }

    @Override
    public <R> List<R> executeOnAllShards(ShardQuery<T, R> query) {
        List<R> results = new ArrayList<>();
        for (Shard shard : shardManager.getAllShards()) {
            if (shard.isActive()) {
                RepositoryDelegate<T, ID> delegate = getDelegate(shard.getId());
                List<R> shardResults = query.execute(this);
                results.addAll(shardResults);
            }
        }
        return results;
    }

    @Override
    public <R> List<R> executeOnShard(String shardId, ShardQuery<T, R> query) {
        if (shardId == null || shardId.trim().isEmpty()) {
            return Collections.emptyList();
        }

        RepositoryDelegate<T, ID> delegate = getDelegate(shardId);
        return query.execute(this);
    }

    @Override
    public ShardManager getShardManager() {
        return shardManager;
    }

    @Override
    public ShardKey getShardKey(Object entityOrId) {
        return shardKeyExtractor.extract(entityOrId);
    }

    /**
     * Gets or creates a delegate for a shard.
     *
     * @param shardId the shard ID
     * @return the delegate
     */
    private RepositoryDelegate<T, ID> getDelegate(String shardId) {
        return delegates.computeIfAbsent(shardId, this::createDelegate);
    }

    /**
     * Creates a delegate for a shard.
     *
     * @param shardId the shard ID
     * @return the delegate
     */
    private RepositoryDelegate<T, ID> createDelegate(String shardId) {
        return new RepositoryDelegate<>(shardId);
    }

    /**
     * Functional interface for extracting shard keys from entities or IDs.
     *
     * @param <T> the entity type
     * @param <ID> the ID type
     */
    @FunctionalInterface
    public interface ShardKeyExtractor<T, ID> {
        /**
         * Extracts a shard key from an entity or ID.
         *
         * @param entityOrId the entity or ID
         * @return the shard key
         */
        ShardKey extract(Object entityOrId);
    }

    /**
     * Internal delegate for repository operations on a specific shard.
     *
     * @param <T> the entity type
     * @param <ID> the ID type
     */
    private static class RepositoryDelegate<T, ID> {
        private final String shardId;

        RepositoryDelegate(String shardId) {
            this.shardId = shardId;
        }

        T save(T entity) {
            throw new UnsupportedOperationException("Save not implemented for delegate");
        }

        List<T> saveAll(Iterable<T> entities) {
            throw new UnsupportedOperationException("SaveAll not implemented for delegate");
        }

        Optional<T> findById(ID id) {
            throw new UnsupportedOperationException("FindById not implemented for delegate");
        }

        List<T> findAllById(Iterable<ID> ids) {
            throw new UnsupportedOperationException("FindAllById not implemented for delegate");
        }

        long count() {
            throw new UnsupportedOperationException("Count not implemented for delegate");
        }

        void delete(T entity) {
            throw new UnsupportedOperationException("Delete not implemented for delegate");
        }

        void deleteById(ID id) {
            throw new UnsupportedOperationException("DeleteById not implemented for delegate");
        }

        void deleteAll(Iterable<T> entities) {
            throw new UnsupportedOperationException("DeleteAll not implemented for delegate");
        }

        boolean existsById(ID id) {
            throw new UnsupportedOperationException("ExistsById not implemented for delegate");
        }

        List<T> findAll() {
            throw new UnsupportedOperationException("FindAll not implemented for delegate");
        }
    }

    /**
     * Exception thrown when sharding operations fail.
     */
    public static class ShardingException extends RuntimeException {
        public ShardingException(String message) {
            super(message);
        }

        public ShardingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
