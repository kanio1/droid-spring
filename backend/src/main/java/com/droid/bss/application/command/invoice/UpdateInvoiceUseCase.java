package com.droid.bss.application.command.invoice;

import com.droid.bss.application.dto.invoice.InvoiceResponse;
import com.droid.bss.application.dto.invoice.UpdateInvoiceCommand;
import com.droid.bss.domain.customer.CustomerEntity;
import com.droid.bss.domain.customer.CustomerEntityRepository;
import com.droid.bss.domain.invoice.InvoiceEntity;
import com.droid.bss.domain.invoice.InvoiceRepository;
import com.droid.bss.domain.invoice.event.InvoiceEventPublisher;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Use case for updating an existing invoice
 */
@Service
@Transactional
public class UpdateInvoiceUseCase {

    private final InvoiceRepository invoiceRepository;
    private final CustomerEntityRepository customerEntityRepository;
    private final InvoiceEventPublisher eventPublisher;

    public UpdateInvoiceUseCase(
            InvoiceRepository invoiceRepository,
            CustomerEntityRepository customerEntityRepository,
            InvoiceEventPublisher eventPublisher) {
        this.invoiceRepository = invoiceRepository;
        this.customerEntityRepository = customerEntityRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Execute the update invoice use case
     */
    public InvoiceResponse execute(@Valid UpdateInvoiceCommand command) {
        // Find invoice by ID or throw 404
        InvoiceEntity invoice = invoiceRepository.findById(command.id())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Invoice not found with id: " + command.id()
                ));

        // Check version for optimistic locking
        if (invoice.getVersion() != null && !invoice.getVersion().equals(command.version())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Invoice has been modified by another process. Please refresh and try again."
            );
        }

        // Validate customer exists
        CustomerEntity customer = customerEntityRepository.findById(UUID.fromString(command.customerId()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Customer not found with id: " + command.customerId()
                ));

        // Validate business rules
        validateBusinessRules(command, invoice);

        // Update invoice fields
        updateInvoiceFields(invoice, command, customer);

        // Save updated invoice
        InvoiceEntity savedInvoice = invoiceRepository.save(invoice);

        // Publish event
        eventPublisher.publishInvoiceUpdated(savedInvoice);

        // Return response
        return InvoiceResponse.from(savedInvoice);
    }

    /**
     * Validate business rules for invoice update
     */
    private void validateBusinessRules(UpdateInvoiceCommand command, InvoiceEntity currentInvoice) {
        // Due date must be after issue date
        if (command.dueDate().isBefore(command.issueDate())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Due date must be after issue date"
            );
        }

        // Validate amounts are non-negative
        if (command.subtotal().compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Subtotal must be greater than or equal to 0"
            );
        }

        if (command.taxAmount().compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Tax amount must be greater than or equal to 0"
            );
        }

        if (command.totalAmount().compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Total amount must be greater than or equal to 0"
            );
        }

        // Validate currency is 3 characters
        if (command.currency() != null && command.currency().length() != 3) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Currency must be exactly 3 characters (ISO 4217)"
            );
        }
    }

    /**
     * Update invoice fields from command
     */
    private void updateInvoiceFields(InvoiceEntity invoice, UpdateInvoiceCommand command, CustomerEntity customer) {
        invoice.setInvoiceNumber(command.invoiceNumber());
        invoice.setCustomer(customer);
        invoice.setInvoiceType(command.invoiceType());
        invoice.setIssueDate(command.issueDate());
        invoice.setDueDate(command.dueDate());
        invoice.setPaidDate(command.paidDate());
        invoice.setBillingPeriodStart(command.billingPeriodStart());
        invoice.setBillingPeriodEnd(command.billingPeriodEnd());
        invoice.setSubtotal(command.subtotal());
        invoice.setDiscountAmount(command.discountAmount() != null ? command.discountAmount() : java.math.BigDecimal.ZERO);
        invoice.setTaxAmount(command.taxAmount());
        invoice.setTotalAmount(command.totalAmount());
        invoice.setCurrency(command.currency());
        invoice.setPaymentTerms(command.paymentTerms());
        invoice.setLateFee(command.lateFee() != null ? command.lateFee() : java.math.BigDecimal.ZERO);
        invoice.setNotes(command.notes());
        invoice.setPdfUrl(command.pdfUrl());
    }
}
