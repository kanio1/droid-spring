package com.droid.bss.application.command.payment;

import com.droid.bss.application.dto.payment.CreatePaymentCommand;
import com.droid.bss.domain.customer.CustomerId;
import com.droid.bss.domain.customer.CustomerEntity;
import com.droid.bss.domain.customer.CustomerEntityRepository;
import com.droid.bss.domain.invoice.InvoiceId;
import com.droid.bss.domain.invoice.InvoiceEntity;
import com.droid.bss.domain.invoice.repository.InvoiceEntityRepository;
import com.droid.bss.domain.payment.Payment;
import com.droid.bss.domain.payment.PaymentEntity;
import com.droid.bss.domain.payment.PaymentId;
import com.droid.bss.domain.payment.PaymentMethod;
import com.droid.bss.domain.payment.repository.PaymentRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@Transactional
public class CreatePaymentUseCase {

    private final PaymentRepository paymentRepository;
    private final CustomerEntityRepository customerEntityRepository;
    private final InvoiceEntityRepository invoiceEntityRepository;

    public CreatePaymentUseCase(
            PaymentRepository paymentRepository,
            CustomerEntityRepository customerEntityRepository,
            InvoiceEntityRepository invoiceEntityRepository) {
        this.paymentRepository = paymentRepository;
        this.customerEntityRepository = customerEntityRepository;
        this.invoiceEntityRepository = invoiceEntityRepository;
    }

    public UUID handle(CreatePaymentCommand command) {
        // Validate customer exists
        CustomerId customerId = new CustomerId(UUID.fromString(command.customerId()));
        CustomerEntity customer = customerEntityRepository.findById(customerId.value())
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + command.customerId()));

        // Validate invoice if provided
        InvoiceEntity invoice = null;
        if (command.invoiceId() != null) {
            InvoiceId invoiceId = new InvoiceId(UUID.fromString(command.invoiceId()));
            invoice = invoiceEntityRepository.findById(invoiceId.value())
                    .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + command.invoiceId()));
        }

        // Generate payment number (format: PAY-YYYYMMDD-XXXX)
        String paymentNumber = generatePaymentNumber();

        // Create payment entity (using JPA entity for persistence)
        PaymentEntity paymentEntity = new PaymentEntity();
        paymentEntity.setPaymentNumber(paymentNumber);
        paymentEntity.setCustomer(customer);
        paymentEntity.setInvoice(invoice);
        paymentEntity.setAmount(command.amount());
        paymentEntity.setCurrency(command.currency() != null ? command.currency() : "PLN");
        paymentEntity.setPaymentMethod(command.paymentMethod());
        paymentEntity.setPaymentDate(java.time.LocalDate.now());
        paymentEntity.setReferenceNumber(command.referenceNumber());
        paymentEntity.setNotes(command.notes());
        paymentEntity.setCreatedAt(java.time.LocalDateTime.now());
        paymentEntity.setUpdatedAt(java.time.LocalDateTime.now());

        // Save payment entity
        PaymentEntity savedPayment = paymentRepository.save(paymentEntity);

        return savedPayment.getId();
    }

    private String generatePaymentNumber() {
        String date = LocalDateTime.now().toString().replace("-", "").substring(0, 8);
        String random = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "PAY-" + date + "-" + random;
    }
}
