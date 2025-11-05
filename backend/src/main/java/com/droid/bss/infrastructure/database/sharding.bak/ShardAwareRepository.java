package com.droid.bss.infrastructure.database.sharding;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for sharded databases.
 * <p>
 * Provides automatic routing to the appropriate shard based on the shard key.
 *
 * @param <T> the entity type
 * @param <ID> the ID type
 * @since 1.0
 */
public interface ShardAwareRepository<T, ID> {

    /**
     * Saves an entity to the appropriate shard.
     *
     * @param entity the entity to save
     * @return the saved entity
     */
    T save(T entity);

    /**
     * Saves multiple entities to their appropriate shards.
     *
     * @param entities the entities to save
     * @return the saved entities
     */
    List<T> saveAll(Iterable<T> entities);

    /**
     * Finds an entity by ID.
     *
     * @param id the ID
     * @return the entity if found
     */
    Optional<T> findById(ID id);

    /**
     * Finds entities by IDs.
     *
     * @param ids the IDs
     * @return the entities
     */
    List<T> findAllById(Iterable<ID> ids);

    /**
     * Counts the total number of entities across all shards.
     *
     * @return the total count
     */
    long count();

    /**
     * Counts the number of entities in a specific shard.
     *
     * @param shardId the shard ID
     * @return the count
     */
    long count(String shardId);

    /**
     * Deletes an entity.
     *
     * @param entity the entity to delete
     */
    void delete(T entity);

    /**
     * Deletes an entity by ID.
     *
     * @param id the ID
     */
    void deleteById(ID id);

    /**
     * Deletes multiple entities.
     *
     * @param entities the entities to delete
     */
    void deleteAll(Iterable<T> entities);

    /**
     * Checks if an entity exists by ID.
     *
     * @param id the ID
     * @return true if exists
     */
    boolean existsById(ID id);

    /**
     * Finds all entities across all shards.
     *
     * @return all entities
     */
    List<T> findAll();

    /**
     * Finds all entities in a specific shard.
     *
     * @param shardId the shard ID
     * @return the entities
     */
    List<T> findAll(String shardId);

    /**
     * Executes a query on all shards.
     *
     * @param query the query to execute
     * @return the results
     */
    <R> List<R> executeOnAllShards(ShardQuery<T, R> query);

    /**
     * Executes a query on a specific shard.
     *
     * @param shardId the shard ID
     * @param query the query to execute
     * @return the results
     */
    <R> List<R> executeOnShard(String shardId, ShardQuery<T, R> query);

    /**
     * Gets the shard manager.
     *
     * @return the shard manager
     */
    ShardManager getShardManager();

    /**
     * Gets the shard key for an entity or ID.
     *
     * @param entityOrId the entity or ID
     * @return the shard key
     */
    ShardKey getShardKey(Object entityOrId);

    /**
     * Functional interface for shard queries.
     *
     * @param <T> the entity type
     * @param <R> the result type
     */
    @FunctionalInterface
    interface ShardQuery<T, R> {
        /**
         * Executes the query.
         *
         * @param repository the repository
         * @return the result
         */
        List<R> execute(ShardAwareRepository<T, ?> repository);
    }
}
