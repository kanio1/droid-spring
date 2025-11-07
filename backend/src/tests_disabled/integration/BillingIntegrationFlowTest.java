package com.droid.bss.integration;

import com.droid.bss.AbstractIntegrationTest;
import com.droid.bss.application.command.billing.*;
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
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end integration test for complete billing flow
 * Tests: Customer -> Subscription -> Usage Ingestion -> Rating -> Billing Cycle -> Invoice
 */
@Transactional
class BillingIntegrationFlowTest extends AbstractIntegrationTest {

    @Autowired
    private IngestUsageRecordUseCase ingestUsageRecordUseCase;

    @Autowired
    private StartBillingCycleUseCase startBillingCycleUseCase;

    @Autowired
    private ProcessBillingCycleUseCase processBillingCycleUseCase;

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
    private ProductEntity voiceProduct;
    private ProductEntity dataProduct;
    private SubscriptionEntity voiceSubscription;
    private SubscriptionEntity dataSubscription;

    @BeforeEach
    void setUp() {
        // Step 1: Create customer
        customer = new CustomerEntity(
                "test-customer@example.com",
                "John",
                "Doe",
                "123456789"
        );
        customer = customerRepository.save(customer);

        // Step 2: Create products
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

        // Step 3: Create subscriptions
        voiceSubscription = new SubscriptionEntity(
                customer,
                voiceProduct,
                SubscriptionStatus.ACTIVE
        );
        voiceSubscription = subscriptionRepository.save(voiceSubscription);

        dataSubscription = new SubscriptionEntity(
                customer,
                dataProduct,
                SubscriptionStatus.ACTIVE
        );
        dataSubscription = subscriptionRepository.save(dataSubscription);

        // Step 4: Create rating rules
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

        RatingRuleEntity dataRule = new RatingRuleEntity(
                UsageType.DATA,
                DestinationType.MOBILE,
                RatePeriod.PEAK,
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2025, 12, 31),
                BigDecimal.valueOf(0.001),
                1L,
                "EUR"
        );
        ratingRuleRepository.save(dataRule);
    }

    @Test
    void shouldCompleteFullBillingFlow() {
        // Step 1: Ingest CDR usage records for January 2024
        List<IngestUsageRecordCommand> cdrRecords = createMockCDRRecords();
        List<UsageRecordResponse> ingestedRecords = cdrRecords.stream()
                .map(ingestUsageRecordUseCase::handle)
                .toList();

        // Verify ingestion
        assertEquals(50, ingestedRecords.size());
        ingestedRecords.forEach(record -> {
            assertNotNull(record.id());
            assertTrue(record.isRated()); // Auto-rated
        });

        // Step 2: Start billing cycle for January 2024
        StartBillingCycleCommand cycleCommand = new StartBillingCycleCommand(
                customer.getId().toString(),
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 31),
                LocalDate.of(2024, 2, 1),
                "MONTHLY"
        );
        BillingCycleEntity billingCycle = startBillingCycleUseCase.handle(cycleCommand);

        assertNotNull(billingCycle.getId());
        assertEquals(BillingCycleStatus.PENDING, billingCycle.getStatus());
        assertEquals(0, billingCycle.getInvoices().size());

        // Step 3: Process billing cycle
        BillingCycleEntity processedCycle = processBillingCycleUseCase.handle(billingCycle.getId());

        // Verify processing
        assertEquals(BillingCycleStatus.PROCESSED, processedCycle.getStatus());
        assertNotNull(processedCycle.getProcessedAt());
        assertEquals(2, processedCycle.getInvoices().size()); // One invoice per subscription

        // Verify invoices
        processedCycle.getInvoices().forEach(invoice -> {
            assertEquals(InvoiceStatus.DRAFT, invoice.getStatus());
            assertTrue(invoice.getTotalAmount().compareTo(BigDecimal.ZERO) > 0);
            assertTrue(invoice.getItems().size() > 0);
        });

        // Verify totals
        assertNotNull(processedCycle.getTotalAmount());
        assertNotNull(processedCycle.getTaxAmount());
        assertNotNull(processedCycle.getTotalWithTax());
        assertEquals(processedCycle.getInvoiceCount(), processedCycle.getInvoices().size());
    }

    @Test
    void shouldHandleMultipleBillingCycles() {
        // Step 1: Ingest usage for January
        List<IngestUsageRecordCommand> januaryUsage = createMockCDRRecordsForPeriod(
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31)
        );
        januaryUsage.forEach(ingestUsageRecordUseCase::handle);

        // Step 2: Start and process January cycle
        StartBillingCycleCommand januaryCycle = new StartBillingCycleCommand(
                customer.getId().toString(),
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 31),
                LocalDate.of(2024, 2, 1),
                "MONTHLY"
        );
        BillingCycleEntity processedJanuary = processBillingCycleUseCase.handle(
                startBillingCycleUseCase.handle(januaryCycle).getId()
        );

        assertEquals(1, processedJanuary.getInvoices().size());

        // Step 3: Ingest usage for February
        List<IngestUsageRecordCommand> februaryUsage = createMockCDRRecordsForPeriod(
                LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 29)
        );
        februaryUsage.forEach(ingestUsageRecordUseCase::handle);

        // Step 4: Start and process February cycle
        StartBillingCycleCommand februaryCycle = new StartBillingCycleCommand(
                customer.getId().toString(),
                LocalDate.of(2024, 2, 1),
                LocalDate.of(2024, 2, 29),
                LocalDate.of(2024, 3, 1),
                "MONTHLY"
        );
        BillingCycleEntity processedFebruary = processBillingCycleUseCase.handle(
                startBillingCycleUseCase.handle(februaryCycle).getId()
        );

        assertEquals(1, processedFebruary.getInvoices().size());
        assertNotEquals(processedJanuary.getId(), processedFebruary.getId());

        // Verify total billing cycles
        assertEquals(2, billingCycleRepository.findAll().size());
    }

    @Test
    void shouldGenerateInvoicesWithCorrectLineItems() {
        // Step 1: Ingest specific usage
        IngestUsageRecordCommand usage1 = new IngestUsageRecordCommand(
                voiceSubscription.getId().toString(),
                "VOICE_CALL",
                "MINUTES",
                BigDecimal.valueOf(10),
                LocalDate.of(2024, 1, 15),
                LocalTime.of(10, 0),
                "MOBILE",
                "+1234567890",
                "US",
                "PEAK",
                "CDR_001",
                "CDR"
        );
        ingestUsageRecordUseCase.handle(usage1);

        IngestUsageRecordCommand usage2 = new IngestUsageRecordCommand(
                voiceSubscription.getId().toString(),
                "VOICE_CALL",
                "MINUTES",
                BigDecimal.valueOf(15),
                LocalDate.of(2024, 1, 20),
                LocalTime.of(14, 0),
                "MOBILE",
                "+1234567891",
                "US",
                "PEAK",
                "CDR_001",
                "CDR"
        );
        ingestUsageRecordUseCase.handle(usage2);

        // Step 2: Process billing cycle
        StartBillingCycleCommand cycle = new StartBillingCycleCommand(
                customer.getId().toString(),
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 31),
                LocalDate.of(2024, 2, 1),
                "MONTHLY"
        );
        BillingCycleEntity processedCycle = processBillingCycleUseCase.handle(
                startBillingCycleUseCase.handle(cycle).getId()
        );

        // Step 3: Verify invoice line items
        InvoiceEntity invoice = processedCycle.getInvoices().get(0);
        assertEquals(2, invoice.getItems().size());

        // Verify each line item
        invoice.getItems().forEach(item -> {
            assertNotNull(item.getDescription());
            assertTrue(item.getTotalPrice().compareTo(BigDecimal.ZERO) > 0);
            assertEquals(BigDecimal.ONE, item.getQuantity());
        });
    }

    @Test
    void shouldApplyCorrectTaxRate() {
        // Step 1: Ingest usage
        IngestUsageRecordCommand usage = new IngestUsageRecordCommand(
                voiceSubscription.getId().toString(),
                "VOICE_CALL",
                "MINUTES",
                BigDecimal.valueOf(100),
                LocalDate.of(2024, 1, 15),
                LocalTime.of(10, 0),
                "MOBILE",
                "+1234567890",
                "US",
                "PEAK",
                "CDR_001",
                "CDR"
        );
        ingestUsageRecordUseCase.handle(usage);

        // Step 2: Process cycle
        StartBillingCycleCommand cycle = new StartBillingCycleCommand(
                customer.getId().toString(),
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 31),
                LocalDate.of(2024, 2, 1),
                "MONTHLY"
        );
        BillingCycleEntity processedCycle = processBillingCycleUseCase.handle(
                startBillingCycleUseCase.handle(cycle).getId()
        );

        // Step 3: Verify tax calculation (23% VAT)
        InvoiceEntity invoice = processedCycle.getInvoices().get(0);
        BigDecimal expectedSubtotal = BigDecimal.valueOf(10.00); // 100 minutes * 0.10
        BigDecimal expectedTax = expectedSubtotal.multiply(new BigDecimal("0.23"));
        BigDecimal expectedTotal = expectedSubtotal.add(expectedTax);

        assertEquals(expectedSubtotal, invoice.getTotalAmount());
        assertEquals(expectedTax.setScale(2, BigDecimal.ROUND_HALF_UP),
                     invoice.getTaxAmount().setScale(2, BigDecimal.ROUND_HALF_UP));
        assertEquals(expectedTotal.setScale(2, BigDecimal.ROUND_HALF_UP),
                     invoice.getTotalWithTax().setScale(2, BigDecimal.ROUND_HALF_UP));
    }

    @Test
    void shouldOnlyInvoiceUsageInBillingPeriod() {
        // Step 1: Ingest usage before billing period
        IngestUsageRecordCommand beforeUsage = new IngestUsageRecordCommand(
                voiceSubscription.getId().toString(),
                "VOICE_CALL",
                "MINUTES",
                BigDecimal.valueOf(50),
                LocalDate.of(2023, 12, 15), // Before billing period
                LocalTime.of(10, 0),
                "MOBILE",
                "+1234567890",
                "US",
                "PEAK",
                "CDR_001",
                "CDR"
        );
        ingestUsageRecordUseCase.handle(beforeUsage);

        // Step 2: Ingest usage during billing period
        IngestUsageRecordCommand duringUsage = new IngestUsageRecordCommand(
                voiceSubscription.getId().toString(),
                "VOICE_CALL",
                "MINUTES",
                BigDecimal.valueOf(30),
                LocalDate.of(2024, 1, 15), // During billing period
                LocalTime.of(10, 0),
                "MOBILE",
                "+1234567890",
                "US",
                "PEAK",
                "CDR_001",
                "CDR"
        );
        ingestUsageRecordUseCase.handle(duringUsage);

        // Step 3: Ingest usage after billing period
        IngestUsageRecordCommand afterUsage = new IngestUsageRecordCommand(
                voiceSubscription.getId().toString(),
                "VOICE_CALL",
                "MINUTES",
                BigDecimal.valueOf(20),
                LocalDate.of(2024, 2, 15), // After billing period
                LocalTime.of(10, 0),
                "MOBILE",
                "+1234567890",
                "US",
                "PEAK",
                "CDR_001",
                "CDR"
        );
        ingestUsageRecordUseCase.handle(afterUsage);

        // Step 4: Process January billing cycle
        StartBillingCycleCommand cycle = new StartBillingCycleCommand(
                customer.getId().toString(),
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 31),
                LocalDate.of(2024, 2, 1),
                "MONTHLY"
        );
        BillingCycleEntity processedCycle = processBillingCycleUseCase.handle(
                startBillingCycleUseCase.handle(cycle).getId()
        );

        // Step 5: Verify only January usage is invoiced
        InvoiceEntity invoice = processedCycle.getInvoices().get(0);
        assertEquals(1, invoice.getItems().size()); // Only one usage in period
        assertEquals(BigDecimal.valueOf(3.00), invoice.getTotalAmount()); // 30 minutes * 0.10
    }

    @Test
    void shouldGenerateSeparateInvoicesPerCustomer() {
        // Step 1: Create second customer
        CustomerEntity customer2 = new CustomerEntity(
                "test-customer-2@example.com",
                "Jane",
                "Smith",
                "987654321"
        );
        customer2 = customerRepository.save(customer2);

        SubscriptionEntity subscription2 = new SubscriptionEntity(
                customer2,
                voiceProduct,
                SubscriptionStatus.ACTIVE
        );
        subscription2 = subscriptionRepository.save(subscription2);

        // Step 2: Ingest usage for both customers
        IngestUsageRecordCommand usage1 = new IngestUsageRecordCommand(
                voiceSubscription.getId().toString(),
                "VOICE_CALL",
                "MINUTES",
                BigDecimal.valueOf(10),
                LocalDate.of(2024, 1, 15),
                LocalTime.of(10, 0),
                "MOBILE",
                "+1234567890",
                "US",
                "PEAK",
                "CDR_001",
                "CDR"
        );
        ingestUsageRecordUseCase.handle(usage1);

        IngestUsageRecordCommand usage2 = new IngestUsageRecordCommand(
                subscription2.getId().toString(),
                "VOICE_CALL",
                "MINUTES",
                BigDecimal.valueOf(20),
                LocalDate.of(2024, 1, 15),
                LocalTime.of(11, 0),
                "MOBILE",
                "+1234567891",
                "US",
                "PEAK",
                "CDR_001",
                "CDR"
        );
        ingestUsageRecordUseCase.handle(usage2);

        // Step 3: Process billing cycle for first customer
        StartBillingCycleCommand cycle1 = new StartBillingCycleCommand(
                customer.getId().toString(),
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 31),
                LocalDate.of(2024, 2, 1),
                "MONTHLY"
        );
        BillingCycleEntity processedCycle1 = processBillingCycleUseCase.handle(
                startBillingCycleUseCase.handle(cycle1).getId()
        );

        // Step 4: Process billing cycle for second customer
        StartBillingCycleCommand cycle2 = new StartBillingCycleCommand(
                customer2.getId().toString(),
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 31),
                LocalDate.of(2024, 2, 1),
                "MONTHLY"
        );
        BillingCycleEntity processedCycle2 = processBillingCycleUseCase.handle(
                startBillingCycleUseCase.handle(cycle2).getId()
        );

        // Step 5: Verify separate invoices
        assertEquals(1, processedCycle1.getInvoices().size());
        assertEquals(1, processedCycle2.getInvoices().size());
        assertNotEquals(processedCycle1.getInvoices().get(0).getId(),
                       processedCycle2.getInvoices().get(0).getId());
    }

    private List<IngestUsageRecordCommand> createMockCDRRecords() {
        return createMockCDRRecordsForPeriod(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31));
    }

    private List<IngestUsageRecordCommand> createMockCDRRecordsForPeriod(LocalDate start, LocalDate end) {
        return IntStream.range(0, 50)
                .mapToObj(i -> {
                    LocalDate usageDate = start.plusDays(ThreadLocalRandom.current().nextInt(0, (int) (end.toEpochDay() - start.toEpochDay() + 1)));
                    return new IngestUsageRecordCommand(
                            i % 2 == 0 ? voiceSubscription.getId().toString() : dataSubscription.getId().toString(),
                            i % 2 == 0 ? "VOICE_CALL" : "DATA",
                            i % 2 == 0 ? "MINUTES" : "MB",
                            BigDecimal.valueOf(ThreadLocalRandom.current().nextInt(1, 20)),
                            usageDate,
                            LocalTime.of(ThreadLocalRandom.current().nextInt(0, 24), 0),
                            "MOBILE",
                            "+123456789" + i,
                            "US",
                            ThreadLocalRandom.current().nextInt(0, 2) == 0 ? "PEAK" : "OFF_PEAK",
                            "CDR_BATCH_" + (i / 10),
                            "CDR"
                    );
                })
                .toList();
    }
}
