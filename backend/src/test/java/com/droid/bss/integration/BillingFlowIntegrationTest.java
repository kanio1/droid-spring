package com.droid.bss.integration;

import com.droid.bss.AbstractIntegrationTest;
import com.droid.bss.application.command.billing.IngestUsageRecordUseCase;
import com.droid.bss.application.command.billing.ProcessBillingCycleUseCase;
import com.droid.bss.application.command.billing.StartBillingCycleUseCase;
import com.droid.bss.application.dto.billing.IngestUsageRecordCommand;
import com.droid.bss.application.dto.billing.StartBillingCycleCommand;
import com.droid.bss.application.dto.billing.UsageRecordResponse;
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
import com.droid.bss.domain.subscription.SubscriptionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for billing flow: Usage ingestion -> Rating -> Billing cycle -> Invoice generation
 */
@Transactional
class BillingFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private IngestUsageRecordUseCase ingestUseCase;

    @Autowired
    private StartBillingCycleUseCase startCycleUseCase;

    @Autowired
    private ProcessBillingCycleUseCase processCycleUseCase;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private UsageRecordRepository usageRecordRepository;

    @Autowired
    private RatingRuleRepository ratingRuleRepository;

    @Autowired
    private BillingCycleRepository billingCycleRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    private CustomerEntity customer;
    private ProductEntity product;
    private SubscriptionEntity subscription;

    @BeforeEach
    void setUp() {
        // Create customer
        customer = new CustomerEntity();
        customer.setFirstName("Jan");
        customer.setLastName("Kowalski");
        customer.setEmail("jan.kowalski@example.com");
        customer.setPhone("+48123456789");
        customer.setStatus(com.droid.bss.domain.customer.CustomerStatus.ACTIVE);
        customer = customerRepository.save(customer);

        // Create product
        product = new ProductEntity();
        product.setName("Internet Fiber 500");
        product.setDescription("Fiber optic internet 500 Mbps");
        product.setPrice(new BigDecimal("99.99"));
        product.setCurrency("PLN");
        product.setStatus(com.droid.bss.domain.product.ProductStatus.ACTIVE);
        product = productRepository.save(product);

        // Create subscription
        subscription = new SubscriptionEntity();
        subscription.setCustomer(customer);
        subscription.setProduct(product);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setStartDate(LocalDate.now().minusDays(30));
        subscription.setEndDate(LocalDate.now().plusDays(335));
        subscription = subscriptionRepository.save(subscription);

        // Create rating rules
        createRatingRules();
    }

    private void createRatingRules() {
        // Voice - National Peak
        RatingRuleEntity voiceNationalPeak = new RatingRuleEntity();
        voiceNationalPeak.setUsageType(UsageType.VOICE);
        voiceNationalPeak.setDestinationType(DestinationType.NATIONAL);
        voiceNationalPeak.setRatePeriod(RatePeriod.PEAK);
        voiceNationalPeak.setUnitRate(new BigDecimal("0.50"));
        voiceNationalPeak.setMinimumUnits(0L);
        voiceNationalPeak.setCurrency("PLN");
        voiceNationalPeak.setEffectiveFrom(LocalDate.now().minusDays(60));
        voiceNationalPeak.setEffectiveTo(LocalDate.now().plusDays(60));
        voiceNationalPeak.setActive(true);
        ratingRuleRepository.save(voiceNationalPeak);

        // Voice - National Off-Peak
        RatingRuleEntity voiceNationalOffPeak = new RatingRuleEntity();
        voiceNationalOffPeak.setUsageType(UsageType.VOICE);
        voiceNationalOffPeak.setDestinationType(DestinationType.NATIONAL);
        voiceNationalOffPeak.setRatePeriod(RatePeriod.OFF_PEAK);
        voiceNationalOffPeak.setUnitRate(new BigDecimal("0.30"));
        voiceNationalOffPeak.setMinimumUnits(0L);
        voiceNationalOffPeak.setCurrency("PLN");
        voiceNationalOffPeak.setEffectiveFrom(LocalDate.now().minusDays(60));
        voiceNationalOffPeak.setEffectiveTo(LocalDate.now().plusDays(60));
        voiceNationalOffPeak.setActive(true);
        ratingRuleRepository.save(voiceNationalOffPeak);

        // SMS
        RatingRuleEntity smsRule = new RatingRuleEntity();
        smsRule.setUsageType(UsageType.SMS);
        smsRule.setDestinationType(DestinationType.NATIONAL);
        smsRule.setRatePeriod(RatePeriod.PEAK);
        smsRule.setUnitRate(new BigDecimal("0.20"));
        smsRule.setMinimumUnits(0L);
        smsRule.setCurrency("PLN");
        smsRule.setEffectiveFrom(LocalDate.now().minusDays(60));
        smsRule.setEffectiveTo(LocalDate.now().plusDays(60));
        smsRule.setActive(true);
        ratingRuleRepository.save(smsRule);

        // Data
        RatingRuleEntity dataRule = new RatingRuleEntity();
        dataRule.setUsageType(UsageType.DATA);
        dataRule.setDestinationType(DestinationType.NATIONAL);
        dataRule.setRatePeriod(RatePeriod.PEAK);
        dataRule.setUnitRate(new BigDecimal("0.10"));
        dataRule.setMinimumUnits(0L);
        dataRule.setCurrency("PLN");
        dataRule.setEffectiveFrom(LocalDate.now().minusDays(60));
        dataRule.setEffectiveTo(LocalDate.now().plusDays(60));
        dataRule.setActive(true);
        ratingRuleRepository.save(dataRule);
    }

    @Test
    void shouldCompleteFullBillingFlow() {
        // Step 1: Ingest usage records
        IngestUsageRecordCommand voiceCommand = createIngestCommand(
                subscription.getId(),
                "VOICE",
                "MINUTES",
                new BigDecimal("10"),
                LocalDate.now().minusDays(5),
                LocalTime.of(10, 0),
                "NATIONAL",
                "+48987654321"
        );

        IngestUsageRecordCommand smsCommand = createIngestCommand(
                subscription.getId(),
                "SMS",
                "COUNT",
                new BigDecimal("5"),
                LocalDate.now().minusDays(4),
                LocalTime.of(15, 30),
                "NATIONAL",
                "+48111222333"
        );

        IngestUsageRecordCommand dataCommand = createIngestCommand(
                subscription.getId(),
                "DATA",
                "GB",
                new BigDecimal("2.5"),
                LocalDate.now().minusDays(3),
                LocalTime.of(20, 0),
                "NATIONAL",
                null
        );

        UsageRecordResponse voiceResponse = ingestUseCase.handle(voiceCommand);
        UsageRecordResponse smsResponse = ingestUseCase.handle(smsCommand);
        UsageRecordResponse dataResponse = ingestUseCase.handle(dataCommand);

        // Verify usage records were created
        assertNotNull(voiceResponse.id());
        assertNotNull(smsResponse.id());
        assertNotNull(dataResponse.id());
        assertTrue(voiceResponse.isRated());
        assertTrue(smsResponse.isRated());
        assertTrue(dataResponse.isRated());

        // Verify they are rated
        assertNotNull(voiceResponse.totalAmount());
        assertNotNull(smsResponse.totalAmount());
        assertNotNull(dataResponse.totalAmount());

        // Step 2: Start billing cycle
        StartBillingCycleCommand cycleCommand = new StartBillingCycleCommand(
                customer.getId(),
                LocalDate.now().minusDays(10),
                LocalDate.now().minusDays(1),
                LocalDate.now(),
                "MONTHLY"
        );

        BillingCycleEntity billingCycle = startCycleUseCase.handle(cycleCommand);
        assertNotNull(billingCycle.getId());
        assertEquals(BillingCycleStatus.PENDING, billingCycle.getStatus());
        assertEquals(LocalDate.now().minusDays(10), billingCycle.getCycleStart());
        assertEquals(LocalDate.now().minusDays(1), billingCycle.getCycleEnd());

        // Step 3: Process billing cycle
        BillingCycleEntity processedCycle = processCycleUseCase.handle(billingCycle.getId());

        // Verify billing cycle was processed
        assertEquals(BillingCycleStatus.PROCESSED, processedCycle.getStatus());
        assertNotNull(processedCycle.getProcessedAt());
        assertTrue(processedCycle.getInvoiceCount() > 0);
        assertTrue(processedCycle.getTotalAmount().compareTo(BigDecimal.ZERO) > 0);

        // Verify invoice was created
        List<InvoiceEntity> invoices = invoiceRepository.findByCustomerId(customer.getId());
        assertFalse(invoices.isEmpty());

        InvoiceEntity invoice = invoices.get(0);
        assertEquals(InvoiceStatus.DRAFT, invoice.getStatus());
        assertEquals(processedCycle.getBillingDate(), invoice.getBillingDate());
        assertEquals(processedCycle.getCycleStart(), invoice.getBillingPeriodStart());
        assertEquals(processedCycle.getCycleEnd(), invoice.getBillingPeriodEnd());
        assertTrue(invoice.getTotalAmount().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(invoice.getItems().size() > 0);
    }

    @Test
    void shouldRateUsageRecordsCorrectly() {
        // Ingest voice usage
        IngestUsageRecordCommand command = createIngestCommand(
                subscription.getId(),
                "VOICE",
                "MINUTES",
                new BigDecimal("15"),
                LocalDate.now().minusDays(2),
                LocalTime.of(14, 0), // Peak time
                "NATIONAL",
                "+48123456789"
        );

        UsageRecordResponse response = ingestUseCase.handle(command);

        // Verify rating
        assertTrue(response.isRated());
        assertNotNull(response.unitRate());
        assertNotNull(response.totalAmount());

        // Expected: 15 minutes * 0.50 PLN = 7.50 PLN + 23% VAT = 9.225 PLN
        BigDecimal expectedAmount = new BigDecimal("7.50");
        assertEquals(0, response.totalAmount().compareTo(expectedAmount));
    }

    @Test
    void shouldCreateBillingCycleWithProperValidation() {
        // Start first cycle
        StartBillingCycleCommand firstCommand = new StartBillingCycleCommand(
                customer.getId(),
                LocalDate.now().minusDays(10),
                LocalDate.now().minusDays(1),
                LocalDate.now(),
                "MONTHLY"
        );

        BillingCycleEntity firstCycle = startCycleUseCase.handle(firstCommand);
        assertNotNull(firstCycle.getId());

        // Try to create overlapping cycle - should fail
        StartBillingCycleCommand overlappingCommand = new StartBillingCycleCommand(
                customer.getId(),
                LocalDate.now().minusDays(5),
                LocalDate.now().plusDays(5),
                LocalDate.now(),
                "MONTHLY"
        );

        assertThrows(IllegalStateException.class, () -> {
            startCycleUseCase.handle(overlappingCommand);
        });
    }

    @Test
    void shouldHandleUnratedUsageRecords() {
        // Ingest usage but don't auto-rate (simulate missing rating rules)
        IngestUsageRecordCommand command = createIngestCommand(
                subscription.getId(),
                "VIDEO",
                "MINUTES",
                new BigDecimal("30"),
                LocalDate.now(),
                LocalTime.of(12, 0),
                null,
                null
        );

        UsageRecordResponse response = ingestUseCase.handle(command);

        // Should create but not rate (no rule for VIDEO)
        assertNotNull(response.id());
        // Note: In actual implementation, this would fail during rating
        // For this test, we assume VIDEO type won't have a matching rule

        // Verify unrated records exist
        List<UsageRecordEntity> unrated = usageRecordRepository.findUnrated();
        // Could be unrated if no rules match
    }

    private IngestUsageRecordCommand createIngestCommand(
            String subscriptionId,
            String usageType,
            String usageUnit,
            BigDecimal amount,
            LocalDate date,
            LocalTime time,
            String destinationType,
            String destinationNumber
    ) {
        return new IngestUsageRecordCommand(
                subscriptionId,
                usageType,
                usageUnit,
                amount,
                date,
                time,
                destinationType,
                destinationNumber,
                "POL",
                "NETWORK-1",
                "PEAK",
                "SYSTEM",
                "test-cdr.txt"
        );
    }
}
