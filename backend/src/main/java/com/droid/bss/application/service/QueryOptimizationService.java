package com.droid.bss.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Query optimization and performance monitoring service
 */
@Service
public class QueryOptimizationService {

    private static final Logger logger = LoggerFactory.getLogger(QueryOptimizationService.class);
    private final PerformanceCacheService cacheService;
    private final ConcurrentHashMap<String, QueryMetrics> queryMetrics = new ConcurrentHashMap<>();
    private final AtomicLong totalQueries = new AtomicLong(0);
    private final AtomicLong cachedQueries = new AtomicLong(0);

    public QueryOptimizationService(PerformanceCacheService cacheService) {
        this.cacheService = cacheService;
    }

    public <T> T executeOptimizedQuery(String cacheKey, String queryName, QueryExecutor<T> executor, long cacheTtlSeconds) {
        long startTime = System.nanoTime();
        totalQueries.incrementAndGet();

        @SuppressWarnings("unchecked")
        T result = (T) cacheService.getOrComputeWithTtl(
            "query:" + queryName + ":" + cacheKey,
            Object.class,
            () -> {
                cachedQueries.incrementAndGet();
                return executor.execute();
            },
            cacheTtlSeconds
        );

        long executionTime = System.nanoTime() - startTime;
        updateMetrics(queryName, executionTime, true);

        logger.debug("Query {} executed in {} ms (cached: {})",
            queryName, executionTime / 1_000_000, result != null);

        return result;
    }

    public <T> T executeQueryWithoutCache(String queryName, QueryExecutor<T> executor) {
        long startTime = System.nanoTime();
        totalQueries.incrementAndGet();

        T result = executor.execute();

        long executionTime = System.nanoTime() - startTime;
        updateMetrics(queryName, executionTime, false);

        logger.debug("Query {} executed in {} ms (uncached)",
            queryName, executionTime / 1_000_000);

        return result;
    }

    private void updateMetrics(String queryName, long executionTimeNs, boolean cached) {
        queryMetrics.compute(queryName, (name, metrics) -> {
            if (metrics == null) {
                return new QueryMetrics(queryName, executionTimeNs, cached);
            } else {
                metrics.recordExecution(executionTimeNs, cached);
                return metrics;
            }
        });
    }

    public QueryMetrics getQueryMetrics(String queryName) {
        return queryMetrics.get(queryName);
    }

    public java.util.Map<String, QueryMetrics> getAllQueryMetrics() {
        return new ConcurrentHashMap<>(queryMetrics);
    }

    public PerformanceStats getPerformanceStats() {
        long total = totalQueries.get();
        long cached = cachedQueries.get();
        double cacheHitRatio = total > 0 ? (double) cached / total * 100 : 0;

        return new PerformanceStats(total, cached, cacheHitRatio, queryMetrics.size());
    }

    public void clearMetrics() {
        queryMetrics.clear();
        totalQueries.set(0);
        cachedQueries.set(0);
    }

    public void invalidateQueryCache(String queryName) {
        cacheService.evictPattern("query:" + queryName + ":*");
    }

    public void invalidateAllQueryCaches() {
        cacheService.evictPattern("query:*");
    }

    public java.util.List<String> getSlowQueries(double thresholdMs) {
        return queryMetrics.entrySet().stream()
            .filter(entry -> entry.getValue().getAverageTimeMs() > thresholdMs)
            .map(entry -> entry.getKey() + " - " + String.format("%.2f ms", entry.getValue().getAverageTimeMs()))
            .collect(java.util.stream.Collectors.toList());
    }

    public void warmUpCache(String queryName, String cacheKey, Object data, long ttlSeconds) {
        cacheService.warmUp("query:" + queryName + ":" + cacheKey, data);
    }

    @FunctionalInterface
    public interface QueryExecutor<T> {
        T execute();
    }

    public static class QueryMetrics {
        private final String queryName;
        private final AtomicLong executionCount = new AtomicLong(0);
        private final AtomicLong totalTime = new AtomicLong(0);
        private final AtomicLong minTime = new AtomicLong(Long.MAX_VALUE);
        private final AtomicLong maxTime = new AtomicLong(0);
        private final AtomicLong cachedCount = new AtomicLong(0);

        public QueryMetrics(String queryName, long executionTime, boolean cached) {
            this.queryName = queryName;
            recordExecution(executionTime, cached);
        }

        public void recordExecution(long executionTime, boolean cached) {
            executionCount.incrementAndGet();
            totalTime.addAndGet(executionTime);
            minTime.updateAndGet(current -> Math.min(current, executionTime));
            maxTime.updateAndGet(current -> Math.max(current, executionTime));
            if (cached) {
                cachedCount.incrementAndGet();
            }
        }

        public long getExecutionCount() {
            return executionCount.get();
        }

        public double getAverageTimeMs() {
            long count = executionCount.get();
            return count > 0 ? (totalTime.get() / count) / 1_000_000.0 : 0;
        }

        public double getMinTimeMs() {
            return minTime.get() / 1_000_000.0;
        }

        public double getMaxTimeMs() {
            return maxTime.get() / 1_000_000.0;
        }

        public long getCachedCount() {
            return cachedCount.get();
        }

        public double getCacheHitRatio() {
            long count = executionCount.get();
            return count > 0 ? (double) cachedCount.get() / count * 100 : 0;
        }

        public String getQueryName() {
            return queryName;
        }
    }

    public static class PerformanceStats {
        private final long totalQueries;
        private final long cachedQueries;
        private final double cacheHitRatio;
        private final int trackedQueries;

        public PerformanceStats(long totalQueries, long cachedQueries, double cacheHitRatio, int trackedQueries) {
            this.totalQueries = totalQueries;
            this.cachedQueries = cachedQueries;
            this.cacheHitRatio = cacheHitRatio;
            this.trackedQueries = trackedQueries;
        }

        public long getTotalQueries() {
            return totalQueries;
        }

        public long getCachedQueries() {
            return cachedQueries;
        }

        public double getCacheHitRatio() {
            return cacheHitRatio;
        }

        public int getTrackedQueries() {
            return trackedQueries;
        }
    }
}
