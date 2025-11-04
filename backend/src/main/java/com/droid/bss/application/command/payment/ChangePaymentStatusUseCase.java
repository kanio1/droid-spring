package com.droid.bss.application.command.payment;

import com.droid.bss.application.dto.payment.ChangePaymentStatusCommand;
import com.droid.bss.domain.payment.PaymentEntity;
import com.droid.bss.domain.payment.PaymentStatus;
import com.droid.bss.domain.payment.repository.PaymentRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
@Transactional
public class ChangePaymentStatusUseCase {

    private final PaymentRepository paymentRepository;

    public ChangePaymentStatusUseCase(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public PaymentEntity handle(ChangePaymentStatusCommand command) {
        // Find existing payment
        PaymentEntity payment = paymentRepository.findById(UUID.fromString(command.id()))
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + command.id()));

        PaymentStatus oldStatus = payment.getPaymentStatus();
        PaymentStatus newStatus = command.status();

        // Update status
        payment.setPaymentStatus(newStatus);
        payment.setUpdatedAt(LocalDateTime.now());

        // Set additional fields based on status
        if (newStatus == PaymentStatus.COMPLETED && oldStatus != PaymentStatus.COMPLETED) {
            payment.setReceivedDate(LocalDate.now());
        }

        if (newStatus == PaymentStatus.REFUNDED && command.reason() != null) {
            payment.setReversalReason(command.reason());
        }

        return paymentRepository.save(payment);
    }
}
