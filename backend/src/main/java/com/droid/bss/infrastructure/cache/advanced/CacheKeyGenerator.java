package com.droid.bss.infrastructure.cache.advanced;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Cache Key Generator
 * Generates consistent cache keys from method parameters
 */
@Slf4j
@Component
public class CacheKeyGenerator {

    private final ObjectMapper objectMapper;
    private static final String SEPARATOR = ":";

    public CacheKeyGenerator() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Generate cache key from class, method, and parameters
     */
    public String generateKey(String className, String methodName, Object... parameters) {
        return generateKey(className, methodName, Arrays.asList(parameters));
    }

    /**
     * Generate cache key from class, method, and parameters
     */
    public String generateKey(String className, String methodName, List<Object> parameters) {
        // Add namespace
        StringBuilder key = new StringBuilder("bss");
        key.append(SEPARATOR).append(className);
        key.append(SEPARATOR).append(methodName);

        // Add parameters
        if (parameters != null && !parameters.isEmpty()) {
            key.append(SEPARATOR).append(generateParameterHash(parameters));
        } else {
            key.append(SEPARATOR).append("no-params");
        }

        return key.toString();
    }

    /**
     * Generate hash from parameters
     */
    private String generateParameterHash(List<Object> parameters) {
        try {
            String paramString = parameters.stream()
                .map(this::serializeParameter)
                .collect(Collectors.joining(SEPARATOR));

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(paramString.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash).substring(0, 12);

        } catch (Exception e) {
            log.warn("Failed to generate parameter hash", e);
            return Integer.toHexString(parameters.hashCode());
        }
    }

    /**
     * Serialize a parameter for hashing
     */
    private String serializeParameter(Object param) {
        if (param == null) {
            return "null";
        }

        // Handle primitives and common types
        if (param instanceof String ||
            param instanceof Number ||
            param instanceof Boolean) {
            return param.toString();
        }

        // Handle collections
        if (param instanceof Collection) {
            return ((Collection<?>) param).stream()
                .map(this::serializeParameter)
                .collect(Collectors.joining(",", "[", "]"));
        }

        // Handle maps
        if (param instanceof Map) {
            return ((Map<?, ?>) param).entrySet().stream()
                .map(entry -> serializeParameter(entry.getKey()) + "=" + serializeParameter(entry.getValue()))
                .collect(Collectors.joining(",", "{", "}"));
        }

        // Handle arrays
        if (param.getClass().isArray()) {
            Object[] array = (Object[]) param;
            return Arrays.stream(array)
                .map(this::serializeParameter)
                .collect(Collectors.joining(",", "[", "]"));
        }

        // For objects, use JSON serialization
        try {
            return objectMapper.writeValueAsString(param);
        } catch (Exception e) {
            log.trace("Failed to serialize parameter: {}", param.getClass().getName(), e);
            return param.getClass().getSimpleName();
        }
    }

    /**
     * Generate pattern for cache invalidation
     */
    public String generatePattern(String className, String methodPrefix) {
        return String.format("bss%s%s%s*", SEPARATOR, className, SEPARATOR, methodPrefix);
    }

    /**
     * Generate key for entity by ID
     */
    public String generateEntityKey(String entityType, String id) {
        return String.format("bss%s%s%s%s", SEPARATOR, entityType, SEPARATOR, id);
    }

    /**
     * Generate key for collection
     */
    public String generateCollectionKey(String entityType, String queryHash) {
        return String.format("bss%s%s%s%s", SEPARATOR, entityType, SEPARATOR, queryHash);
    }

    /**
     * Generate key for aggregated data
     */
    public String generateAggregateKey(String aggregateType, String groupBy, String filterHash) {
        return String.format("bss%s%s%s%s%s%s", SEPARATOR, aggregateType, SEPARATOR, groupBy, SEPARATOR, filterHash);
    }
}
