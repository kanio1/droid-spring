package com.droid.bss.infrastructure.graphql.persisted;

import org.junit.jupiter.api.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Persisted Query Tests
 * Tests query persistence, storage, validation, and execution
 */
class PersistedQueryTest {

    @Configuration
    static class TestConfig {
        @Bean
        public PersistedQueryStorage storage() {
            return new InMemoryPersistedQueryStorage();
        }

        @Bean
        public PersistedQueryHashGenerator hashGenerator() {
            return new PersistedQueryHashGenerator();
        }

        @Bean
        public PersistedQueryValidator validator() {
            return new PersistedQueryValidator();
        }

        @Bean
        public PersistedQueryService service() {
            return new PersistedQueryService(storage(), hashGenerator(), validator());
        }
    }

    private PersistedQueryStorage storage;
    private PersistedQueryHashGenerator hashGenerator;
    private PersistedQueryValidator validator;
    private PersistedQueryService service;

    @BeforeEach
    void setUp() {
        storage = new InMemoryPersistedQueryStorage();
        hashGenerator = new PersistedQueryHashGenerator();
        validator = new PersistedQueryValidator();
        service = new PersistedQueryService(storage, hashGenerator, validator);
    }

    @Test
    @DisplayName("Hash generator should create consistent hashes")
    void testHashGeneration() {
        String query = "query { customer(id: \"123\") { name email } }";
        String hash1 = hashGenerator.generateHash(query);
        String hash2 = hashGenerator.generateHash(query);

        assertNotNull(hash1);
        assertNotNull(hash2);
        assertEquals(hash1, hash2);
        assertEquals(16, hash1.length());

        System.out.printf("Generated hash: %s%n", hash1);
    }

    @Test
    @DisplayName("Hash generator should create different hashes for different queries")
    void testHashUniqueness() {
        String query1 = "query { customer(id: \"123\") { name } }";
        String query2 = "query { customer(id: \"456\") { name } }";

        String hash1 = hashGenerator.generateHash(query1);
        String hash2 = hashGenerator.generateHash(query2);

        assertNotEquals(hash1, hash2);
    }

    @Test
    @DisplayName("Query validator should accept valid queries")
    void testValidQueryValidation() {
        String query = "query { customer(id: \"123\") { name email } }";
        var result = validator.validate(query, "CustomerQuery");

        assertTrue(result.isValid(), "Valid query should pass validation");
        assertNull(result.getErrorMessage());
    }

