package com.droid.bss.domain.streaming;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Revenue analytics from materialized view
 */
@Data
@Builder
public class RevenueAnalytics {
    private Instant revenueDate;
    private Long invoiceCount;
    private Long uniqueCustomers;
    private Double dailyRevenue;
    private Double avgInvoiceAmount;
    private Double maxInvoiceAmount;
}
