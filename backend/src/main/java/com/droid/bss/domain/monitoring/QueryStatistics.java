package com.droid.bss.domain.monitoring;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QueryStatistics {
    private String query;
    private Long calls;
    private Double totalTime;
    private Double meanTime;
    private Double minTime;
    private Double maxTime;
    private Long rows;
    private Double hitPercent;
}
