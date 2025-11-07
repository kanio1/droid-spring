package com.droid.bss.application.command.billing;

import com.droid.bss.AbstractIntegrationTest;
import com.droid.bss.domain.billing.*;
import com.droid.bss.domain.customer.CustomerEntity;
import com.droid.bss.domain.customer.CustomerRepository;
import com.droid.bss.domain.invoice.InvoiceEntity;
import com.droid.bss.domain.invoice.InvoiceRepository;
import com.droid.bss.domain.invoice.InvoiceStatus;
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
 * Integration test for ProcessBillingCycleUseCase
 * Tests complete billing cycle processing flow
 */
@Transactional
class ProcessBillingCycleUseCaseTest extends AbstractIntegrationTest {

    @Autowired
    private ProcessBillingCycleUseCase processBillingCycleUseCase;

    @Autowired
    private StartBillingCycleUseCase startBillingCycleUseCase;

    @Autowired
    private IngestUsageRecordUseCase ingestUsageRecordUseCase;

    @Autowired
    private BillingCycleRepository billingCycleRepository;

    @Autowired
    private UsageRecordRepository usageRecordRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private RatingRuleRepository ratingRuleRepository;

    private CustomerEntity customer;
    private ProductEntity product;
    private SubscriptionEntity subscription;
    private BillingCycleEntity billingCycle;
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
                com.droid.bss.domain.subscription.SubscriptionStatus.ACTIVE
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

