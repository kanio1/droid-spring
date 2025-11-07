package com.droid.bss.application.dto.search;

import java.util.Map;

/**
 * Search Statistics DTO
 */
public class SearchStatistics {

    private long totalSearches;
    private long successfulSearches;
    private long failedSearches;
    private double averageResponseTimeMs;
    private Map<String, Long> searchByEntityType;
    private Map<String, Long> topSearchQueries;
    private double cacheHitRate;
    private long totalResultsReturned;

    public SearchStatistics() {}

    public long getTotalSearches() {
        return totalSearches;
    }

    public void setTotalSearches(long totalSearches) {
        this.totalSearches = totalSearches;
    }

    public long getSuccessfulSearches() {
        return successfulSearches;
    }

    public void setSuccessfulSearches(long successfulSearches) {
        this.successfulSearches = successfulSearches;
    }

    public long getFailedSearches() {
        return failedSearches;
    }

    public void setFailedSearches(long failedSearches) {
        this.failedSearches = failedSearches;
    }

    public double getAverageResponseTimeMs() {
        return averageResponseTimeMs;
    }

    public void setAverageResponseTimeMs(double averageResponseTimeMs) {
        this.averageResponseTimeMs = averageResponseTimeMs;
    }

    public Map<String, Long> getSearchByEntityType() {
        return searchByEntityType;
    }

    public void setSearchByEntityType(Map<String, Long> searchByEntityType) {
        this.searchByEntityType = searchByEntityType;
    }

    public Map<String, Long> getTopSearchQueries() {
        return topSearchQueries;
    }

    public void setTopSearchQueries(Map<String, Long> topSearchQueries) {
        this.topSearchQueries = topSearchQueries;
    }

    public double getCacheHitRate() {
        return cacheHitRate;
    }

    public void setCacheHitRate(double cacheHitRate) {
        this.cacheHitRate = cacheHitRate;
    }

    public long getTotalResultsReturned() {
        return totalResultsReturned;
    }

    public void setTotalResultsReturned(long totalResultsReturned) {
        this.totalResultsReturned = totalResultsReturned;
    }

    public double getSuccessRate() {
        if (totalSearches == 0) return 0.0;
        return (double) successfulSearches / totalSearches;
    }
}
