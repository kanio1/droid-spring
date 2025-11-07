package com.droid.bss.infrastructure.graphql.persisted;

import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Generates hashes for GraphQL queries for persisted query storage
 */
@Slf4j
public class PersistedQueryHashGenerator {

    private static final String ALGORITHM = "SHA-256";
    private static final int HASH_PREFIX_LENGTH = 16;

    /**
     * Generate a hash for a GraphQL query
     */
    public String generateHash(String query) {
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            byte[] hash = digest.digest(query.getBytes(StandardCharsets.UTF_8));
            String base64Hash = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);

            // Use first 16 characters of the base64 hash
            return base64Hash.substring(0, HASH_PREFIX_LENGTH);

        } catch (NoSuchAlgorithmException e) {
            throw new PersistedQueryException("Failed to generate hash: " + e.getMessage(), e);
        }
    }

    /**
     * Generate a detailed hash with metadata
     */
    public PersistedQueryHash generateDetailedHash(String query, String operationName) {
        String normalizedQuery = normalizeQuery(query);
        String hash = generateHash(normalizedQuery);

        return new PersistedQueryHash(
            hash,
            normalizedQuery,
            operationName,
            query.length()
        );
    }

    /**
     * Normalize a GraphQL query for consistent hashing
     */
    private String normalizeQuery(String query) {
        // Remove extra whitespace
        String normalized = query.trim()
            .replaceAll("\\s+", " ");

        // Remove comments
        normalized = normalized.replaceAll("#.*$", "");

        // Normalize operation names (if not present, add default)
        if (!normalized.contains("query ") && !normalized.contains("mutation ") && !normalized.contains("subscription ")) {
            if (normalized.trim().startsWith("{")) {
                normalized = "query " + normalized;
            }
        }

        return normalized;
    }

    /**
     * Record for hash metadata
     */
    public static class PersistedQueryHash {
        private final String hash;
        private final String normalizedQuery;
        private final String operationName;
        private final int queryLength;

        public PersistedQueryHash(String hash, String normalizedQuery, String operationName, int queryLength) {
            this.hash = hash;
            this.normalizedQuery = normalizedQuery;
            this.operationName = operationName;
            this.queryLength = queryLength;
        }

        public String getHash() {
            return hash;
        }

        public String getNormalizedQuery() {
            return normalizedQuery;
        }

        public String getOperationName() {
            return operationName;
        }

        public int getQueryLength() {
            return queryLength;
        }

        @Override
        public String toString() {
            return "PersistedQueryHash{" +
                "hash='" + hash + '\'' +
                ", operationName='" + operationName + '\'' +
                ", queryLength=" + queryLength +
                '}';
        }
    }
}
