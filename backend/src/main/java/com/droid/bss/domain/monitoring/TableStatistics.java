package com.droid.bss.domain.monitoring;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class TableStatistics {
    private String schemaName;
    private String tableName;
    private Long seqScan;
    private Long seqTupRead;
    private Long idxScan;
    private Long idxTupFetch;
    private Long nTupIns;
    private Long nTupUpd;
    private Long nTupDel;
    private Long nLiveTup;
    private Long nDeadTup;
    private Instant lastVacuum;
    private Instant lastAutovacuum;
    private Instant lastAnalyze;
    private Instant lastAutoanalyze;
}
