package com.droid.bss.application.command.billing;

import com.droid.bss.domain.billing.*;
import com.droid.bss.domain.invoice.InvoiceEntity;
import com.droid.bss.domain.invoice.InvoiceItemEntity;
import com.droid.bss.domain.invoice.InvoiceItemType;
import com.droid.bss.domain.invoice.InvoiceRepository;
import com.droid.bss.domain.invoice.InvoiceStatus;
import com.droid.bss.domain.invoice.InvoiceType;
import com.droid.bss.infrastructure.metrics.BusinessMetrics;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Use case for processing a billing cycle
 */
@Service
public class ProcessBillingCycleUseCase {

    private final BillingCycleRepository billingCycleRepository;
    private final UsageRecordRepository usageRecordRepository;
    private final InvoiceRepository invoiceRepository;
    private final RatingEngine ratingEngine;
    private final BusinessMetrics businessMetrics;

    public ProcessBillingCycleUseCase(
            BillingCycleRepository billingCycleRepository,
            UsageRecordRepository usageRecordRepository,
            InvoiceRepository invoiceRepository,
            RatingEngine ratingEngine,
            BusinessMetrics businessMetrics) {
        this.billingCycleRepository = billingCycleRepository;
        this.usageRecordRepository = usageRecordRepository;
        this.invoiceRepository = invoiceRepository;
        this.ratingEngine = ratingEngine;
        this.businessMetrics = businessMetrics;
    }

    @Transactional
    public BillingCycleEntity handle(String billingCycleId) {
        Timer.Sample sample = businessMetrics.startBillingCycleProcessing();

        try {
            // Get billing cycle
            BillingCycleEntity billingCycle = billingCycleRepository.findById(billingCycleId)
                    .orElseThrow(() -> new RuntimeException("Billing cycle not found: " + billingCycleId));

            if (!billingCycle.isPending()) {
                throw new IllegalStateException("Billing cycle is not pending: " + billingCycleId);
            }

            // Mark as generated
            billingCycle.setStatus(BillingCycleStatus.GENERATED);
            billingCycle.setGeneratedAt(java.time.LocalDateTime.now());

        // Rate all unrated usage for this cycle period
        var unrated = usageRecordRepository.findByDateRange(billingCycle.getCycleStart(), billingCycle.getCycleEnd());
        for (UsageRecordEntity usage : unrated) {
            if (!usage.isRated()) {
                try {
                    ratingEngine.rateUsageRecord(usage);
                } catch (Exception e) {
                    System.err.println("Failed to rate usage record: " + usage.getId() + " - " + e.getMessage());
                }
            }
        }

        // Get rated usage records
        var ratedUsage = usageRecordRepository.findByDateRange(billingCycle.getCycleStart(), billingCycle.getCycleEnd())
                .stream()
                .filter(UsageRecordEntity::isRated)
                .collect(Collectors.groupingBy(ur -> ur.getSubscription().getCustomer().getId()));

        // Create invoices for each customer
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal taxAmount = BigDecimal.ZERO;

        for (var customerId : ratedUsage.keySet()) {
            List<UsageRecordEntity> customerUsage = ratedUsage.get(customerId);
            BigDecimal customerTotal = customerUsage.stream()
                    .map(ur -> ur.getTotalAmount() != null ? ur.getTotalAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            totalAmount = totalAmount.add(customerTotal);
            taxAmount = taxAmount.add(customerTotal.multiply(new BigDecimal("0.23"))); // 23% VAT

            // Create invoice
            InvoiceEntity invoice = new InvoiceEntity();
            invoice.setInvoiceNumber("INV-" + System.currentTimeMillis());
            invoice.setCustomer(customerUsage.get(0).getSubscription().getCustomer());
            invoice.setInvoiceType(InvoiceType.USAGE);
            invoice.setStatus(InvoiceStatus.DRAFT);
            invoice.setBillingPeriodStart(billingCycle.getCycleStart());
            invoice.setBillingPeriodEnd(billingCycle.getCycleEnd());
            invoice.setBillingDate(billingCycle.getBillingDate());
            invoice.setDueDate(billingCycle.getBillingDate().plusDays(14));
            invoice.setTotalAmount(customerTotal);
            invoice.setTaxAmount(customerTotal.multiply(new BigDecimal("0.23")));
            invoice.setTotalWithTax(customerTotal.add(customerTotal.multiply(new BigDecimal("0.23"))));

            invoice = invoiceRepository.save(invoice);

            // Add invoice items
            for (UsageRecordEntity usage : customerUsage) {
                InvoiceItemEntity item = new InvoiceItemEntity();
                item.setInvoice(invoice);
                item.setItemType(InvoiceItemType.USAGE);
                item.setDescription(usage.getUsageType().getDescription() + " - " + usage.getUsageDate());
                item.setQuantity(BigDecimal.ONE);
                item.setUnitPrice(usage.getTotalAmount() != null ? usage.getTotalAmount() : BigDecimal.ZERO);
                item.setTotalPrice(usage.getTotalAmount() != null ? usage.getTotalAmount() : BigDecimal.ZERO);

                invoice.getItems().add(item);
            }

            invoiceRepository.save(invoice);
            billingCycle.addInvoice(invoice);

            // Track invoice creation
            businessMetrics.incrementInvoiceCreated();
        }

        // Update cycle totals
        billingCycle.setTotalAmount(totalAmount);
        billingCycle.setTaxAmount(taxAmount);
        billingCycle.setTotalWithTax(totalAmount.add(taxAmount));
        billingCycle.setInvoiceCount(billingCycle.getInvoices().size());

        // Mark as processed
        billingCycle.setStatus(BillingCycleStatus.PROCESSED);
        billingCycle.setProcessedAt(java.time.LocalDateTime.now());

        BillingCycleEntity saved = billingCycleRepository.save(billingCycle);

        businessMetrics.recordBillingCycleProcessing(sample);
        businessMetrics.incrementBillingCycleProcessed();

        return saved;
        } catch (Exception e) {
            businessMetrics.recordBillingCycleProcessing(sample);
            throw e;
        }
    }
}
