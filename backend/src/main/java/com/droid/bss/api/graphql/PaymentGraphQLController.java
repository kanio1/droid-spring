package com.droid.bss.api.graphql;

import com.droid.bss.domain.payment.PaymentEntity;
import com.droid.bss.domain.payment.PaymentStatus;
import com.droid.bss.domain.payment.PaymentMethod;
import com.droid.bss.domain.invoice.InvoiceEntity;
import com.droid.bss.infrastructure.read.PaymentReadRepository;
import com.droid.bss.infrastructure.read.InvoiceReadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * GraphQL Controller for Payment-related queries and mutations
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class PaymentGraphQLController {

    private final PaymentReadRepository paymentRepository;
    private final InvoiceReadRepository invoiceRepository;

    @QueryMapping
    @PreAuthorize("hasRole('USER')")
    public CompletableFuture<PaymentEntity> payment(@Argument UUID id) {
        log.debug("Fetching payment with id: {}", id);
        return CompletableFuture.supplyAsync(() ->
            paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + id))
        );
    }

    @QueryMapping
    @PreAuthorize("hasRole('USER')")
    public CompletableFuture<List<PaymentEntity>> payments(
            @Argument Optional<Integer> page,
            @Argument Optional<Integer> size,
            @Argument Optional<PaymentStatus> status,
            @Argument Optional<UUID> customerId,
            @Argument Optional<UUID> invoiceId) {

        log.debug("Fetching payments with filters");
        return CompletableFuture.supplyAsync(() -> {
            List<PaymentEntity> payments = paymentRepository.findAll();

            if (status.isPresent()) {
                payments = payments.stream()
                    .filter(p -> p.getStatus() == status.get())
                    .collect(java.util.stream.Collectors.toList());
            }

            if (invoiceId.isPresent()) {
                payments = payments.stream()
                    .filter(p -> p.getInvoice().getId().equals(invoiceId.get()))
                    .collect(java.util.stream.Collectors.toList());
            }

            return payments;
        });
    }

    @QueryMapping
    @PreAuthorize("hasRole('USER')")
    public CompletableFuture<List<PaymentEntity>> paymentsByInvoice(@Argument UUID invoiceId) {
        log.debug("Fetching payments for invoice: {}", invoiceId);
        return CompletableFuture.supplyAsync(() ->
            paymentRepository.findByInvoiceId(invoiceId)
        );
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public CompletableFuture<PaymentEntity> createPayment(@Argument("input") CreatePaymentInput input) {
        log.info("Creating payment for invoice: {}", input.getInvoiceId());
        return CompletableFuture.supplyAsync(() -> {
            InvoiceEntity invoice = invoiceRepository.findById(input.getInvoiceId())
                .orElseThrow(() -> new RuntimeException("Invoice not found: " + input.getInvoiceId()));

            PaymentEntity payment = PaymentEntity.builder()
                .id(UUID.randomUUID())
                .invoice(invoice)
                .amount(input.getAmount())
                .paymentMethod(input.getPaymentMethod())
                .status(PaymentStatus.PENDING)
                .transactionId(input.getTransactionId())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

            return paymentRepository.save(payment);
        });
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public CompletableFuture<PaymentEntity> updatePayment(
            @Argument UUID id,
            @Argument("input") UpdatePaymentInput input) {

        log.info("Updating payment: {}", id);
        return CompletableFuture.supplyAsync(() -> {
            PaymentEntity payment = paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + id));

            if (input.getStatus() != null) payment.setStatus(input.getStatus());
            if (input.getTransactionId() != null) payment.setTransactionId(input.getTransactionId());
            if (input.getGatewayResponse() != null) payment.setGatewayResponse(input.getGatewayResponse());
            payment.setUpdatedAt(LocalDateTime.now());

            return paymentRepository.save(payment);
        });
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public CompletableFuture<PaymentEntity> processPayment(@Argument UUID id) {
        log.info("Processing payment: {}", id);
        return CompletableFuture.supplyAsync(() -> {
            PaymentEntity payment = paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + id));

            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setProcessedAt(LocalDateTime.now());
            payment.setUpdatedAt(LocalDateTime.now());

            return paymentRepository.save(payment);
        });
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public CompletableFuture<PaymentEntity> refundPayment(
            @Argument UUID id,
            @Argument Optional<BigDecimal> amount,
            @Argument Optional<String> reason) {

        log.info("Refunding payment: {}", id);
        return CompletableFuture.supplyAsync(() -> {
            PaymentEntity payment = paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + id));

            payment.setStatus(PaymentStatus.REFUNDED);
            payment.setRefundedAt(LocalDateTime.now());
            payment.setRefundAmount(amount.orElse(payment.getAmount()));
            payment.setRefundReason(reason.orElse("No reason provided"));
            payment.setUpdatedAt(LocalDateTime.now());

            return paymentRepository.save(payment);
        });
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public CompletableFuture<PaymentEntity> cancelPayment(@Argument UUID id) {
        log.info("Cancelling payment: {}", id);
        return CompletableFuture.supplyAsync(() -> {
            PaymentEntity payment = paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + id));

            payment.setStatus(PaymentStatus.CANCELLED);
            payment.setUpdatedAt(LocalDateTime.now());

            return paymentRepository.save(payment);
        });
    }

    // ========== INPUT CLASSES ==========

    public static class CreatePaymentInput {
        private UUID invoiceId;
        private BigDecimal amount;
        private PaymentMethod paymentMethod;
        private String transactionId;

        public UUID getInvoiceId() { return invoiceId; }
        public void setInvoiceId(UUID invoiceId) { this.invoiceId = invoiceId; }

        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }

        public PaymentMethod getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

        public String getTransactionId() { return transactionId; }
        public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    }

    public static class UpdatePaymentInput {
        private PaymentStatus status;
        private String transactionId;
        private String gatewayResponse;

        public PaymentStatus getStatus() { return status; }
        public void setStatus(PaymentStatus status) { this.status = status; }

        public String getTransactionId() { return transactionId; }
        public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

        public String getGatewayResponse() { return gatewayResponse; }
        public void setGatewayResponse(String gatewayResponse) { this.gatewayResponse = gatewayResponse; }
    }
}
