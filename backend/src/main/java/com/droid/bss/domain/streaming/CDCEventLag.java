package com.droid.bss.domain.streaming;

import lombok.Builder;
import lombok.Data;

/**
 * CDC Event Lag information
 */
@Data
@Builder
public class CDCEventLag {

    private String consumerGroup;
    private String tableName;
    private Long totalEvents;
    private Long pendingEvents;
    private Long maxOperationId;
    private Long lagCount;
    private String maxLagDuration;
}