        // Create billing cycle
        var startCommand = new com.droid.bss.application.dto.billing.StartBillingCycleCommand(
                customer.getId().toString(),
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 31),
                LocalDate.of(2024, 2, 1),
                "MONTHLY"
        );
        billingCycle = startBillingCycleUseCase.handle(startCommand);
    }

    @Test
    void shouldProcessBillingCycleWithSingleCustomer() {
        // Given - Ingest usage records
        IngestUsageRecordCommand usage1 = new IngestUsageRecordCommand(
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
        ingestUsageRecordUseCase.handle(usage1);

        IngestUsageRecordCommand usage2 = new IngestUsageRecordCommand(
                subscription.getId().toString(),
                "VOICE_CALL",
                "MINUTES",
                BigDecimal.valueOf(15),
                LocalDate.of(2024, 1, 20),
                LocalTime.of(14, 0),
                "MOBILE",
                "+1234567891",
                "US",
                "PEAK",
                "CDR_FILE_001",
                "CDR"
        );
        ingestUsageRecordUseCase.handle(usage2);

        // When
        BillingCycleEntity processedCycle = processBillingCycleUseCase.handle(billingCycle.getId());

        // Then
        assertNotNull(processedCycle);
        assertEquals(BillingCycleStatus.PROCESSED, processedCycle.getStatus());
        assertNotNull(processedCycle.getProcessedAt());
        assertEquals(2, processedCycle.getInvoices().size());

        // Verify invoice
        InvoiceEntity invoice = processedCycle.getInvoices().get(0);
        assertEquals(InvoiceStatus.DRAFT, invoice.getStatus());
        assertEquals(customer, invoice.getCustomer());
        assertEquals(LocalDate.of(2024, 1, 1), invoice.getBillingPeriodStart());
        assertEquals(LocalDate.of(2024, 1, 31), invoice.getBillingPeriodEnd());
        assertEquals(LocalDate.of(2024, 2, 1), invoice.getBillingDate());
        assertEquals(LocalDate.of(2024, 2, 15), invoice.getDueDate());
        assertNotNull(invoice.getTotalAmount());

        // Verify invoice items
        assertEquals(2, invoice.getItems().size());

        // Verify totals
        assertNotNull(processedCycle.getTotalAmount());
        assertNotNull(processedCycle.getTaxAmount());
        assertNotNull(processedCycle.getTotalWithTax());
        assertEquals(processedCycle.getInvoiceCount(), processedCycle.getInvoices().size());
    }

    @Test
    void shouldProcessBillingCycleWithMultipleCustomers() {
        // Given - Create second customer
        CustomerEntity customer2 = new CustomerEntity(
                "test-customer-2@example.com",
                "Jane",
                "Smith",
                "987654321"
        );
        customer2 = customerRepository.save(customer2);

        SubscriptionEntity subscription2 = new SubscriptionEntity(
                customer2,
                product,
                com.droid.bss.domain.subscription.SubscriptionStatus.ACTIVE
        );
        subscription2 = subscriptionRepository.save(subscription2);

        // Ingest usage for first customer
        IngestUsageRecordCommand usage1 = new IngestUsageRecordCommand(
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
        ingestUsageRecordUseCase.handle(usage1);

        // Ingest usage for second customer
        IngestUsageRecordCommand usage2 = new IngestUsageRecordCommand(
                subscription2.getId().toString(),
                "VOICE_CALL",
                "MINUTES",
                BigDecimal.valueOf(20),
                LocalDate.of(2024, 1, 16),
                LocalTime.of(11, 0),
                "MOBILE",
                "+1234567891",
                "US",
                "PEAK",
                "CDR_FILE_002",
                "CDR"
        );
        ingestUsageRecordUseCase.handle(usage2);

        // When
        BillingCycleEntity processedCycle = processBillingCycleUseCase.handle(billingCycle.getId());

        // Then
        assertEquals(2, processedCycle.getInvoices().size()); // One invoice per customer
        assertEquals(2, processedCycle.getInvoiceCount());

        // Verify both invoices
        processedCycle.getInvoices().forEach(invoice -> {
            assertEquals(InvoiceStatus.DRAFT, invoice.getStatus());
            assertEquals(1, invoice.getItems().size()); // One usage record per invoice
        });
    }

    @Test
    void shouldSkipAlreadyRatedUsageRecords() {
        // Given - Ingest usage record
        IngestUsageRecordCommand usage1 = new IngestUsageRecordCommand(
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
        var savedUsage = ingestUsageRecordUseCase.handle(usage1);

        // When - Process cycle twice
        processBillingCycleUseCase.handle(billingCycle.getId());
        var processedCycle2 = processBillingCycleUseCase.handle(billingCycle.getId());

        // Then - Should not create duplicate invoices
        assertEquals(1, processedCycle2.getInvoices().size());
    }

    @Test
    void shouldProcessBillingCycleWithUnratedUsage() {
        // Given - Usage without rating rule (should not crash)
        IngestUsageRecordCommand smsUsage = new IngestUsageRecordCommand(
                subscription.getId().toString(),
                "SMS", // No rating rule for SMS
                "MESSAGES",
                BigDecimal.valueOf(5),
                LocalDate.of(2024, 1, 15),
                LocalTime.of(10, 0),
                "MOBILE",
                "+1234567890",
                "US",
                "PEAK",
                "CDR_FILE_001",
                "CDR"
        );
        // This should succeed but the SMS won't be rated

        // When
        BillingCycleEntity processedCycle = processBillingCycleUseCase.handle(billingCycle.getId());

        // Then - Cycle should still process (SMS usage is skipped as it's not rated)
        assertEquals(BillingCycleStatus.PROCESSED, processedCycle.getStatus());
        assertNotNull(processedCycle.getProcessedAt());
    }

    @Test
    void shouldThrowExceptionWhenBillingCycleNotFound() {
        // Given
        String nonExistentId = "99999999-9999-9999-9999-999999999999";

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            processBillingCycleUseCase.handle(nonExistentId);
        });

        assertTrue(exception.getMessage().contains("Billing cycle not found"));
    }

    @Test
    void shouldThrowExceptionWhenBillingCycleAlreadyProcessed() {
        // Given - Process cycle once
        processBillingCycleUseCase.handle(billingCycle.getId());

        // When & Then - Try to process again
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            processBillingCycleUseCase.handle(billingCycle.getId());
        });

        assertTrue(exception.getMessage().contains("is not pending"));
    }

    @Test
    void shouldCalculateTotalsCorrectly() {
        // Given
        IngestUsageRecordCommand usage1 = new IngestUsageRecordCommand(
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
        ingestUsageRecordUseCase.handle(usage1);

        IngestUsageRecordCommand usage2 = new IngestUsageRecordCommand(
                subscription.getId().toString(),
                "VOICE_CALL",
                "MINUTES",
                BigDecimal.valueOf(15),
                LocalDate.of(2024, 1, 20),
                LocalTime.of(14, 0),
                "MOBILE",
                "+1234567891",
                "US",
                "PEAK",
                "CDR_FILE_001",
                "CDR"
        );
        ingestUsageRecordUseCase.handle(usage2);

        // When
        BillingCycleEntity processedCycle = processBillingCycleUseCase.handle(billingCycle.getId());

        // Then - Verify totals (25 minutes * 0.10 = 2.50 EUR + tax)
        assertNotNull(processedCycle.getTotalAmount());
        assertNotNull(processedCycle.getTaxAmount());
        assertNotNull(processedCycle.getTotalWithTax());

        // Tax should be 23% of total
        BigDecimal expectedTax = processedCycle.getTotalAmount().multiply(new BigDecimal("0.23"));
        assertEquals(expectedTax.setScale(2, BigDecimal.ROUND_HALF_UP),
                     processedCycle.getTaxAmount().setScale(2, BigDecimal.ROUND_HALF_UP));
    }

    @Test
    void shouldOnlyProcessUsageInCyclePeriod() {
        // Given - Usage records inside and outside billing period
        IngestUsageRecordCommand usageInPeriod = new IngestUsageRecordCommand(
                subscription.getId().toString(),
                "VOICE_CALL",
                "MINUTES",
                BigDecimal.valueOf(10),
                LocalDate.of(2024, 1, 15), // Inside period (Jan 1-31)
                LocalTime.of(10, 0),
                "MOBILE",
                "+1234567890",
                "US",
                "PEAK",
                "CDR_FILE_001",
                "CDR"
        );
        ingestUsageRecordUseCase.handle(usageInPeriod);

        IngestUsageRecordCommand usageOutOfPeriod = new IngestUsageRecordCommand(
                subscription.getId().toString(),
                "VOICE_CALL",
                "MINUTES",
                BigDecimal.valueOf(20),
                LocalDate.of(2024, 2, 15), // Outside period
                LocalTime.of(10, 0),
                "MOBILE",
                "+1234567891",
                "US",
                "PEAK",
                "CDR_FILE_002",
                "CDR"
        );
        ingestUsageRecordUseCase.handle(usageOutOfPeriod);

        // When
        BillingCycleEntity processedCycle = processBillingCycleUseCase.handle(billingCycle.getId());

        // Then - Only in-period usage should be invoiced
        InvoiceEntity invoice = processedCycle.getInvoices().get(0);
        assertEquals(1, invoice.getItems().size()); // Only one usage in period
        assertEquals(BigDecimal.valueOf(1.00), invoice.getTotalAmount()); // 10 minutes * 0.10
    }
}
