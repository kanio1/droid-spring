package com.droid.bss.infrastructure.graphql.persisted;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing persisted GraphQL queries
 */
@Slf4j
@Service
public class PersistedQueryService {

    private final PersistedQueryStorage storage;
    private final PersistedQueryHashGenerator hashGenerator;
    private final PersistedQueryValidator validator;

    public PersistedQueryService(
            PersistedQueryStorage storage,
            PersistedQueryHashGenerator hashGenerator,
            PersistedQueryValidator validator) {
        this.storage = storage;
        this.hashGenerator = hashGenerator;
        this.validator = validator;
    }

    /**
     * Persist a new query
     */
    public PersistQueryResult persistQuery(String query, String operationName, String description) {
        log.info("Persisting query: {}", operationName);

        // Validate the query
        PersistedQueryValidator.ValidationResult validation = validator.validate(query, operationName);
        if (!validation.isValid()) {
            return PersistQueryResult.failure(validation.getErrorMessage());
        }

        // Generate hash
        PersistedQueryHashGenerator.PersistedQueryHash hashInfo = hashGenerator.generateDetailedHash(query, operationName);

        // Check if query already exists
        if (storage.exists(hashInfo.getHash())) {
            return PersistQueryResult.failure("Query with hash '" + hashInfo.getHash() + "' already exists");
        }

        // Store the query
        storage.storeQuery(hashInfo.getHash(), query, operationName, description);

        log.info("Persisted query: {} with hash: {}", operationName, hashInfo.getHash());

        return PersistQueryResult.success(hashInfo.getHash(), query, operationName);
    }

    /**
     * Execute a persisted query by hash
     */
    public ExecuteQueryResult executePersistedQuery(String hash) {
        log.debug("Executing persisted query: {}", hash);

        // Get the query from storage
        Optional<PersistedQuery> queryOpt = storage.getQuery(hash);
        if (queryOpt.isEmpty()) {
            return ExecuteQueryResult.notFound("Query not found: " + hash);
        }

        PersistedQuery query = queryOpt.get();
        log.debug("Found persisted query: {} - {}", hash, query.getOperationName());

        return ExecuteQueryResult.success(query.getQuery(), query.getOperationName());
    }

    /**
     * Execute a persisted query with variables
     */
    public ExecuteQueryResult executePersistedQueryWithVariables(String hash) {
        ExecuteQueryResult result = executePersistedQuery(hash);
        if (!result.isFound()) {
            return result;
        }

        // Add variable placeholders if needed
        String query = result.getQuery();
        // In a real implementation, you would handle variables here

        return ExecuteQueryResult.success(query, result.getOperationName());
    }

    /**
     * Get a persisted query without execution
     */
    public GetQueryResult getQuery(String hash) {
        Optional<PersistedQuery> queryOpt = storage.getQuery(hash);
        if (queryOpt.isEmpty()) {
            return GetQueryResult.notFound(hash);
        }

        PersistedQuery query = queryOpt.get();
        return GetQueryResult.found(query);
    }

    /**
     * List all persisted queries
     */
    public List<PersistedQuery> listQueries() {
        return storage.listQueries();
    }

    /**
     * Delete a persisted query
     */
    public DeleteQueryResult deleteQuery(String hash) {
        log.info("Deleting persisted query: {}", hash);

        boolean deleted = storage.deleteQuery(hash);
        if (deleted) {
            log.info("Deleted persisted query: {}", hash);
            return DeleteQueryResult.success(hash);
        } else {
            return DeleteQueryResult.notFound(hash);
        }
    }

    /**
     * Get statistics about persisted queries
     */
    public QueryStatistics getStatistics() {
        List<PersistedQuery> queries = storage.listQueries();
        int totalQueries = queries.size();
        int totalAccessCount = queries.stream()
            .mapToInt(PersistedQuery::getAccessCount)
            .sum();

        return new QueryStatistics(
            totalQueries,
            totalAccessCount,
            totalQueries > 0 ? (double) totalAccessCount / totalQueries : 0.0
        );
    }

    /**
     * Check if a query hash exists
     */
    public boolean queryExists(String hash) {
        return storage.exists(hash);
    }

    /**
     * Result of query persistence
     */
    public static class PersistQueryResult {
        private final boolean success;
        private final String hash;
        private final String query;
        private final String operationName;
        private final String errorMessage;

        private PersistQueryResult(boolean success, String hash, String query, String operationName, String errorMessage) {
            this.success = success;
            this.hash = hash;
            this.query = query;
            this.operationName = operationName;
            this.errorMessage = errorMessage;
        }

        public static PersistQueryResult success(String hash, String query, String operationName) {
            return new PersistQueryResult(true, hash, query, operationName, null);
        }

        public static PersistQueryResult failure(String errorMessage) {
            return new PersistQueryResult(false, null, null, null, errorMessage);
        }

        public boolean isSuccess() {
            return success;
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

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    /**
     * Result of query execution
     */
    public static class ExecuteQueryResult {
        private final boolean found;
        private final String query;
        private final String operationName;
        private final String errorMessage;

        private ExecuteQueryResult(boolean found, String query, String operationName, String errorMessage) {
            this.found = found;
            this.query = query;
            this.operationName = operationName;
            this.errorMessage = errorMessage;
        }

        public static ExecuteQueryResult success(String query, String operationName) {
            return new ExecuteQueryResult(true, query, operationName, null);
        }

        public static ExecuteQueryResult notFound(String errorMessage) {
            return new ExecuteQueryResult(false, null, null, errorMessage);
        }

        public boolean isFound() {
            return found;
        }

        public String getQuery() {
            return query;
        }

        public String getOperationName() {
            return operationName;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    /**
     * Result of query retrieval
     */
    public static class GetQueryResult {
        private final boolean found;
        private final String hash;
        private final PersistedQuery query;
        private final String errorMessage;

        private GetQueryResult(boolean found, String hash, PersistedQuery query, String errorMessage) {
            this.found = found;
            this.hash = hash;
            this.query = query;
            this.errorMessage = errorMessage;
        }

        public static GetQueryResult found(PersistedQuery query) {
            return new GetQueryResult(true, query.getHash(), query, null);
        }

        public static GetQueryResult notFound(String hash) {
            return new GetQueryResult(false, hash, null, "Query not found: " + hash);
        }

        public boolean isFound() {
            return found;
        }

        public String getHash() {
            return hash;
        }

        public PersistedQuery getQuery() {
            return query;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    /**
     * Result of query deletion
     */
    public static class DeleteQueryResult {
        private final boolean success;
        private final String hash;
        private final String errorMessage;

        private DeleteQueryResult(boolean success, String hash, String errorMessage) {
            this.success = success;
            this.hash = hash;
            this.errorMessage = errorMessage;
        }

        public static DeleteQueryResult success(String hash) {
            return new DeleteQueryResult(true, hash, null);
        }

        public static DeleteQueryResult notFound(String hash) {
            return new DeleteQueryResult(false, hash, "Query not found: " + hash);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getHash() {
            return hash;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    /**
     * Statistics about persisted queries
     */
    public static class QueryStatistics {
        private final int totalQueries;
        private final int totalAccessCount;
        private final double averageAccessCount;

        public QueryStatistics(int totalQueries, int totalAccessCount, double averageAccessCount) {
            this.totalQueries = totalQueries;
            this.totalAccessCount = totalAccessCount;
            this.averageAccessCount = averageAccessCount;
        }

        public int getTotalQueries() {
            return totalQueries;
        }

        public int getTotalAccessCount() {
            return totalAccessCount;
        }

        public double getAverageAccessCount() {
            return averageAccessCount;
        }
    }
}
