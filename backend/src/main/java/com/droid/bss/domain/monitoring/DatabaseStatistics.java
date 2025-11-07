package com.droid.bss.domain.monitoring;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DatabaseStatistics {
    private Long totalConnections;
    private Long activeConnections;
    private Long idleConnections;
    private Long totalCommits;
    private Long totalRollbacks;
    private Long blksRead;
    private Long blksHit;
    private Long tupReturned;
    private Long tupFetched;
    private Long tupInserted;
    private Long tupUpdated;
    private Long tupDeleted;
    private Double cacheHitRatio;
}
