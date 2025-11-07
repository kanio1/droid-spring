package com.droid.bss.domain.streaming;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Replication health status
 */
@Data
@Builder
public class ReplicationHealth {
    private String component;
    private String name;
    private String status;
    private Instant checkTime;
}
