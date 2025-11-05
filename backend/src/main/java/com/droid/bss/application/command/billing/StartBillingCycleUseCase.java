package com.droid.bss.application.command.billing;

import com.droid.bss.application.dto.billing.StartBillingCycleCommand;
import com.droid.bss.domain.billing.BillingCycleEntity;
import com.droid.bss.domain.billing.BillingCycleRepository;
import com.droid.bss.domain.billing.BillingCycleType;
import com.droid.bss.domain.customer.Customer;
import com.droid.bss.domain.customer.CustomerEntity;
import com.droid.bss.domain.customer.CustomerId;
import com.droid.bss.domain.customer.CustomerRepository;
import com.droid.bss.infrastructure.metrics.BusinessMetrics;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case for starting a billing cycle
 */
@Service
public class StartBillingCycleUseCase {

    private final BillingCycleRepository billingCycleRepository;
    private final CustomerRepository customerRepository;
    private final BusinessMetrics businessMetrics;

    public StartBillingCycleUseCase(
            BillingCycleRepository billingCycleRepository,
            CustomerRepository customerRepository,
            BusinessMetrics businessMetrics) {
        this.billingCycleRepository = billingCycleRepository;
        this.customerRepository = customerRepository;
        this.businessMetrics = businessMetrics;
    }

    @Transactional
    public BillingCycleEntity handle(StartBillingCycleCommand command) {
        // Get customer
        Customer customer = customerRepository.findById(CustomerId.of(command.customerId()))
                .orElseThrow(() -> new RuntimeException("Customer not found: " + command.customerId()));

        // Check for overlapping cycles
        var existingCycles = billingCycleRepository.findByCycleOverlap(command.cycleStart(), command.cycleEnd());
        if (!existingCycles.isEmpty()) {
            throw new IllegalStateException("Billing cycle overlaps with existing cycle for customer: " + command.customerId());
        }

        // Create billing cycle
        BillingCycleEntity billingCycle = new BillingCycleEntity(
                CustomerEntity.from(customer),
                command.cycleStart(),
                command.cycleEnd(),
                command.billingDate(),
                BillingCycleType.valueOf(command.cycleType())
        );

        billingCycle.setStatus(com.droid.bss.domain.billing.BillingCycleStatus.PENDING);

        // Save
        BillingCycleEntity saved = billingCycleRepository.save(billingCycle);

        // Track metrics
        businessMetrics.incrementBillingCycleStarted();

        return saved;
    }
}
