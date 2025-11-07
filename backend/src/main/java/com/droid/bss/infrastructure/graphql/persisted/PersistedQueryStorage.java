package com.droid.bss.infrastructure.graphql.persisted;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Storage interface for persisted GraphQL queries
 */
public interface PersistedQueryStorage {

    /**
     * Store a persisted query
     */
    void storeQuery(String hash, String query, String operationName, String description);

    /**
     * Retrieve a persisted query by hash
     */
    Optional<PersistedQuery> getQuery(String hash);

    /**
     * Delete a persisted query
     */
    boolean deleteQuery(String hash);

    /**
     * List all persisted queries
     */
    List<PersistedQuery> listQueries();

    /**
     * Check if a query hash exists
     */
    boolean exists(String hash);

    /**
     * Clear all persisted queries
     */
    void clear();

    /**
     * Get count of persisted queries
     */
    int count();
}
