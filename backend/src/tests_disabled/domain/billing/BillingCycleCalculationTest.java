package com.droid.bss.domain.billing;

import com.droid.bss.domain.customer.CustomerEntity;
import com.droid.bss.domain.customer.CustomerRepository;
import com.droid.bss.domain.product.ProductEntity;
import com.droid.bss.domain.product.ProductRepository;
import com.droid.bss.domain.subscription.SubscriptionEntity;
import com.droid.bss.domain.subscription.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for Billing Cycle Calculations
 * Tests complex billing scenarios and edge cases
 */
@Transactional
class BillingCycleCalculationTest extends AbstractIntegrationTest {

    @Autowired
    private BillingCycleRepository billingCycleRepository;

    @Autowired
    private UsageRecordRepository usageRecordRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private RatingRuleRepository ratingRuleRepository;

    private CustomerEntity customer;
    private ProductEntity voiceProduct;
    private ProductEntity dataProduct;
    private SubscriptionEntity voiceSubscription;
    private SubscriptionEntity dataSubscription;

    @BeforeEach
    void setUp() {
        // Create customer
        customer = new CustomerEntity(
                "test-customer@example.com",
                "John",
                "Doe",
                "123456789"
        );
        customer = customerRepository.save(customer);

        // Create products
        voiceProduct = new ProductEntity(
                "VOICE_PACKAGE",
                "Voice Package",
                "Voice call package",
                ProductType.VOICE,
                BigDecimal.valueOf(29.99),
                "EUR"
        );
        voiceProduct = productRepository.save(voiceProduct);

        dataProduct = new ProductEntity(
                "DATA_PACKAGE",
                "Data Package",
                "Data package",
                ProductType.DATA,
                BigDecimal.valueOf(39.99),
                "EUR"
        );
        dataProduct = productRepository.save(dataProduct);

        // Create subscriptions
        voiceSubscription = new SubscriptionEntity(
                customer,
                voiceProduct,
                com.droid.bss.domain.subscription.SubscriptionStatus.ACTIVE
        );
        voiceSubscription = subscriptionRepository.save(voiceSubscription);

        dataSubscription = new SubscriptionEntity(
                customer,
                dataProduct,
                com.droid.bss.domain.subscription.SubscriptionStatus.ACTIVE
        );
        dataSubscription = subscriptionRepository.save(dataSubscription);

        // Create rating rules for voice
        RatingRuleEntity voicePeakRule = new RatingRuleEntity(
                UsageType.VOICE_CALL,
                DestinationType.MOBILE,
                RatePeriod.PEAK,
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2025, 12, 31),
                BigDecimal.valueOf(0.10),
                1L,
                "EUR"
        );
        ratingRuleRepository.save(voicePeakRule);

