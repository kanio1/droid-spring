package com.droid.bss.application.command.payment;

import com.droid.bss.application.dto.payment.ChangePaymentStatusCommand;
import com.droid.bss.domain.payment.Payment;
import com.droid.bss.domain.payment.PaymentEntity;
import com.droid.bss.domain.payment.PaymentId;
import com.droid.bss.domain.payment.PaymentStatus;
import com.droid.bss.domain.payment.repository.PaymentRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Use case for changing payment status (immutable update)
 */
@Component
@Transactional
public class ChangePaymentStatusUseCase {

    private final PaymentRepository paymentRepository;

    public ChangePaymentStatusUseCase(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public UUID handle(ChangePaymentStatusCommand command) {
        // Find existing payment
        PaymentId paymentId = new PaymentId(UUID.fromString(command.id()));
        PaymentEntity entity = paymentRepository.findById(paymentId.value())
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + command.id()));

        // Convert to DDD aggregate
        Payment payment = entity.toDomain();
        PaymentStatus newStatus = command.status();

        // Use immutable update - create new payment instance
        Payment updatedPayment = payment.changeStatus(newStatus);

        // Handle additional fields based on status
        Payment finalPayment = updatedPayment;
        if (newStatus == PaymentStatus.COMPLETED) {
            finalPayment = updatedPayment.complete();
        } else if (newStatus == PaymentStatus.FAILED) {
            finalPayment = updatedPayment.fail();
        } else if (newStatus == PaymentStatus.REFUNDED && command.reason() != null) {
            finalPayment = updatedPayment.refund(command.reason());
        }

        // Save the updated payment - convert to entity first
        PaymentEntity newEntity = PaymentEntity.from(finalPayment);
        PaymentEntity savedEntity = paymentRepository.save(newEntity);

        return savedEntity.getId();
    }
}
