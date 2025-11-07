package com.droid.bss.infrastructure.graphql.persisted;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents a persisted GraphQL query
 */
public class PersistedQuery {

    private final String hash;
    private final String query;
    private final String operationName;
    private final String description;
    private final LocalDateTime createdAt;
    private final LocalDateTime lastAccessedAt;
    private final int accessCount;

    public PersistedQuery(
            String hash,
            String query,
            String operationName,
            String description,
            LocalDateTime createdAt,
            LocalDateTime lastAccessedAt,
            int accessCount) {
        this.hash = hash;
        this.query = query;
        this.operationName = operationName;
        this.description = description;
        this.createdAt = createdAt;
        this.lastAccessedAt = lastAccessedAt;
        this.accessCount = accessCount;
    }

    public static PersistedQuery create(String hash, String query, String operationName, String description) {
        return new PersistedQuery(
            hash,
            query,
            operationName,
            description,
            LocalDateTime.now(),
            LocalDateTime.now(),
            0
        );
    }

    public PersistedQuery updateAccess() {
        return new PersistedQuery(
            hash,
            query,
            operationName,
            description,
            createdAt,
            LocalDateTime.now(),
            accessCount + 1
        );
    }

    public String getHash() {
        return hash;
    }

    public String getQuery() {
        return query;
    }

    public String getOperationName() {
        return operationName;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastAccessedAt() {
        return lastAccessedAt;
    }

    public int getAccessCount() {
        return accessCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PersistedQuery that = (PersistedQuery) o;
        return Objects.equals(hash, that.hash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hash);
    }

    @Override
    public String toString() {
        return "PersistedQuery{" +
            "hash='" + hash + '\'' +
            ", operationName='" + operationName + '\'' +
            ", description='" + description + '\'' +
            ", accessCount=" + accessCount +
            '}';
    }
}
