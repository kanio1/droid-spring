package com.droid.bss.application.command.payment;

import com.droid.bss.application.dto.payment.CreatePaymentCommand;
import com.droid.bss.domain.customer.CustomerEntity;
import com.droid.bss.domain.customer.CustomerEntityRepository;
import com.droid.bss.domain.invoice.InvoiceEntity;
import com.droid.bss.domain.invoice.repository.InvoiceEntityRepository;
import com.droid.bss.domain.payment.PaymentEntity;
import com.droid.bss.domain.payment.PaymentMethod;
import com.droid.bss.domain.payment.PaymentStatus;
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
        CustomerEntity customer = customerEntityRepository.findById(UUID.fromString(command.customerId()))
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + command.customerId()));

        // Validate invoice if provided
        InvoiceEntity invoice = null;
        if (command.invoiceId() != null) {
            invoice = invoiceEntityRepository.findById(UUID.fromString(command.invoiceId()))
                    .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + command.invoiceId()));
        }

        // Generate payment number (format: PAY-YYYYMMDD-XXXX)
        String paymentNumber = generatePaymentNumber();

        // Create payment entity
        PaymentEntity payment = new PaymentEntity();
        payment.setPaymentNumber(paymentNumber);
        payment.setCustomer(customer);
        payment.setInvoice(invoice);
        payment.setAmount(command.amount());
        payment.setCurrency(command.currency());
        payment.setPaymentMethod(command.paymentMethod());
        payment.setPaymentStatus(PaymentStatus.PENDING);
        payment.setPaymentDate(command.paymentDate());
        payment.setReferenceNumber(command.referenceNumber());
        payment.setNotes(command.notes());
        payment.setCreatedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());

        // Save payment
        PaymentEntity savedPayment = paymentRepository.save(payment);

        return savedPayment.getId();
    }

    private String generatePaymentNumber() {
        String date = LocalDateTime.now().toString().replace("-", "").substring(0, 8);
        String random = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "PAY-" + date + "-" + random;
    }
}
