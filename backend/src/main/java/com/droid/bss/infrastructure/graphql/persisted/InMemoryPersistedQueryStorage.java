package com.droid.bss.infrastructure.graphql.persisted;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory implementation of persisted query storage
 * For production, use Redis-based implementation
 */
@Slf4j
public class InMemoryPersistedQueryStorage implements PersistedQueryStorage {

    private final Map<String, PersistedQuery> queries = new ConcurrentHashMap<>();

    @Override
    public void storeQuery(String hash, String query, String operationName, String description) {
        PersistedQuery persistedQuery = PersistedQuery.create(hash, query, operationName, description);
        queries.put(hash, persistedQuery);
        log.debug("Stored persisted query: {} - {}", hash, operationName);
    }

    @Override
    public Optional<PersistedQuery> getQuery(String hash) {
        return Optional.ofNullable(queries.get(hash))
            .map(PersistedQuery::updateAccess);
    }

    @Override
    public boolean deleteQuery(String hash) {
        PersistedQuery removed = queries.remove(hash);
        if (removed != null) {
            log.debug("Deleted persisted query: {}", hash);
            return true;
        }
        return false;
    }

    @Override
    public List<PersistedQuery> listQueries() {
        return queries.values().stream()
            .sorted(Comparator.comparing(PersistedQuery::getLastAccessedAt).reversed())
            .collect(Collectors.toList());
    }

    @Override
    public boolean exists(String hash) {
        return queries.containsKey(hash);
    }

    @Override
    public void clear() {
        queries.clear();
        log.info("Cleared all persisted queries");
    }

    @Override
    public int count() {
        return queries.size();
    }
}
