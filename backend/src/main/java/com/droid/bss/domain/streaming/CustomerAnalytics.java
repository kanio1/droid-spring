package com.droid.bss.domain.streaming;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

/**
 * Customer analytics from materialized view
 */
@Data
@Builder
public class CustomerAnalytics {
    private UUID customerId;
    private String email;
    private Instant createdAt;
    private Long totalOrders;
    private Long totalInvoices;
    private Long activeSubscriptions;
    private Double totalRevenue;
    private Instant updatedAt;
}
