package com.droid.bss.application.dto.billing;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Command to ingest a usage record (CDR)
 */
public record IngestUsageRecordCommand(
        @NotNull
        String subscriptionId,

        @NotNull
        String usageType,

        @NotNull
        String usageUnit,

        @NotNull
        BigDecimal usageAmount,

        @NotNull
        LocalDate usageDate,

        @NotNull
        LocalTime usageTime,

        String destinationType,
        String destinationNumber,
        String destinationCountry,
        String networkId,
        String ratePeriod,
        String source,
        String sourceFile
) {
}
