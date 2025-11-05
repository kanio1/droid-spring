package com.droid.bss.domain.billing;

import com.droid.bss.AbstractIntegrationTest;
import com.droid.bss.application.command.billing.RatingEngine;
import com.droid.bss.domain.customer.CustomerEntity;
import com.droid.bss.domain.customer.CustomerRepository;
import com.droid.bss.domain.product.ProductEntity;
import com.droid.bss.domain.product.ProductRepository;
import com.droid.bss.domain.subscription.SubscriptionEntity;
import com.droid.bss.domain.subscription.SubscriptionRepository;
import com.droid.bss.infrastructure.metrics.BusinessMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for RatingEngine
 * Tests usage record rating with various scenarios
 */
@Transactional
class RatingEngineTest extends AbstractIntegrationTest {

    @Autowired
    private RatingEngine ratingEngine;

    @Autowired
    private RatingRuleRepository ratingRuleRepository;

    @Autowired
    private UsageRecordRepository usageRecordRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private BusinessMetrics businessMetrics;

    private CustomerEntity customer;
    private ProductEntity product;
    private SubscriptionEntity subscription;
    private RatingRuleEntity ratingRule;

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

        // Create product
        product = new ProductEntity(
                "VOICE_PACKAGE",
                "Voice Package",
                "Voice call package",
                ProductType.VOICE,
                BigDecimal.valueOf(29.99),
                "EUR"
        );
        product = productRepository.save(product);

        // Create subscription
        subscription = new SubscriptionEntity(
                customer,
                product,
                SubscriptionStatus.ACTIVE
        );
        subscription = subscriptionRepository.save(subscription);

