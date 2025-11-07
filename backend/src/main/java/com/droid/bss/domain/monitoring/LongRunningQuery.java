package com.droid.bss.domain.monitoring;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class LongRunningQuery {
    private Long pid;
    private String username;
    private String applicationName;
    private String clientAddress;
    private String state;
    private Instant queryStart;
    private Long durationSeconds;
    private String query;
}