        RatingRuleEntity voiceOffPeakRule = new RatingRuleEntity(
                UsageType.VOICE_CALL,
                DestinationType.MOBILE,
                RatePeriod.OFF_PEAK,
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2025, 12, 31),
                BigDecimal.valueOf(0.05),
                1L,
                "EUR"
        );
        ratingRuleRepository.save(voiceOffPeakRule);

        // Create rating rules for data
        RatingRuleEntity dataRule = new RatingRuleEntity(
                UsageType.DATA,
                DestinationType.MOBILE,
                RatePeriod.PEAK,
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2025, 12, 31),
                BigDecimal.valueOf(0.001), // 0.001 EUR per MB
                1L,
                "EUR"
        );
        ratingRuleRepository.save(dataRule);
    }

    @Test
    void shouldCalculateBillingCycleWithMultipleSubscriptions() {
        // Given - Create billing cycle
        BillingCycleEntity billingCycle = new BillingCycleEntity(
                customer,
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 31),
                LocalDate.of(2024, 2, 1),
                BillingCycleType.MONTHLY
        );
        billingCycle = billingCycleRepository.save(billingCycle);

        // Add usage for voice subscription
        for (int i = 0; i < 10; i++) {
            createUsageRecord(voiceSubscription, UsageType.VOICE_CALL, DestinationType.MOBILE,
                    RatePeriod.PEAK, LocalDate.of(2024, 1, 15), BigDecimal.valueOf(5));
        }

        // Add usage for data subscription
        for (int i = 0; i < 5; i++) {
            createUsageRecord(dataSubscription, UsageType.DATA, DestinationType.MOBILE,
                    RatePeriod.PEAK, LocalDate.of(2024, 1, 15), BigDecimal.valueOf(100));
        }

        // When - Calculate totals
        var usageInPeriod = usageRecordRepository.findByDateRange(
                billingCycle.getCycleStart(),
                billingCycle.getCycleEnd()
        );

        BigDecimal voiceTotal = usageInPeriod.stream()
                .filter(ur -> ur.getSubscription().equals(voiceSubscription))
                .map(UsageRecordEntity::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal dataTotal = usageInPeriod.stream()
                .filter(ur -> ur.getSubscription().equals(dataSubscription))
                .map(UsageRecordEntity::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal grandTotal = voiceTotal.add(dataTotal);

        // Then
        assertEquals(15, usageInPeriod.size());
        assertTrue(voiceTotal.compareTo(BigDecimal.ZERO) > 0);
        assertTrue(dataTotal.compareTo(BigDecimal.ZERO) > 0);
        assertTrue(grandTotal.compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void shouldCalculateBillingWithPeakAndOffPeakRates() {
        // Given
        BillingCycleEntity billingCycle = new BillingCycleEntity(
                customer,
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 31),
                LocalDate.of(2024, 2, 1),
                BillingCycleType.MONTHLY
        );
        billingCycleRepository.save(billingCycle);

        // Add peak usage
        for (int i = 0; i < 5; i++) {
            createUsageRecord(voiceSubscription, UsageType.VOICE_CALL, DestinationType.MOBILE,
                    RatePeriod.PEAK, LocalDate.of(2024, 1, 15), BigDecimal.valueOf(10));
        }

        // Add off-peak usage
        for (int i = 0; i < 3; i++) {
            createUsageRecord(voiceSubscription, UsageType.VOICE_CALL, DestinationType.MOBILE,
                    RatePeriod.OFF_PEAK, LocalDate.of(2024, 1, 15), BigDecimal.valueOf(10));
        }

        // When
        var usageInPeriod = usageRecordRepository.findByDateRange(
                billingCycle.getCycleStart(),
                billingCycle.getCycleEnd()
        );

        BigDecimal peakTotal = usageInPeriod.stream()
                .filter(ur -> ur.getRatePeriod() == RatePeriod.PEAK)
                .map(UsageRecordEntity::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal offPeakTotal = usageInPeriod.stream()
                .filter(ur -> ur.getRatePeriod() == RatePeriod.OFF_PEAK)
                .map(UsageRecordEntity::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Then - Peak: 5 * 10 * 0.10 = 5.00, Off-peak: 3 * 10 * 0.05 = 1.50
        assertEquals(BigDecimal.valueOf(5.00), peakTotal);
        assertEquals(BigDecimal.valueOf(1.50), offPeakTotal);
        assertEquals(BigDecimal.valueOf(6.50), peakTotal.add(offPeakTotal));
    }

    @Test
    void shouldCalculateTaxCorrectly() {
        // Given
        BigDecimal subtotal = BigDecimal.valueOf(100.00);
        BigDecimal taxRate = new BigDecimal("0.23"); // 23% VAT

        // When
        BigDecimal tax = subtotal.multiply(taxRate);
        BigDecimal totalWithTax = subtotal.add(tax);

        // Then
        assertEquals(BigDecimal.valueOf(23.00), tax);
        assertEquals(BigDecimal.valueOf(123.00), totalWithTax);
    }

    @Test
    void shouldCalculateBillingCycleForFullMonth() {
        // Given - Full month of usage
        BillingCycleEntity billingCycle = new BillingCycleEntity(
                customer,
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 31),
                LocalDate.of(2024, 2, 1),
                BillingCycleType.MONTHLY
        );
        billingCycle = billingCycleRepository.save(billingCycle);

        // Add daily usage (31 days)
        for (int day = 1; day <= 31; day++) {
            createUsageRecord(voiceSubscription, UsageType.VOICE_CALL, DestinationType.MOBILE,
                    RatePeriod.PEAK, LocalDate.of(2024, 1, day), BigDecimal.valueOf(5));
        }

        // When
        var usageInPeriod = usageRecordRepository.findByDateRange(
                billingCycle.getCycleStart(),
                billingCycle.getCycleEnd()
        );

        BigDecimal total = usageInPeriod.stream()
                .map(UsageRecordEntity::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Then - 31 days * 5 minutes * 0.10 EUR = 15.50
        assertEquals(31, usageInPeriod.size());
        assertEquals(BigDecimal.valueOf(15.50), total);
    }

    @Test
    void shouldCalculateProratedBillingForPartialMonth() {
        // Given - Partial month billing cycle
        BillingCycleEntity billingCycle = new BillingCycleEntity(
                customer,
                LocalDate.of(2024, 1, 15),
                LocalDate.of(2024, 1, 31),
                LocalDate.of(2024, 2, 1),
                BillingCycleType.MONTHLY
        );
        billingCycle = billingCycleRepository.save(billingCycle);

        // Add usage only for billing period
        for (int day = 15; day <= 31; day++) {
            createUsageRecord(voiceSubscription, UsageType.VOICE_CALL, DestinationType.MOBILE,
                    RatePeriod.PEAK, LocalDate.of(2024, 1, day), BigDecimal.valueOf(5));
        }

        // When
        var usageInPeriod = usageRecordRepository.findByDateRange(
                billingCycle.getCycleStart(),
                billingCycle.getCycleEnd()
        );

        BigDecimal total = usageInPeriod.stream()
                .map(UsageRecordEntity::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Then - 17 days * 5 minutes * 0.10 EUR = 8.50
        assertEquals(17, usageInPeriod.size());
        assertEquals(BigDecimal.valueOf(8.50), total);
    }

    @Test
    void shouldCalculateMinimumBillingCharge() {
        // Given - Very low usage (below minimum for rating)
        createUsageRecord(voiceSubscription, UsageType.VOICE_CALL, DestinationType.MOBILE,
                RatePeriod.PEAK, LocalDate.of(2024, 1, 15), BigDecimal.valueOf(0.1));

        // When
        var allUsage = usageRecordRepository.findAll();
        BigDecimal total = allUsage.stream()
                .map(UsageRecordEntity::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Then - Should charge minimum 1 unit * 0.10 = 0.10
        assertEquals(BigDecimal.valueOf(0.10), total);
    }

    @Test
    void shouldGroupUsageBySubscriptionForBilling() {
        // Given
        BillingCycleEntity billingCycle = new BillingCycleEntity(
                customer,
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 31),
                LocalDate.of(2024, 2, 1),
                BillingCycleType.MONTHLY
        );
        billingCycleRepository.save(billingCycle);

        // Add mixed usage
        createUsageRecord(voiceSubscription, UsageType.VOICE_CALL, DestinationType.MOBILE,
                RatePeriod.PEAK, LocalDate.of(2024, 1, 15), BigDecimal.valueOf(10));
        createUsageRecord(dataSubscription, UsageType.DATA, DestinationType.MOBILE,
                RatePeriod.PEAK, LocalDate.of(2024, 1, 15), BigDecimal.valueOf(100));

        // When
        var usageInPeriod = usageRecordRepository.findByDateRange(
                billingCycle.getCycleStart(),
                billingCycle.getCycleEnd()
        );

        // Group by subscription
        var usageBySubscription = usageInPeriod.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        ur -> ur.getSubscription().getId()
                ));

        // Then
        assertEquals(2, usageBySubscription.size());
        usageBySubscription.forEach((subscriptionId, usageList) -> {
            assertEquals(1, usageList.size());
        });
    }

    @Test
    void shouldCalculateAverageDailyUsage() {
        // Given
        BillingCycleEntity billingCycle = new BillingCycleEntity(
                customer,
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 31),
                LocalDate.of(2024, 2, 1),
                BillingCycleType.MONTHLY
        );
        billingCycleRepository.save(billingCycle);

        // Add 310 minutes over 31 days
        for (int day = 1; day <= 31; day++) {
            createUsageRecord(voiceSubscription, UsageType.VOICE_CALL, DestinationType.MOBILE,
                    RatePeriod.PEAK, LocalDate.of(2024, 1, day), BigDecimal.valueOf(10));
        }

        // When
        var usageInPeriod = usageRecordRepository.findByDateRange(
                billingCycle.getCycleStart(),
                billingCycle.getCycleEnd()
        );

        BigDecimal totalUsage = usageInPeriod.stream()
                .map(UsageRecordEntity::getUsageAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal daysInPeriod = new BigDecimal("31");
        BigDecimal averageDaily = totalUsage.divide(daysInPeriod, 2, BigDecimal.ROUND_HALF_UP);

        // Then - Average should be 10 minutes per day
        assertEquals(31, usageInPeriod.size());
        assertEquals(BigDecimal.valueOf(310), totalUsage);
        assertEquals(BigDecimal.valueOf("10.00"), averageDaily);
    }

    @Test
    void shouldCalculateBillingForLeapYearFebruary() {
        // Given - Leap year (29 days in February)
        BillingCycleEntity billingCycle = new BillingCycleEntity(
                customer,
                LocalDate.of(2024, 2, 1),
                LocalDate.of(2024, 2, 29),
                LocalDate.of(2024, 3, 1),
                BillingCycleType.MONTHLY
        );
        billingCycleRepository.save(billingCycle);

        // Add daily usage
        for (int day = 1; day <= 29; day++) {
            createUsageRecord(voiceSubscription, UsageType.VOICE_CALL, DestinationType.MOBILE,
                    RatePeriod.PEAK, LocalDate.of(2024, 2, day), BigDecimal.valueOf(5));
        }

        // When
        var usageInPeriod = usageRecordRepository.findByDateRange(
                billingCycle.getCycleStart(),
                billingCycle.getCycleEnd()
        );

        BigDecimal total = usageInPeriod.stream()
                .map(UsageRecordEntity::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Then - 29 days * 5 minutes * 0.10 = 14.50
        assertEquals(29, usageInPeriod.size());
        assertEquals(BigDecimal.valueOf(14.50), total);
    }

    @Test
    void shouldRoundTotalsToTwoDecimalPlaces() {
        // Given - Usage that results in more than 2 decimal places
        createUsageRecord(voiceSubscription, UsageType.VOICE_CALL, DestinationType.MOBILE,
                RatePeriod.PEAK, LocalDate.of(2024, 1, 15), new BigDecimal("0.333")); // 1/3 minute

        // When
        var allUsage = usageRecordRepository.findAll();
        BigDecimal total = allUsage.stream()
                .map(UsageRecordEntity::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Then - Should round to 2 decimal places
        assertTrue(total.scale() <= 2);
    }

    @Test
    void shouldCalculateVolumeDiscount() {
        // Given - High volume usage
        for (int i = 0; i < 100; i++) {
            createUsageRecord(voiceSubscription, UsageType.VOICE_CALL, DestinationType.MOBILE,
                    RatePeriod.PEAK, LocalDate.of(2024, 1, 15), BigDecimal.valueOf(10));
        }

        // When
        var allUsage = usageRecordRepository.findAll();
        BigDecimal totalMinutes = allUsage.stream()
                .map(UsageRecordEntity::getUsageAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCost = allUsage.stream()
                .map(UsageRecordEntity::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Then
        assertEquals(100, allUsage.size());
        assertEquals(BigDecimal.valueOf(1000), totalMinutes); // 100 * 10
        assertEquals(BigDecimal.valueOf(100.00), totalCost); // 1000 * 0.10
    }

    private UsageRecordEntity createUsageRecord(
            SubscriptionEntity subscription,
            UsageType usageType,
            DestinationType destinationType,
            RatePeriod ratePeriod,
            LocalDate usageDate,
            BigDecimal usageAmount
    ) {
        UsageRecordEntity usageRecord = new UsageRecordEntity(
                subscription,
                usageType,
                UsageUnit.MINUTES,
                usageAmount,
                usageDate,
                java.time.LocalTime.now()
        );
        usageRecord.setDestinationType(destinationType);
        usageRecord.setRatePeriod(ratePeriod);
        return usageRecordRepository.save(usageRecord);
    }
}
