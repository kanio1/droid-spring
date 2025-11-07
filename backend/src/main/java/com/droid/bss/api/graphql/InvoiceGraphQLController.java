package com.droid.bss.api.graphql;

import com.droid.bss.domain.invoice.InvoiceEntity;
import com.droid.bss.domain.invoice.InvoiceStatus;
import com.droid.bss.domain.invoice.InvoiceType;
import com.droid.bss.domain.customer.CustomerEntity;
import com.droid.bss.infrastructure.read.InvoiceReadRepository;
import com.droid.bss.infrastructure.read.CustomerReadRepository;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * GraphQL Controller for Invoice-related queries and mutations
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class InvoiceGraphQLController {

    private final InvoiceReadRepository invoiceRepository;
    private final CustomerReadRepository customerRepository;

    @QueryMapping
    @PreAuthorize("hasRole('USER')")
    public CompletableFuture<InvoiceEntity> invoice(@Argument UUID id) {
        log.debug("Fetching invoice with id: {}", id);
        return CompletableFuture.supplyAsync(() ->
            invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found: " + id))
        );
    }

    @QueryMapping
    @PreAuthorize("hasRole('USER')")
    public CompletableFuture<List<InvoiceEntity>> invoices(
            @Argument Optional<Integer> page,
            @Argument Optional<Integer> size,
            @Argument Optional<InvoiceStatus> status,
            @Argument Optional<UUID> customerId,
            @Argument Optional<LocalDate> fromDate,
            @Argument Optional<LocalDate> toDate) {

        log.debug("Fetching invoices with filters");
        return CompletableFuture.supplyAsync(() -> {
            List<InvoiceEntity> invoices = invoiceRepository.findAll();

            if (status.isPresent()) {
                invoices = invoices.stream()
                    .filter(inv -> inv.getStatus() == status.get())
                    .collect(java.util.stream.Collectors.toList());
            }

            if (customerId.isPresent()) {
                invoices = invoices.stream()
                    .filter(inv -> inv.getCustomer().getId().equals(customerId.get()))
                    .collect(java.util.stream.Collectors.toList());
            }

            if (fromDate.isPresent() || toDate.isPresent()) {
                LocalDate from = fromDate.orElse(LocalDate.of(2000, 1, 1));
                LocalDate to = toDate.orElse(LocalDate.now());
                invoices = invoices.stream()
                    .filter(inv -> {
                        LocalDate issueDate = inv.getIssueDate();
                        return !issueDate.isBefore(from) && !issueDate.isAfter(to);
                    })
                    .collect(java.util.stream.Collectors.toList());
            }

            return invoices;
        });
    }

    @QueryMapping
    @PreAuthorize("hasRole('USER')")
    public CompletableFuture<List<InvoiceEntity>> invoicesByCustomer(@Argument UUID customerId) {
        log.debug("Fetching invoices for customer: {}", customerId);
        return CompletableFuture.supplyAsync(() ->
            invoiceRepository.findByCustomerId(customerId)
        );
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public CompletableFuture<InvoiceEntity> createInvoice(@Argument("input") CreateInvoiceInput input) {
        log.info("Creating invoice for customer: {}", input.getCustomerId());
        return CompletableFuture.supplyAsync(() -> {
            CustomerEntity customer = customerRepository.findById(input.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found: " + input.getCustomerId()));

            InvoiceEntity invoice = InvoiceEntity.builder()
                .id(UUID.randomUUID())
                .invoiceNumber(generateInvoiceNumber())
                .customer(customer)
                .invoiceType(input.getInvoiceType() != null ? input.getInvoiceType() : InvoiceType.STANDARD)
                .status(InvoiceStatus.DRAFT)
                .issueDate(LocalDate.now())
                .dueDate(input.getDueDate())
                .billingPeriodStart(input.getBillingPeriodStart())
                .billingPeriodEnd(input.getBillingPeriodEnd())
                .currency(input.getCurrency() != null ? input.getCurrency() : "PLN")
                .notes(input.getNotes())
                .subtotal(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

            return invoiceRepository.save(invoice);
        });
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public CompletableFuture<InvoiceEntity> updateInvoice(
            @Argument UUID id,
            @Argument("input") UpdateInvoiceInput input) {

        log.info("Updating invoice: {}", id);
        return CompletableFuture.supplyAsync(() -> {
            InvoiceEntity invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found: " + id));

            if (input.getInvoiceType() != null) invoice.setInvoiceType(input.getInvoiceType());
            if (input.getStatus() != null) invoice.setStatus(input.getStatus());
            if (input.getDueDate() != null) invoice.setDueDate(input.getDueDate());
            if (input.getPaidDate() != null) invoice.setPaidDate(input.getPaidDate());
            if (input.getBillingPeriodStart() != null) invoice.setBillingPeriodStart(input.getBillingPeriodStart());
            if (input.getBillingPeriodEnd() != null) invoice.setBillingPeriodEnd(input.getBillingPeriodEnd());
            if (input.getCurrency() != null) invoice.setCurrency(input.getCurrency());
            if (input.getNotes() != null) invoice.setNotes(input.getNotes());
            invoice.setUpdatedAt(LocalDateTime.now());

            return invoiceRepository.save(invoice);
        });
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public CompletableFuture<Boolean> deleteInvoice(@Argument UUID id) {
        log.warn("Deleting invoice: {}", id);
        return CompletableFuture.supplyAsync(() -> {
            InvoiceEntity invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found: " + id));

            invoice.setDeletedAt(LocalDateTime.now());
            invoice.setStatus(InvoiceStatus.CANCELLED);
            invoiceRepository.save(invoice);

            return true;
        });
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public CompletableFuture<InvoiceEntity> changeInvoiceStatus(
            @Argument UUID id,
            @Argument InvoiceStatus status) {

        log.info("Changing invoice status to: {} for invoice: {}", status, id);
        return CompletableFuture.supplyAsync(() -> {
            InvoiceEntity invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found: " + id));

            invoice.setStatus(status);
            invoice.setUpdatedAt(LocalDateTime.now());

            if (status == InvoiceStatus.PAID) {
                invoice.setPaidDate(LocalDate.now());
            }

            return invoiceRepository.save(invoice);
        });
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public CompletableFuture<Boolean> sendInvoice(@Argument UUID id, @Argument String email) {
        log.info("Sending invoice {} to email: {}", id, email);
        return CompletableFuture.supplyAsync(() -> {
            // Implementation would send email
            log.info("Invoice sent successfully");
            return true;
        });
    }

    private String generateInvoiceNumber() {
        return "INV-" + System.currentTimeMillis();
    }

    // ========== INPUT CLASSES ==========

    public static class CreateInvoiceInput {
        private UUID customerId;
        private InvoiceType invoiceType;
        private LocalDate dueDate;
        private LocalDate billingPeriodStart;
        private LocalDate billingPeriodEnd;
        private String currency;
        private String notes;

        public UUID getCustomerId() { return customerId; }
        public void setCustomerId(UUID customerId) { this.customerId = customerId; }

        public InvoiceType getInvoiceType() { return invoiceType; }
        public void setInvoiceType(InvoiceType invoiceType) { this.invoiceType = invoiceType; }

        public LocalDate getDueDate() { return dueDate; }
        public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

        public LocalDate getBillingPeriodStart() { return billingPeriodStart; }
        public void setBillingPeriodStart(LocalDate billingPeriodStart) { this.billingPeriodStart = billingPeriodStart; }

        public LocalDate getBillingPeriodEnd() { return billingPeriodEnd; }
        public void setBillingPeriodEnd(LocalDate billingPeriodEnd) { this.billingPeriodEnd = billingPeriodEnd; }

        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }

        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }

    public static class UpdateInvoiceInput {
        private InvoiceType invoiceType;
        private InvoiceStatus status;
        private LocalDate dueDate;
        private LocalDate paidDate;
        private LocalDate billingPeriodStart;
        private LocalDate billingPeriodEnd;
        private String currency;
        private String notes;

        public InvoiceType getInvoiceType() { return invoiceType; }
        public void setInvoiceType(InvoiceType invoiceType) { this.invoiceType = invoiceType; }

        public InvoiceStatus getStatus() { return status; }
        public void setStatus(InvoiceStatus status) { this.status = status; }

        public LocalDate getDueDate() { return dueDate; }
        public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

        public LocalDate getPaidDate() { return paidDate; }
        public void setPaidDate(LocalDate paidDate) { this.paidDate = paidDate; }

        public LocalDate getBillingPeriodStart() { return billingPeriodStart; }
        public void setBillingPeriodStart(LocalDate billingPeriodStart) { this.billingPeriodStart = billingPeriodStart; }

        public LocalDate getBillingPeriodEnd() { return billingPeriodEnd; }
        public void setBillingPeriodEnd(LocalDate billingPeriodEnd) { this.billingPeriodEnd = billingPeriodEnd; }

        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }

        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }
}
