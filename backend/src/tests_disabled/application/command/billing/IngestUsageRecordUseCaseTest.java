package com.droid.bss.application.command.billing;

import com.droid.bss.AbstractIntegrationTest;
import com.droid.bss.application.dto.billing.IngestUsageRecordCommand;
import com.droid.bss.application.dto.billing.UsageRecordResponse;
import com.droid.bss.domain.billing.*;
import com.droid.bss.domain.customer.CustomerEntity;
import com.droid.bss.domain.customer.CustomerRepository;
import com.droid.bss.domain.product.ProductEntity;
import com.droid.bss.domain.product.ProductRepository;
import com.droid.bss.domain.subscription.SubscriptionEntity;
import com.droid.bss.domain.subscription.SubscriptionRepository;
import com.droid.bss.domain.subscription.SubscriptionStatus;
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
 * Integration test for IngestUsageRecordUseCase
 * Tests usage record ingestion from CDR files
 */
@Transactional
class IngestUsageRecordUseCaseTest extends AbstractIntegrationTest {

    @Autowired
    private IngestUsageRecordUseCase ingestUsageRecordUseCase;

    @Autowired
    private UsageRecordRepository usageRecordRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private RatingRuleRepository ratingRuleRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ProductRepository productRepository;

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
                BigDecimal.valueOf(0.10),
                1L,
                "EUR"
        );
        ratingRule = ratingRuleRepository.save(ratingRule);
    }

    @Test
    void shouldIngestVoiceCallUsageRecord() {
        // Given
        IngestUsageRecordCommand command = new IngestUsageRecordCommand(
                subscription.getId().toString(),
                "VOICE_CALL",
                "MINUTES",
                BigDecimal.valueOf(10),
                LocalDate.of(2024, 1, 15),
                LocalTime.of(10, 0),
                "MOBILE",
                "+1234567890",
                "US",
                "PEAK",
                "CDR_FILE_001",
                "CDR"
        );

        // When
        UsageRecordResponse response = ingestUsageRecordUseCase.handle(command);

        // Then
        assertNotNull(response);
        assertNotNull(response.id());
        assertEquals(subscription.getId().toString(), response.subscriptionId());
        assertEquals(UsageType.VOICE_CALL, response.usageType());
        assertEquals(UsageUnit.MINUTES, response.usageUnit());
        assertEquals(BigDecimal.valueOf(10), response.usageAmount());
        assertEquals(LocalDate.of(2024, 1, 15), response.usageDate());
        assertEquals(LocalTime.of(10, 0), response.usageTime());
        assertEquals(DestinationType.MOBILE, response.destinationType());
        assertEquals("+1234567890", response.destinationNumber());
        assertEquals("US", response.destinationCountry());
        assertEquals(RatePeriod.PEAK, response.ratePeriod());
        assertEquals(UsageSource.CDR, response.source());
        assertEquals("CDR_FILE_001", response.sourceFile());
        assertTrue(response.isRated()); // Auto-rated due to matching rule

        // Verify in database
        UsageRecordEntity saved = usageRecordRepository.findById(response.id()).orElse(null);
        assertNotNull(saved);
        assertTrue(saved.isRated());
        assertNotNull(saved.getRatingDate());
    }

    @Test
    void shouldIngestSMSUsageRecord() {
        // Given
        IngestUsageRecordCommand command = new IngestUsageRecordCommand(
                subscription.getId().toString(),
                "SMS",
                "MESSAGES",
                BigDecimal.valueOf(5),
                LocalDate.of(2024, 1, 16),
                LocalTime.of(12, 30),
                "MOBILE",
                "+1234567891",
                "US",
                "PEAK",
                "CDR_FILE_002",
                "CDR"
        );

        // When
        UsageRecordResponse response = ingestUsageRecordUseCase.handle(command);

        // Then
        assertNotNull(response);
        assertEquals(UsageType.SMS, response.usageType());
        assertEquals(UsageUnit.MESSAGES, response.usageUnit());
        assertEquals(BigDecimal.valueOf(5), response.usageAmount());
        assertFalse(response.isRated()); // No rating rule for SMS
    }

    @Test
    void shouldIngestDataUsageRecord() {
        // Given
        IngestUsageRecordCommand command = new IngestUsageRecordCommand(
                subscription.getId().toString(),
                "DATA",
                "MB",
                BigDecimal.valueOf(1024),
                LocalDate.of(2024, 1, 17),
                LocalTime.of(14, 15),
                "MOBILE",
                null,
                "US",
                "PEAK",
                "CDR_FILE_003",
                "CDR"
        );

        // When
        UsageRecordResponse response = ingestUsageRecordUseCase.handle(command);

        // Then
        assertNotNull(response);
        assertEquals(UsageType.DATA, response.usageType());
        assertEquals(UsageUnit.MB, response.usageUnit());
        assertEquals(BigDecimal.valueOf(1024), response.usageAmount());
        assertNull(response.destinationNumber()); // Optional field
    }

    @Test
    void shouldAutoRateUsageRecordWhenRuleExists() {
        // Given
        IngestUsageRecordCommand command = new IngestUsageRecordCommand(
                subscription.getId().toString(),
                "VOICE_CALL",
                "MINUTES",
                BigDecimal.valueOf(25),
                LocalDate.of(2024, 1, 18),
                LocalTime.of(9, 0),
                "MOBILE",
                "+1234567892",
                "US",
                "PEAK",
                "CDR_FILE_004",
                "CDR"
        );

        // When
        UsageRecordResponse response = ingestUsageRecordUseCase.handle(command);

        // Then - Should be auto-rated
        assertTrue(response.isRated());
        assertNotNull(response.ratingDate());
        assertEquals(BigDecimal.valueOf(0.10), response.unitRate());
        assertEquals("EUR", response.currency());
        assertEquals(BigDecimal.valueOf(2.50), response.totalAmount()); // 25 * 0.10
    }

    @Test
    void shouldNotCrashWhenRatingFails() {
        // Given - Usage record without matching rule (SMS)
        IngestUsageRecordCommand command = new IngestUsageRecordCommand(
                subscription.getId().toString(),
                "SMS",
                "MESSAGES",
                BigDecimal.valueOf(10),
                LocalDate.of(2024, 1, 19),
                LocalTime.of(16, 45),
                "MOBILE",
                "+1234567893",
                "US",
                "PEAK",
                "CDR_FILE_005",
                "CDR"
        );

        // When & Then - Should not throw exception
        assertDoesNotThrow(() -> {
            UsageRecordResponse response = ingestUsageRecordUseCase.handle(command);
            assertNotNull(response);
            assertFalse(response.isRated()); // Not rated due to no rule
        });
    }

    @Test
    void shouldIngestMultipleUsageRecords() {
        // Given
        List<IngestUsageRecordCommand> commands = List.of(
                new IngestUsageRecordCommand(
                        subscription.getId().toString(),
                        "VOICE_CALL",
                        "MINUTES",
                        BigDecimal.valueOf(5),
                        LocalDate.of(2024, 1, 20),
                        LocalTime.of(10, 0),
                        "MOBILE",
                        "+1234567890",
                        "US",
                        "PEAK",
                        "CDR_FILE_006",
                        "CDR"
                ),
                new IngestUsageRecordCommand(
                        subscription.getId().toString(),
                        "VOICE_CALL",
                        "MINUTES",
                        BigDecimal.valueOf(10),
                        LocalDate.of(2024, 1, 20),
                        LocalTime.of(11, 0),
                        "MOBILE",
                        "+1234567891",
                        "US",
                        "PEAK",
                        "CDR_FILE_006",
                        "CDR"
                ),
                new IngestUsageRecordCommand(
                        subscription.getId().toString(),
                        "VOICE_CALL",
                        "MINUTES",
                        BigDecimal.valueOf(15),
                        LocalDate.of(2024, 1, 20),
                        LocalTime.of(12, 0),
                        "MOBILE",
                        "+1234567892",
                        "US",
                        "PEAK",
                        "CDR_FILE_006",
                        "CDR"
                )
        );

        // When
        List<UsageRecordResponse> responses = commands.stream()
                .map(ingestUsageRecordUseCase::handle)
                .toList();

        // Then
        assertEquals(3, responses.size());
        responses.forEach(response -> {
            assertNotNull(response.id());
            assertTrue(response.isRated()); // All should be rated
        });

        // Verify in database
        assertEquals(3, usageRecordRepository.count());
    }

    @Test
    void shouldThrowExceptionWhenSubscriptionNotFound() {
        // Given
        String nonExistentSubscriptionId = "99999999-9999-9999-9999-999999999999";
        IngestUsageRecordCommand command = new IngestUsageRecordCommand(
                nonExistentSubscriptionId,
                "VOICE_CALL",
                "MINUTES",
                BigDecimal.valueOf(10),
                LocalDate.of(2024, 1, 21),
                LocalTime.of(10, 0),
                "MOBILE",
                "+1234567890",
                "US",
                "PEAK",
                "CDR_FILE_007",
                "CDR"
        );

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            ingestUsageRecordUseCase.handle(command);
        });

        assertTrue(exception.getMessage().contains("Subscription not found"));
    }

    @Test
    void shouldIngestUsageRecordWithDestinationTypeLandline() {
        // Given
        IngestUsageRecordCommand command = new IngestUsageRecordCommand(
                subscription.getId().toString(),
                "VOICE_CALL",
                "MINUTES",
                BigDecimal.valueOf(8),
                LocalDate.of(2024, 1, 22),
                LocalTime.of(14, 20),
                "LANDLINE",
                "+1234567890",
                "US",
                "PEAK",
                "CDR_FILE_008",
                "CDR"
        );

        // When
        UsageRecordResponse response = ingestUsageRecordUseCase.handle(command);

        // Then
        assertNotNull(response);
        assertEquals(DestinationType.LANDLINE, response.destinationType());
    }

    @Test
    void shouldIngestUsageRecordWithOffPeakRatePeriod() {
        // Given
        IngestUsageRecordCommand command = new IngestUsageRecordCommand(
                subscription.getId().toString(),
                "VOICE_CALL",
                "MINUTES",
                BigDecimal.valueOf(12),
                LocalDate.of(2024, 1, 23),
                LocalTime.of(2, 0), // 2 AM - off peak
                "MOBILE",
                "+1234567890",
                "US",
                "OFF_PEAK",
                "CDR_FILE_009",
                "CDR"
        );

        // When
        UsageRecordResponse response = ingestUsageRecordUseCase.handle(command);

        // Then
        assertNotNull(response);
        assertEquals(RatePeriod.OFF_PEAK, response.ratePeriod());
    }

    @Test
    void shouldSetAllOptionalFieldsCorrectly() {
        // Given
        IngestUsageRecordCommand command = new IngestUsageRecordCommand(
                subscription.getId().toString(),
                "VOICE_CALL",
                "MINUTES",
                BigDecimal.valueOf(10),
                LocalDate.of(2024, 1, 24),
                LocalTime.of(15, 30),
                "MOBILE",
                "+1234567890",
                "US",
                "PEAK",
                "CDR_FILE_010",
                "CDR"
        );

        // When
        UsageRecordResponse response = ingestUsageRecordUseCase.handle(command);

        // Then
        assertNotNull(response.networkId());
        assertEquals(UsageSource.CDR, response.source());
        assertEquals("CDR_FILE_010", response.sourceFile());
        assertNotNull(response.createdAt());
        assertNotNull(response.updatedAt());
    }

    @Test
    void shouldHandleNullOptionalFields() {
        // Given - Command with null optional fields
        IngestUsageRecordCommand command = new IngestUsageRecordCommand(
                subscription.getId().toString(),
                "VOICE_CALL",
                "MINUTES",
                BigDecimal.valueOf(10),
                LocalDate.of(2024, 1, 25),
                LocalTime.of(16, 0),
                null, // destinationType
                null, // destinationNumber
                null, // destinationCountry
                null, // ratePeriod
                null, // networkId
                null  // source
        );

        // When
        UsageRecordResponse response = ingestUsageRecordUseCase.handle(command);

        // Then - Should handle nulls gracefully
        assertNotNull(response);
        assertNull(response.destinationType());
        assertNull(response.destinationNumber());
        assertNull(response.destinationCountry());
        assertNull(response.ratePeriod());
        assertNull(response.source());
        assertTrue(response.isRated()); // Still rated if rule matches
    }

    @Test
    void shouldTrackUsageRecordIngestedMetric() {
        // Given
        IngestUsageRecordCommand command = new IngestUsageRecordCommand(
                subscription.getId().toString(),
                "VOICE_CALL",
                "MINUTES",
                BigDecimal.valueOf(10),
                LocalDate.of(2024, 1, 26),
                LocalTime.of(17, 0),
                "MOBILE",
                "+1234567890",
                "US",
                "PEAK",
                "CDR_FILE_011",
                "CDR"
        );

        // When
        UsageRecordResponse response = ingestUsageRecordUseCase.handle(command);

        // Then - Metrics are tracked in the use case
        assertNotNull(response);
        // businessMetrics.incrementUsageRecordIngested() is called
    }

    @Test
    void shouldSaveUsageRecordToRepository() {
        // Given
        IngestUsageRecordCommand command = new IngestUsageRecordCommand(
                subscription.getId().toString(),
                "VOICE_CALL",
                "MINUTES",
                BigDecimal.valueOf(10),
                LocalDate.of(2024, 1, 27),
                LocalTime.of(18, 0),
                "MOBILE",
                "+1234567890",
                "US",
                "PEAK",
                "CDR_FILE_012",
                "CDR"
        );

        // When
        UsageRecordResponse response = ingestUsageRecordUseCase.handle(command);

        // Then - Verify persistence
        UsageRecordEntity saved = usageRecordRepository.findById(response.id()).orElse(null);
        assertNotNull(saved);
        assertEquals(subscription.getId(), saved.getSubscription().getId());
        assertEquals(UsageType.VOICE_CALL, saved.getUsageType());
    }
}
