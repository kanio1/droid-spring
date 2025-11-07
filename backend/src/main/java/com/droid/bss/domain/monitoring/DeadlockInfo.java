package com.droid.bss.domain.monitoring;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class DeadlockInfo {
    private Long pid;
    private String database;
    private String username;
    private String applicationName;
    private String clientAddress;
    private Instant queryStart;
    private String state;
    private String query;
}
