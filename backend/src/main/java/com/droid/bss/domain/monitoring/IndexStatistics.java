package com.droid.bss.domain.monitoring;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class IndexStatistics {
    private String schemaName;
    private String tableName;
    private String indexName;
    private Long idxScan;
    private Long idxTupRead;
    private Long idxTupFetch;
}
