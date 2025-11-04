package com.droid.bss.application.command.invoice;

import com.droid.bss.application.dto.invoice.CreateInvoiceCommand;
import com.droid.bss.application.dto.invoice.InvoiceResponse;
import com.droid.bss.domain.customer.CustomerEntity;
import com.droid.bss.domain.customer.CustomerEntityRepository;
import com.droid.bss.domain.invoice.*;
import com.droid.bss.domain.invoice.event.InvoiceEventPublisher;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

/**
 * Use case for creating a new invoice
 */
@Service
@Transactional
public class CreateInvoiceUseCase {

    private final InvoiceRepository invoiceRepository;
    private final CustomerEntityRepository customerEntityRepository;
    private final InvoiceEventPublisher eventPublisher;

    public CreateInvoiceUseCase(
            InvoiceRepository invoiceRepository,
            CustomerEntityRepository customerEntityRepository,
            InvoiceEventPublisher eventPublisher) {
        this.invoiceRepository = invoiceRepository;
        this.customerEntityRepository = customerEntityRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Execute the create invoice use case
     */
    public InvoiceResponse execute(@Valid CreateInvoiceCommand command) {
        // Validate customer exists
        CustomerEntity customer = customerEntityRepository.findById(UUID.fromString(command.customerId()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Customer not found with id: " + command.customerId()
                ));

        // Check if invoice number is unique
        Optional<InvoiceEntity> existingInvoice = invoiceRepository.findByInvoiceNumber(command.invoiceNumber());
        if (existingInvoice.isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Invoice with number " + command.invoiceNumber() + " already exists"
            );
        }

        // Create invoice entity
        InvoiceEntity invoice = new InvoiceEntity();
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
        invoice.setBillingPeriodStart(command.billingPeriodStart());
        invoice.setBillingPeriodEnd(command.billingPeriodEnd());
        invoice.setStatus(InvoiceStatus.DRAFT);

        // Save invoice
        InvoiceEntity savedInvoice = invoiceRepository.save(invoice);

        // Publish event
        eventPublisher.publishInvoiceCreated(savedInvoice);

        // Return response
        return InvoiceResponse.from(savedInvoice);
    }
}
