package com.droid.bss.application.command.billing;

import com.droid.bss.domain.billing.*;
import com.droid.bss.infrastructure.metrics.BusinessMetrics;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Engine for rating usage records
 */
@Service
public class RatingEngine {

    private final RatingRuleRepository ratingRuleRepository;
    private final UsageRecordRepository usageRecordRepository;
    private final BusinessMetrics businessMetrics;

    public RatingEngine(
            RatingRuleRepository ratingRuleRepository,
            UsageRecordRepository usageRecordRepository,
            BusinessMetrics businessMetrics) {
        this.ratingRuleRepository = ratingRuleRepository;
        this.usageRecordRepository = usageRecordRepository;
        this.businessMetrics = businessMetrics;
    }

    /**
     * Rate a single usage record
     */
    @Transactional
    public UsageRecordEntity rateUsageRecord(UsageRecordEntity usageRecord) {
        Timer.Sample sample = businessMetrics.startUsageRating();

        try {
            // Find applicable rating rules
            List<RatingRuleEntity> rules = ratingRuleRepository.findMatchingRules(
                    usageRecord.getUsageType(),
                    usageRecord.getDestinationType(),
                    usageRecord.getRatePeriod(),
                    usageRecord.getUsageDate()
            );

            if (rules.isEmpty()) {
                throw new RuntimeException("No rating rule found for usage: " + usageRecord.getId());
            }

            // Use first matching rule (in production, would need more sophisticated logic)
            RatingRuleEntity rule = rules.get(0);
            businessMetrics.incrementRatingRuleMatched();

            // Calculate charge
            BigDecimal chargeAmount = calculateCharge(
                    usageRecord.getUsageAmount(),
                    rule.getUnitRate(),
                    rule.getMinimumUnits()
            );

            // Apply to usage record
            usageRecord.setUnitRate(rule.getUnitRate());
            usageRecord.setChargeAmount(chargeAmount);
            usageRecord.setCurrency(rule.getCurrency());
            usageRecord.setRated(true);
            usageRecord.setRatingDate(LocalDate.now());

            // Calculate totals
            usageRecord.calculateTotals();

            UsageRecordEntity saved = usageRecordRepository.save(usageRecord);

            businessMetrics.recordUsageRating(sample);

            return saved;
        } catch (Exception e) {
            businessMetrics.recordUsageRating(sample);
            throw e;
        }
    }

    /**
     * Rate all unrated usage records
     */
    @Transactional
    public List<UsageRecordEntity> rateAllUnrated() {
        List<UsageRecordEntity> unrated = usageRecordRepository.findUnrated();
        return unrated.stream()
                .map(this::rateUsageRecord)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Rate usage records for a specific period
     */
    @Transactional
    public List<UsageRecordEntity> rateUsageByPeriod(String subscriptionId, java.time.LocalDate startDate, java.time.LocalDate endDate) {
        // In a real implementation, would get subscription and query records
        return usageRecordRepository.findUnrated().stream()
                .filter(ur -> ur.getUsageDate().isAfter(startDate.minusDays(1)) &&
                             ur.getUsageDate().isBefore(endDate.plusDays(1)))
                .map(this::rateUsageRecord)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Calculate charge amount based on usage and rate
     */
    private BigDecimal calculateCharge(BigDecimal usageAmount, BigDecimal unitRate, Long minimumUnits) {
        BigDecimal units = new BigDecimal(usageAmount.toString());
        BigDecimal minUnits = new BigDecimal(minimumUnits);

        // Use minimum units if usage is less
        BigDecimal billableUnits = units.compareTo(minUnits) < 0 ? minUnits : units;

        return billableUnits.multiply(unitRate);
    }
}