    @Test
    @DisplayName("Query validator should reject invalid queries")
    void testInvalidQueryValidation() {
        // Test query with introspection
        String query = "query { __schema { types { name } } }";
        var result = validator.validate(query, null);

        assertFalse(result.isValid(), "Introspection query should be rejected");
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().contains("disallowed"));
    }

    @Test
    @DisplayName("Query validator should check query size")
    void testQuerySizeValidation() {
        // Create a large query
        StringBuilder largeQuery = new StringBuilder("query { ");
        for (int i = 0; i < 10000; i++) {
            largeQuery.append("field").append(i).append(" ");
        }
        largeQuery.append("}");

        var result = validator.validate(largeQuery.toString(), null);
        assertFalse(result.isValid(), "Oversized query should be rejected");
        assertTrue(result.getErrorMessage().contains("size"));
    }

    @Test
    @DisplayName("Query validator should check operation name")
    void testOperationNameValidation() {
        String query = "query MyQuery { customer(id: \"123\") { name } }";
        var result = validator.validate(query, "DifferentQuery");

        assertFalse(result.isValid(), "Mismatched operation name should be rejected");
        assertTrue(result.getErrorMessage().contains("mismatch"));
    }

    @Test
    @DisplayName("Persisted query service should persist queries")
    void testPersistQuery() {
        String query = "query { customer(id: \"123\") { name email } }";
        String operationName = "CustomerQuery";
        String description = "Get customer by ID";

        var result = service.persistQuery(query, operationName, description);

        assertTrue(result.isSuccess(), "Query should be persisted successfully");
        assertNotNull(result.getHash());
        assertEquals(query, result.getQuery());
        assertEquals(operationName, result.getOperationName());

        System.out.printf("Persisted query with hash: %s%n", result.getHash());
    }

    @Test
    @DisplayName("Persisted query service should not persist duplicate queries")
    void testDuplicateQuery() {
        String query = "query { product(id: \"456\") { name price } }";
        String operationName = "ProductQuery";

        // First persistence should succeed
        var result1 = service.persistQuery(query, operationName, "First description");
        assertTrue(result1.isSuccess());

        // Second persistence should fail
        var result2 = service.persistQuery(query, operationName, "Second description");
        assertFalse(result2.isSuccess(), "Duplicate query should be rejected");
        assertTrue(result2.getErrorMessage().contains("already exists"));
    }

    @Test
    @DisplayName("Persisted query service should execute persisted queries")
    void testExecutePersistedQuery() {
        String query = "query { order(id: \"789\") { id total status } }";
        String operationName = "OrderQuery";

        // Persist the query
        var persistResult = service.persistQuery(query, operationName, "Get order by ID");
        assertTrue(persistResult.isSuccess());

        // Execute the persisted query
        var executeResult = service.executePersistedQuery(persistResult.getHash());

        assertTrue(executeResult.isFound(), "Persisted query should be found");
        assertEquals(query, executeResult.getQuery());
        assertEquals(operationName, executeResult.getOperationName());

        System.out.printf("Executed persisted query: %s%n", executeResult.getOperationName());
    }

    @Test
    @DisplayName("Persisted query service should return not found for invalid hash")
    void testExecuteNonExistentQuery() {
        var result = service.executePersistedQuery("non-existent-hash-12345");

        assertFalse(result.isFound(), "Non-existent query should return not found");
        assertNotNull(result.getErrorMessage());
    }

    @Test
    @DisplayName("Persisted query storage should store and retrieve queries")
    void testStorageOperations() {
        String hash = hashGenerator.generateHash("query { test { field } }");
        String query = "query { test { field } }";
        String operationName = "TestQuery";
        String description = "Test query";

        // Store
        storage.storeQuery(hash, query, operationName, description);

        // Retrieve
        var retrieved = storage.getQuery(hash);
        assertTrue(retrieved.isPresent(), "Query should be stored and retrieved");

        PersistedQuery persistedQuery = retrieved.get();
        assertEquals(hash, persistedQuery.getHash());
        assertEquals(query, persistedQuery.getQuery());
        assertEquals(operationName, persistedQuery.getOperationName());
        assertEquals(description, persistedQuery.getDescription());

        System.out.printf("Stored and retrieved query: %s%n", operationName);
    }

    @Test
    @DisplayName("Persisted query storage should delete queries")
    void testDeleteQuery() {
        String hash = hashGenerator.generateHash("query { delete { field } }");
        String query = "query { delete { field } }";

        storage.storeQuery(hash, query, "DeleteQuery", "Delete query");

        // Verify it exists
        assertTrue(storage.exists(hash));

        // Delete
        boolean deleted = storage.deleteQuery(hash);
        assertTrue(deleted, "Query should be deleted successfully");

        // Verify it's gone
        assertFalse(storage.exists(hash));
        assertFalse(storage.getQuery(hash).isPresent());
    }

    @Test
    @DisplayName("Persisted query service should list all queries")
    void testListQueries() {
        // Persist multiple queries
        service.persistQuery("query { q1 { f1 } }", "Query1", "First query");
        service.persistQuery("query { q2 { f2 } }", "Query2", "Second query");
        service.persistQuery("query { q3 { f3 } }", "Query3", "Third query");

        List<PersistedQuery> queries = service.listQueries();

        assertEquals(3, queries.size(), "Should have 3 persisted queries");
        assertNotNull(queries.get(0).getHash());
        assertNotNull(queries.get(0).getOperationName());

        System.out.printf("Listed %d persisted queries%n", queries.size());
    }

    @Test
    @DisplayName("Persisted query service should provide statistics")
    void testQueryStatistics() {
        // Add some queries
        service.persistQuery("query { q1 { f1 } }", "Query1", "Query 1");
        service.persistQuery("query { q2 { f2 } }", "Query2", "Query 2");

        var stats = service.getStatistics();

        assertEquals(2, stats.getTotalQueries());
        assertTrue(stats.getTotalAccessCount() >= 0);
        assertTrue(stats.getAverageAccessCount() >= 0);

        System.out.printf("Query statistics: %d queries, %d total access, avg: %.2f%n",
            stats.getTotalQueries(),
            stats.getTotalAccessCount(),
            stats.getAverageAccessCount());
    }

    @Test
    @DisplayName("Persisted query service should check query existence")
    void testQueryExistence() {
        String query = "query { existence { test } }";
        var persistResult = service.persistQuery(query, "ExistenceTest", "Test existence");

        assertTrue(service.queryExists(persistResult.getHash()), "Persisted query should exist");
        assertFalse(service.queryExists("non-existent-hash"), "Non-existent query should not exist");
    }

    @Test
    @DisplayName("Persisted query should track access count")
    void testAccessCountTracking() {
        String query = "query { access { test } }";
        var persistResult = service.persistQuery(query, "AccessTest", "Test access");

        String hash = persistResult.getHash();

        // Execute multiple times
        service.executePersistedQuery(hash);
        service.executePersistedQuery(hash);
        service.executePersistedQuery(hash);

        // Get the query and check access count
        var queryResult = service.getQuery(hash);
        assertTrue(queryResult.isFound());
        assertTrue(queryResult.getQuery().getAccessCount() >= 3,
            "Access count should be tracked");

        System.out.printf("Access count: %d%n", queryResult.getQuery().getAccessCount());
    }

    @Nested
    @DisplayName("Hash Generation Tests")
    class HashGenerationTests {

        @Test
        @DisplayName("Should generate consistent hashes across multiple calls")
        void testConsistentHashes() {
            String query = "query { test { field } }";

            for (int i = 0; i < 10; i++) {
                String hash = hashGenerator.generateHash(query);
                assertNotNull(hash);
                assertEquals(16, hash.length());
            }
        }

        @Test
        @DisplayName("Should handle different query formats")
        void testDifferentFormats() {
            String query1 = "query{test{field}}";
            String query2 = "query { test { field } }";

            String hash1 = hashGenerator.generateHash(query1);
            String hash2 = hashGenerator.generateHash(query2);

            // Queries are normalized, so they should have the same hash
            assertEquals(hash1, hash2);
        }
    }

    @Nested
    @DisplayName("Storage Tests")
    class StorageTests {

        @Test
        @DisplayName("Should correctly count queries")
        void testQueryCount() {
            assertEquals(0, storage.count());

            storage.storeQuery("hash1", "query1", "op1", "desc1");
            assertEquals(1, storage.count());

            storage.storeQuery("hash2", "query2", "op2", "desc2");
            assertEquals(2, storage.count());

            storage.clear();
            assertEquals(0, storage.count());
        }
    }
}
