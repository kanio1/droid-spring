package com.droid.bss.domain.performance;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * Index Recommendation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndexRecommendation {

    private String tableName;
    private String columnName;
    private Integer frequency; // How often this column is filtered
    private String recommendationType; // ADD_INDEX, DROP_INDEX, REBUILD_INDEX
    private String priority; // HIGH, MEDIUM, LOW
    private String sql; // Suggested SQL statement

    public String getSummary() {
        return String.format("%s: Add index on %s.%s (used %d times)",
            priority, tableName, columnName, frequency);
    }

    public boolean isHighPriority() {
        return "HIGH".equals(priority);
    }
}
