package com.droid.bss.application.dto.billing;

import com.droid.bss.domain.billing.UsageRecordEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Response DTO for Usage Record
 */
public record UsageRecordResponse(
        String id,
        String subscriptionId,
        String usageType,
        String usageUnit,
        BigDecimal usageAmount,
        LocalDate usageDate,
        LocalTime usageTime,
        String destinationType,
        String destinationNumber,
        String destinationCountry,
        String networkId,
        String ratePeriod,
        BigDecimal unitRate,
        BigDecimal chargeAmount,
        String currency,
        BigDecimal taxRate,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        Boolean rated,
        LocalDate ratingDate,
        String source,
        String sourceFile,
        Boolean processed,
        String invoiceId
) {
    public static UsageRecordResponse from(UsageRecordEntity entity) {
        return new UsageRecordResponse(
                entity.getId(),
                entity.getSubscription().getId(),
                entity.getUsageType().name(),
                entity.getUsageUnit().name(),
                entity.getUsageAmount(),
                entity.getUsageDate(),
                entity.getUsageTime(),
                entity.getDestinationType() != null ? entity.getDestinationType().name() : null,
                entity.getDestinationNumber(),
                entity.getDestinationCountry(),
                entity.getNetworkId(),
                entity.getRatePeriod() != null ? entity.getRatePeriod().name() : null,
                entity.getUnitRate(),
                entity.getChargeAmount(),
                entity.getCurrency(),
                entity.getTaxRate(),
                entity.getTaxAmount(),
                entity.getTotalAmount(),
                entity.getRated(),
                entity.getRatingDate(),
                entity.getSource() != null ? entity.getSource().name() : null,
                entity.getSourceFile(),
                entity.getProcessed(),
                entity.getInvoice() != null ? entity.getInvoice().getId() : null
        );
    }
}
