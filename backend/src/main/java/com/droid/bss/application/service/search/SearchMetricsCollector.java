package com.droid.bss.application.service.search;

import com.droid.bss.application.dto.search.AdvancedSearchRequest;
import com.droid.bss.application.dto.search.SearchStatistics;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Search Metrics Collector
 * Collects and tracks search performance metrics
 */
@Component
public class SearchMetricsCollector {

    private final AtomicLong totalSearches = new AtomicLong(0);
    private final AtomicLong successfulSearches = new AtomicLong(0);
    private final AtomicLong failedSearches = new AtomicLong(0);
    private final AtomicLong totalResponseTime = new AtomicLong(0);
    private final AtomicLong totalResults = new AtomicLong(0);

    private final Map<String, AtomicLong> searchByEntityType = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> topSearchQueries = new ConcurrentHashMap<>();
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);

    /**
     * Record successful search
     */
    public void recordSearch(AdvancedSearchRequest request, int resultCount, long responseTimeMs) {
        totalSearches.incrementAndGet();
        successfulSearches.incrementAndGet();
        totalResponseTime.addAndGet(responseTimeMs);
        totalResults.addAndGet(resultCount);

        // Track by entity type
        String entityType = request.getEntityType() != null ? request.getEntityType().name() : "ALL";
        searchByEntityType.computeIfAbsent(entityType, k -> new AtomicLong(0)).incrementAndGet();

        // Track top queries
        if (request.getQuery() != null) {
            topSearchQueries.computeIfAbsent(request.getQuery(), k -> new AtomicLong(0)).incrementAndGet();
        }
    }

    /**
     * Record failed search
     */
    public void recordFailedSearch(AdvancedSearchRequest request) {
        totalSearches.incrementAndGet();
        failedSearches.incrementAndGet();
    }

    /**
     * Record cache hit
     */
    public void recordCacheHit() {
        cacheHits.incrementAndGet();
    }

    /**
     * Record cache miss
     */
    public void recordCacheMiss() {
        cacheMisses.incrementAndGet();
    }

    /**
     * Get search statistics
     */
    public SearchStatistics getStatistics() {
        SearchStatistics stats = new SearchStatistics();

        stats.setTotalSearches(totalSearches.get());
        stats.setSuccessfulSearches(successfulSearches.get());
        stats.setFailedSearches(failedSearches.get());

        long total = totalSearches.get();
        stats.setAverageResponseTimeMs(total > 0 ? (double) totalResponseTime.get() / total : 0.0);

        // Convert entity type map
        Map<String, Long> entityTypeStats = new HashMap<>();
        searchByEntityType.forEach((key, value) -> entityTypeStats.put(key, value.get()));
        stats.setSearchByEntityType(entityTypeStats);

        // Get top 10 queries
        Map<String, Long> topQueries = topSearchQueries.entrySet().stream()
            .sorted(Map.Entry.<String, AtomicLong>comparingByValue().reversed())
            .limit(10)
            .collect(LinkedHashMap::new, (map, entry) -> map.put(entry.getKey(), entry.getValue().get()), Map::putAll);
        stats.setTopSearchQueries(topQueries);

        // Calculate cache hit rate
        long totalCacheRequests = cacheHits.get() + cacheMisses.get();
        stats.setCacheHitRate(totalCacheRequests > 0 ? (double) cacheHits.get() / totalCacheRequests : 0.0);

        stats.setTotalResultsReturned(totalResults.get());

        return stats;
    }
}
