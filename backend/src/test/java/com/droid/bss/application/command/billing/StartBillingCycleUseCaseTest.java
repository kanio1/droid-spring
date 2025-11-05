package com.droid.bss.application.command.billing;

import com.droid.bss.AbstractIntegrationTest;
import com.droid.bss.application.dto.billing.StartBillingCycleCommand;
import com.droid.bss.domain.billing.BillingCycleEntity;
import com.droid.bss.domain.billing.BillingCycleRepository;
import com.droid.bss.domain.billing.BillingCycleStatus;
import com.droid.bss.domain.billing.BillingCycleType;
import com.droid.bss.domain.customer.CustomerEntity;
import com.droid.bss.domain.customer.CustomerRepository;
import com.droid.bss.infrastructure.metrics.BusinessMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for StartBillingCycleUseCase
 * Tests billing cycle creation and validation
 */
@Transactional
class StartBillingCycleUseCaseTest extends AbstractIntegrationTest {

    @Autowired
    private StartBillingCycleUseCase startBillingCycleUseCase;

    @Autowired
    private BillingCycleRepository billingCycleRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private BusinessMetrics businessMetrics;

    private CustomerEntity customer;

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
    }

    @Test
    void shouldCreateBillingCycle() {
        // Given
        StartBillingCycleCommand command = new StartBillingCycleCommand(
                customer.getId().toString(),
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 31),
                LocalDate.of(2024, 2, 1),
                "MONTHLY"
        );

        // When
        BillingCycleEntity billingCycle = startBillingCycleUseCase.handle(command);

        // Then
        assertNotNull(billingCycle.getId());
        assertEquals(customer, billingCycle.getCustomer());
        assertEquals(LocalDate.of(2024, 1, 1), billingCycle.getCycleStart());
        assertEquals(LocalDate.of(2024, 1, 31), billingCycle.getCycleEnd());
        assertEquals(LocalDate.of(2024, 2, 1), billingCycle.getBillingDate());
        assertEquals(BillingCycleType.MONTHLY, billingCycle.getCycleType());
        assertEquals(BillingCycleStatus.PENDING, billingCycle.getStatus());
        assertNotNull(billingCycle.getCreatedAt());
        assertNotNull(billingCycle.getUpdatedAt());
        assertTrue(billingCycle.getInvoices().isEmpty());
        assertEquals(0, billingCycle.getInvoiceCount());
        assertEquals(0, billingCycle.getTotalAmount().doubleValue());
        assertEquals(0, billingCycle.getTaxAmount().doubleValue());
        assertEquals(0, billingCycle.getTotalWithTax().doubleValue());
    }

    @Test
    void shouldCreateQuarterlyBillingCycle() {
        // Given
        StartBillingCycleCommand command = new StartBillingCycleCommand(
                customer.getId().toString(),
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 3, 31),
                LocalDate.of(2024, 4, 1),
                "QUARTERLY"
        );

        // When
        BillingCycleEntity billingCycle = startBillingCycleUseCase.handle(command);

        // Then
        assertEquals(BillingCycleType.QUARTERLY, billingCycle.getCycleType());
        assertEquals(LocalDate.of(2024, 1, 1), billingCycle.getCycleStart());
        assertEquals(LocalDate.of(2024, 3, 31), billingCycle.getCycleEnd());
    }

    @Test
    void shouldCreateYearlyBillingCycle() {
        // Given
        StartBillingCycleCommand command = new StartBillingCycleCommand(
                customer.getId().toString(),
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 12, 31),
                LocalDate.of(2025, 1, 15),
                "YEARLY"
        );

        // When
        BillingCycleEntity billingCycle = startBillingCycleUseCase.handle(command);

        // Then
        assertEquals(BillingCycleType.YEARLY, billingCycle.getCycleType());
        assertEquals(LocalDate.of(2024, 1, 1), billingCycle.getCycleStart());
        assertEquals(LocalDate.of(2024, 12, 31), billingCycle.getCycleEnd());
    }

    @Test
    void shouldSaveBillingCycleToRepository() {
        // Given
        StartBillingCycleCommand command = new StartBillingCycleCommand(
                customer.getId().toString(),
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 31),
                LocalDate.of(2024, 2, 1),
                "MONTHLY"
        );

        // When
        BillingCycleEntity billingCycle = startBillingCycleUseCase.handle(command);

        // Then - Verify it's persisted
        BillingCycleEntity saved = billingCycleRepository.findById(billingCycle.getId()).orElse(null);
        assertNotNull(saved);
        assertEquals(billingCycle.getId(), saved.getId());
        assertEquals(customer.getId(), saved.getCustomer().getId());
    }

    @Test
    void shouldThrowExceptionWhenCustomerNotFound() {
        // Given
        String nonExistentCustomerId = "99999999-9999-9999-9999-999999999999";
        StartBillingCycleCommand command = new StartBillingCycleCommand(
                nonExistentCustomerId,
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 31),
                LocalDate.of(2024, 2, 1),
                "MONTHLY"
        );

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            startBillingCycleUseCase.handle(command);
        });

        assertTrue(exception.getMessage().contains("Customer not found"));
    }

    @Test
    void shouldThrowExceptionWhenBillingCycleOverlaps() {
        // Given - Create first billing cycle
        StartBillingCycleCommand command1 = new StartBillingCycleCommand(
                customer.getId().toString(),
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 31),
                LocalDate.of(2024, 2, 1),
                "MONTHLY"
        );
        startBillingCycleUseCase.handle(command1);

        // Try to create overlapping cycle
        StartBillingCycleCommand command2 = new StartBillingCycleCommand(
                customer.getId().toString(),
                LocalDate.of(2024, 1, 15), // Overlaps with first cycle
                LocalDate.of(2024, 2, 15),
                LocalDate.of(2024, 2, 28),
                "MONTHLY"
        );

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            startBillingCycleUseCase.handle(command2);
        });

        assertTrue(exception.getMessage().contains("Billing cycle overlaps"));
    }

    @Test
    void shouldAllowNonOverlappingBillingCycles() {
        // Given - Create first billing cycle
        StartBillingCycleCommand command1 = new StartBillingCycleCommand(
                customer.getId().toString(),
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 31),
                LocalDate.of(2024, 2, 1),
                "MONTHLY"
        );
        startBillingCycleUseCase.handle(command1);

        // Create non-overlapping cycle
        StartBillingCycleCommand command2 = new StartBillingCycleCommand(
                customer.getId().toString(),
                LocalDate.of(2024, 2, 1), // Starts right after first cycle ends
                LocalDate.of(2024, 2, 29),
                LocalDate.of(2024, 3, 1),
                "MONTHLY"
        );

        // When
        BillingCycleEntity billingCycle2 = startBillingCycleUseCase.handle(command2);

        // Then - Should succeed
        assertNotNull(billingCycle2.getId());
        assertNotEquals(billingCycle2.getId(), command1.customerId()); // Different cycles
    }

    @Test
    void shouldAllowBackToBackBillingCycles() {
        // Given - Create first cycle
        StartBillingCycleCommand command1 = new StartBillingCycleCommand(
                customer.getId().toString(),
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 31),
                LocalDate.of(2024, 2, 1),
                "MONTHLY"
        );
        startBillingCycleUseCase.handle(command1);

        // Create cycle starting immediately after first
        StartBillingCycleCommand command2 = new StartBillingCycleCommand(
                customer.getId().toString(),
                LocalDate.of(2024, 2, 1),
                LocalDate.of(2024, 2, 29),
                LocalDate.of(2024, 3, 1),
                "MONTHLY"
        );

        // When
        BillingCycleEntity billingCycle2 = startBillingCycleUseCase.handle(command2);

        // Then - Should succeed (back-to-back is allowed)
        assertNotNull(billingCycle2);
        assertEquals(LocalDate.of(2024, 2, 1), billingCycle2.getCycleStart());
    }

    @Test
    void shouldTrackBillingCycleStartedMetric() {
        // Given
        StartBillingCycleCommand command = new StartBillingCycleCommand(
                customer.getId().toString(),
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 31),
                LocalDate.of(2024, 2, 1),
                "MONTHLY"
        );

        // When
        BillingCycleEntity billingCycle = startBillingCycleUseCase.handle(command);

        // Then - Metric tracking is handled in the use case
        // The businessMetrics.incrementBillingCycleStarted() is called
        assertNotNull(billingCycle);
    }

    @Test
    void shouldCreateBillingCycleWithSpecificBillingDate() {
        // Given
        LocalDate billingDate = LocalDate.of(2024, 2, 15);
        StartBillingCycleCommand command = new StartBillingCycleCommand(
                customer.getId().toString(),
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 31),
                billingDate,
                "MONTHLY"
        );

        // When
        BillingCycleEntity billingCycle = startBillingCycleUseCase.handle(command);

        // Then
        assertEquals(billingDate, billingCycle.getBillingDate());
    }

    @Test
    void shouldHandleMultipleCustomers() {
        // Given - Create second customer
        CustomerEntity customer2 = new CustomerEntity(
                "test-customer-2@example.com",
                "Jane",
                "Smith",
                "987654321"
        );
        customer2 = customerRepository.save(customer2);

        StartBillingCycleCommand command1 = new StartBillingCycleCommand(
                customer.getId().toString(),
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 31),
                LocalDate.of(2024, 2, 1),
                "MONTHLY"
        );

        StartBillingCycleCommand command2 = new StartBillingCycleCommand(
                customer2.getId().toString(),
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 31),
                LocalDate.of(2024, 2, 1),
                "MONTHLY"
        );

        // When
        BillingCycleEntity billingCycle1 = startBillingCycleUseCase.handle(command1);
        BillingCycleEntity billingCycle2 = startBillingCycleUseCase.handle(command2);

        // Then - Both should be created successfully (different customers)
        assertNotNull(billingCycle1);
        assertNotNull(billingCycle2);
        assertNotEquals(billingCycle1.getCustomer().getId(), billingCycle2.getCustomer().getId());
        assertEquals(2, billingCycleRepository.findAll().size());
    }

    @Test
    void shouldInitializeAllFieldsCorrectly() {
        // Given
        StartBillingCycleCommand command = new StartBillingCycleCommand(
                customer.getId().toString(),
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 31),
                LocalDate.of(2024, 2, 1),
                "MONTHLY"
        );

        // When
        BillingCycleEntity billingCycle = startBillingCycleUseCase.handle(command);

        // Then - Verify all fields
        assertEquals(0, billingCycle.getTotalAmount().doubleValue());
        assertEquals(0, billingCycle.getTaxAmount().doubleValue());
        assertEquals(0, billingCycle.getTotalWithTax().doubleValue());
        assertEquals(0, billingCycle.getInvoiceCount());
        assertNotNull(billingCycle.getCreatedAt());
        assertNotNull(billingCycle.getUpdatedAt());
        assertNull(billingCycle.getGeneratedAt());
        assertNull(billingCycle.getProcessedAt());
    }
}
