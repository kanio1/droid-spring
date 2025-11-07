package com.droid.bss.domain.performance;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Query Performance Metrics
 * Represents performance statistics for SQL queries from pg_stat_statements
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryPerformanceMetrics {

    private Long queryId;
    private String query;
    private Long calls;
    private BigDecimal totalExecTime;
    private BigDecimal meanExecTime;
    private BigDecimal minExecTime;
    private BigDecimal maxExecTime;
    private BigDecimal stddevExecTime;
    private Long rows;
    private BigDecimal hitPercent;

    // Performance grade (A-F) based on execution time
    public String getPerformanceGrade() {
        if (meanExecTime == null) return "N/A";

        double meanMs = meanExecTime.divide(new BigDecimal(1000), 3, RoundingMode.HALF_UP).doubleValue();

        if (meanMs < 10) return "A";
        if (meanMs < 50) return "B";
        if (meanMs < 100) return "C";
        if (meanMs < 500) return "D";
        if (meanMs < 1000) return "E";
        return "F";
    }

    // Normalized query (removes values for grouping)
    public String getNormalizedQuery() {
        if (query == null) return "";
        return query
            .replaceAll("'[^']*'", "''")  // Replace strings
            .replaceAll("\\b\\d+\\b", "?")  // Replace numbers
            .replaceAll("::[\\w\\s]+", "::type")  // Replace type casts
            .trim();
    }

    // Executive summary
    public String getSummary() {
        return String.format("Query called %d times with avg %.2fms - Grade: %s",
            calls != null ? calls : 0,
            meanExecTime != null ? meanExecTime.divide(new BigDecimal(1000), 2, RoundingMode.HALF_UP).doubleValue() : 0,
            getPerformanceGrade());
    }
}
