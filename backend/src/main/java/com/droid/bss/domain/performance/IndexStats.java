package com.droid.bss.domain.performance;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * Index Usage Statistics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndexStats {

    private String schemaName;
    private String tableName;
    private String indexName;
    private Long indexScans;
    private Long tuplesRead;
    private Long tuplesFetched;
    private Double avgTuplesFetched;

    // Usage percentage (0-100)
    public Double getUsagePercent() {
        if (indexScans == null || indexScans == 0) return 0.0;
        return Math.min(100.0, (avgTuplesFetched != null ? avgTuplesFetched : 0.0) * 10);
    }

    public String getSummary() {
        return String.format("Index %s on %s: %d scans, %.2f avg rows",
            indexName, tableName,
            indexScans != null ? indexScans : 0,
            avgTuplesFetched != null ? avgTuplesFetched : 0.0);
    }
}
