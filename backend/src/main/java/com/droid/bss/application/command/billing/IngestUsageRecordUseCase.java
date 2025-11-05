package com.droid.bss.application.command.billing;

import com.droid.bss.application.dto.billing.IngestUsageRecordCommand;
import com.droid.bss.application.dto.billing.UsageRecordResponse;
import com.droid.bss.domain.billing.*;
import com.droid.bss.domain.subscription.SubscriptionEntity;
import com.droid.bss.domain.subscription.SubscriptionRepository;
import com.droid.bss.infrastructure.metrics.BusinessMetrics;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Use case for ingesting usage records (CDRs)
 */
@Service
public class IngestUsageRecordUseCase {

    private final UsageRecordRepository usageRecordRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final RatingEngine ratingEngine;
    private final BusinessMetrics businessMetrics;

    public IngestUsageRecordUseCase(
            UsageRecordRepository usageRecordRepository,
            SubscriptionRepository subscriptionRepository,
            RatingEngine ratingEngine,
            BusinessMetrics businessMetrics) {
        this.usageRecordRepository = usageRecordRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.ratingEngine = ratingEngine;
        this.businessMetrics = businessMetrics;
    }

    @Transactional
    public UsageRecordResponse handle(IngestUsageRecordCommand command) {
        // Get subscription
        SubscriptionEntity subscription = subscriptionRepository.findById(command.subscriptionId())
                .orElseThrow(() -> new RuntimeException("Subscription not found: " + command.subscriptionId()));

        // Create usage record
        UsageRecordEntity usageRecord = new UsageRecordEntity(
                subscription,
                UsageType.valueOf(command.usageType()),
                UsageUnit.valueOf(command.usageUnit()),
                command.usageAmount(),
                command.usageDate(),
                command.usageTime()
        );

        // Set optional fields
        if (command.destinationType() != null) {
            usageRecord.setDestinationType(DestinationType.valueOf(command.destinationType()));
        }
        usageRecord.setDestinationNumber(command.destinationNumber());
        usageRecord.setDestinationCountry(command.destinationCountry());
        usageRecord.setNetworkId(command.networkId());

        if (command.ratePeriod() != null) {
            usageRecord.setRatePeriod(RatePeriod.valueOf(command.ratePeriod()));
        }

        if (command.source() != null) {
            usageRecord.setSource(UsageSource.valueOf(command.source()));
        }
        usageRecord.setSourceFile(command.sourceFile());

        // Save
        UsageRecordEntity saved = usageRecordRepository.save(usageRecord);

        // Track metrics
        businessMetrics.incrementUsageRecordIngested();

        // Auto-rate if configured
        try {
            ratingEngine.rateUsageRecord(saved);
            businessMetrics.incrementUsageRecordRated();
        } catch (Exception e) {
            // Log error but don't fail ingestion
            System.err.println("Failed to rate usage record: " + e.getMessage());
        }

        return UsageRecordResponse.from(saved);
    }
}