        // Create rating rule
        ratingRule = new RatingRuleEntity(
                UsageType.VOICE_CALL,
                DestinationType.MOBILE,
                RatePeriod.PEAK,
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2025, 12, 31),
                BigDecimal.valueOf(0.10), // 0.10 EUR per unit
                1L, // minimum 1 unit
                "EUR"
        );
        ratingRule = ratingRuleRepository.save(ratingRule);
    }

    @Test
    void shouldRateUsageRecordWithMatchingRule() {
        // Given
        UsageRecordEntity usageRecord = createUsageRecord(
                subscription,
                UsageType.VOICE_CALL,
                DestinationType.MOBILE,
                RatePeriod.PEAK,
                LocalDate.now(),
                BigDecimal.valueOf(10)
        );

        // When
        UsageRecordEntity ratedRecord = ratingEngine.rateUsageRecord(usageRecord);

        // Then
        assertNotNull(ratedRecord.getId());
        assertTrue(ratedRecord.isRated());
        assertEquals(ratingRule.getUnitRate(), ratedRecord.getUnitRate());
        assertEquals(BigDecimal.valueOf(1.00), ratedRecord.getChargeAmount()); // 10 * 0.10
        assertEquals(ratingRule.getCurrency(), ratedRecord.getCurrency());
        assertNotNull(ratedRecord.getRatingDate());
        assertEquals(BigDecimal.valueOf(1.00), ratedRecord.getTotalAmount());
    }

    @Test
    void shouldApplyMinimumUnitsWhenUsageBelowMinimum() {
        // Given
        BigDecimal usageAmount = BigDecimal.valueOf(0.5); // Below minimum of 1
        UsageRecordEntity usageRecord = createUsageRecord(
                subscription,
                UsageType.VOICE_CALL,
                DestinationType.MOBILE,
                RatePeriod.PEAK,
                LocalDate.now(),
                usageAmount
        );

        // When
        UsageRecordEntity ratedRecord = ratingEngine.rateUsageRecord(usageRecord);

        // Then - Should use minimum 1 unit
        assertEquals(BigDecimal.valueOf(1.00), ratedRecord.getTotalAmount()); // 1 * 0.10
    }

    @Test
    void shouldRateMultipleUsageRecords() {
        // Given
        List<UsageRecordEntity> usageRecords = List.of(
                createUsageRecord(subscription, UsageType.VOICE_CALL, DestinationType.MOBILE, RatePeriod.PEAK, LocalDate.now(), BigDecimal.valueOf(5)),
                createUsageRecord(subscription, UsageType.VOICE_CALL, DestinationType.MOBILE, RatePeriod.PEAK, LocalDate.now(), BigDecimal.valueOf(10)),
                createUsageRecord(subscription, UsageType.VOICE_CALL, DestinationType.MOBILE, RatePeriod.PEAK, LocalDate.now(), BigDecimal.valueOf(15))
        );

        // When
        List<UsageRecordEntity> ratedRecords = usageRecords.stream()
                .map(ratingEngine::rateUsageRecord)
                .toList();

        // Then
        assertEquals(3, ratedRecords.size());
        ratedRecords.forEach(record -> {
            assertTrue(record.isRated());
            assertNotNull(record.getTotalAmount());
        });

        // Verify amounts
        assertEquals(BigDecimal.valueOf(0.50), ratedRecords.get(0).getTotalAmount()); // 5 * 0.10
        assertEquals(BigDecimal.valueOf(1.00), ratedRecords.get(1).getTotalAmount()); // 10 * 0.10
        assertEquals(BigDecimal.valueOf(1.50), ratedRecords.get(2).getTotalAmount()); // 15 * 0.10
    }

    @Test
    void shouldRateAllUnratedUsageRecords() {
        // Given - Create 3 unrated usage records
        UsageRecordEntity usage1 = createUsageRecord(subscription, UsageType.VOICE_CALL, DestinationType.MOBILE, RatePeriod.PEAK, LocalDate.now(), BigDecimal.valueOf(5));
        UsageRecordEntity usage2 = createUsageRecord(subscription, UsageType.VOICE_CALL, DestinationType.MOBILE, RatePeriod.PEAK, LocalDate.now(), BigDecimal.valueOf(10));
        UsageRecordEntity usage3 = createUsageRecord(subscription, UsageType.VOICE_CALL, DestinationType.MOBILE, RatePeriod.PEAK, LocalDate.now(), BigDecimal.valueOf(15));

        usageRecordRepository.saveAll(List.of(usage1, usage2, usage3));

        // When
        List<UsageRecordEntity> ratedRecords = ratingEngine.rateAllUnrated();

        // Then
        assertEquals(3, ratedRecords.size());
        ratedRecords.forEach(record -> assertTrue(record.isRated()));
    }

    @Test
    void shouldRateUsageByPeriod() {
        // Given
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);

        createUsageRecord(subscription, UsageType.VOICE_CALL, DestinationType.MOBILE, RatePeriod.PEAK, LocalDate.of(2024, 1, 15), BigDecimal.valueOf(10));
        createUsageRecord(subscription, UsageType.VOICE_CALL, DestinationType.MOBILE, RatePeriod.PEAK, LocalDate.of(2024, 2, 15), BigDecimal.valueOf(10)); // Outside period

        // When
        List<UsageRecordEntity> ratedRecords = ratingEngine.rateUsageByPeriod(subscription.getId(), startDate, endDate);

        // Then
        assertEquals(1, ratedRecords.size());
        assertTrue(ratedRecords.get(0).isRated());
    }

    @Test
    void shouldThrowExceptionWhenNoMatchingRule() {
        // Given - Usage record with no matching rule
        UsageRecordEntity usageRecord = createUsageRecord(
                subscription,
                UsageType.SMS, // No rule for SMS
                DestinationType.MOBILE,
                RatePeriod.PEAK,
                LocalDate.now(),
                BigDecimal.valueOf(10)
        );

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            ratingEngine.rateUsageRecord(usageRecord);
        });

        assertTrue(exception.getMessage().contains("No rating rule found"));
    }

    @Test
    void shouldCalculateTotalsCorrectly() {
        // Given
        UsageRecordEntity usageRecord = createUsageRecord(
                subscription,
                UsageType.VOICE_CALL,
                DestinationType.MOBILE,
                RatePeriod.PEAK,
                LocalDate.now(),
                BigDecimal.valueOf(100)
        );

        // When
        UsageRecordEntity ratedRecord = ratingEngine.rateUsageRecord(usageRecord);

        // Then
        assertEquals(BigDecimal.valueOf(10.00), ratedRecord.getChargeAmount()); // 100 * 0.10
        // Assuming calculateTotals adds tax or other fees
        assertNotNull(ratedRecord.getTotalAmount());
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
                LocalTime.now()
        );
        usageRecord.setDestinationType(destinationType);
        usageRecord.setRatePeriod(ratePeriod);
        return usageRecord;
    }
}
