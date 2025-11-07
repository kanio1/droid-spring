package com.droid.bss.domain.monitoring;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class PerformanceAlert {
    private String severity;
    private String type;
    private String message;
    private Long count;
    private Instant timestamp;
}
