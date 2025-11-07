package com.droid.bss.domain.monitoring;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class SystemMetrics {
    private Long connections;
    private Double maxQueryDuration;
    private Double avgQueryDuration;
    private Long waitingLocks;
    private Long idleInTransaction;
    private Instant timestamp;
}
